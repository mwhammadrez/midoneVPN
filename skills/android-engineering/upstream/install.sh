#!/usr/bin/env bash
# Install android-agent-engineering into an Android project repository.
#
# Usage:
#   ./install.sh /path/to/android-project [options]
#
# Options:
#   --ref <tag|branch|commit>   Version to install (default: latest vX.Y.Z tag, else HEAD)
#   --source-url <git-url>      Clone from a remote instead of this local checkout
#   --yes                       Don't prompt for confirmation (for scripting/CI)
#   -h, --help                  Show this help
#
# What it does:
#   - Copies skills/, workflows/, templates/ into <target>/.agent-engineering/
#   - Copies skills+workflows into <target>/.claude/skills/ for Claude Code
#   - Adds/updates a marked block in <target>/AGENTS.md pointing at
#     .agent-engineering/ (for Codex and other AGENTS.md-reading agents)
#   - Never writes outside those locations, and never overwrites an
#     existing file without a diff preview and confirmation.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/scripts/lib.sh"

usage() { sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

TARGET=""
REF=""
SOURCE_URL=""
ASSUME_YES=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ref) REF="$2"; shift 2 ;;
        --source-url) SOURCE_URL="$2"; shift 2 ;;
        --yes) ASSUME_YES=1; shift ;;
        -h|--help) usage; exit 0 ;;
        -*) aae_die "Unknown option: $1" ;;
        *)
            if [[ -z "$TARGET" ]]; then TARGET="$1"; else aae_die "Unexpected argument: $1"; fi
            shift
            ;;
    esac
done

[[ -n "$TARGET" ]] || { usage; aae_die "Missing required argument: path to Android project"; }
[[ -d "$TARGET" ]] || aae_die "Target directory does not exist: $TARGET"
command -v git >/dev/null 2>&1 || aae_die "git is required"

TARGET="$(cd "$TARGET" && pwd)"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if [[ -n "$SOURCE_URL" ]]; then
    REF="${REF:-main}"
    aae_log "Cloning $SOURCE_URL @ $REF ..."
    git clone --quiet "$SOURCE_URL" "$WORKDIR/src"
    git -C "$WORKDIR/src" checkout --quiet "$REF"
    SRC="$WORKDIR/src"
    COMMIT_REPO="$WORKDIR/src"
else
    [[ -d "$SCRIPT_DIR/.git" ]] || aae_die "This checkout has no .git directory; pass --source-url to install from a remote instead."
    REF="${REF:-$(aae_latest_ref "$SCRIPT_DIR")}"
    aae_log "Installing from local checkout @ $REF ..."
    mkdir -p "$WORKDIR/src"
    git -C "$SCRIPT_DIR" archive "$REF" | tar -x -C "$WORKDIR/src"
    SRC="$WORKDIR/src"
    COMMIT_REPO="$SCRIPT_DIR"
fi

COMMIT="$(git -C "$COMMIT_REPO" rev-parse "$REF" 2>/dev/null || echo unknown)"
VERSION="$(cat "$SRC/VERSION" 2>/dev/null || echo unknown)"

AAE_DIR="$TARGET/.agent-engineering"
aae_log "Target: $AAE_DIR (version $VERSION, ref $REF)"

for sub in skills workflows templates; do
    dest="$AAE_DIR/$sub"
    echo
    aae_log "Reviewing changes to .agent-engineering/$sub"
    aae_diff_summary "$dest" "$SRC/$sub"
    if aae_confirm "Apply .agent-engineering/$sub?"; then
        aae_replace_dir "$dest" "$SRC/$sub"
    else
        aae_warn "Skipped .agent-engineering/$sub"
    fi
done

echo
aae_log "Reviewing Claude Code skill placement (.claude/skills/)"
if aae_confirm "Copy skills + workflows into .claude/skills/ for Claude Code discovery?"; then
    mkdir -p "$TARGET/.claude/skills"
    count=0
    while IFS= read -r d; do
        [[ -d "$d" ]] || continue
        name="$(basename "$d")"
        aae_replace_dir "$TARGET/.claude/skills/$name" "$d"
        count=$((count + 1))
    done < <(aae_skill_dirs "$SRC")
    aae_log "Placed $count skill/workflow directories under .claude/skills/ (flat, regardless of source grouping)"
    aae_warn "If this project already has its own skill with the same name under .claude/skills/, rename one to avoid a collision."
else
    aae_warn "Skipped .claude/skills/ placement"
fi

echo
aae_log "Reviewing AGENTS.md (Codex / generic SKILL.md-aware agents)"
AGENTS_FILE="$TARGET/AGENTS.md"
if aae_file_has_block "$AGENTS_FILE"; then
    aae_log "AGENTS.md already has an android-agent-engineering block; showing what would change:"
    diff <(aae_extract_block "$AGENTS_FILE") <(aae_extract_block "$SRC/AGENTS.template.md") || true
    if aae_confirm "Update the android-agent-engineering block in AGENTS.md?"; then
        aae_replace_block "$AGENTS_FILE" "$SRC/AGENTS.template.md"
    else
        aae_warn "Left AGENTS.md unchanged"
    fi
elif [[ -f "$AGENTS_FILE" ]]; then
    if aae_confirm "AGENTS.md exists with no android-agent-engineering block. Append one?"; then
        aae_append_block "$AGENTS_FILE" "$SRC/AGENTS.template.md"
    else
        aae_warn "Left AGENTS.md unchanged"
    fi
else
    if aae_confirm "No AGENTS.md found. Create one from the template?"; then
        cp "$SRC/AGENTS.template.md" "$AGENTS_FILE"
    else
        aae_warn "Skipped AGENTS.md"
    fi
fi

echo
aae_log "Reviewing CLAUDE.md (optional explicit pointer for Claude Code)"
CLAUDE_FILE="$TARGET/CLAUDE.md"
if aae_file_has_block "$CLAUDE_FILE"; then
    if aae_confirm "Update the android-agent-engineering block in CLAUDE.md?"; then
        aae_replace_block "$CLAUDE_FILE" "$SRC/CLAUDE.template.md"
    fi
elif [[ -f "$CLAUDE_FILE" ]]; then
    if aae_confirm "CLAUDE.md exists with no android-agent-engineering block. Append one?"; then
        aae_append_block "$CLAUDE_FILE" "$SRC/CLAUDE.template.md"
    fi
else
    aae_confirm "No CLAUDE.md found. Create one from the template? (optional - .claude/skills/ placement already makes skills discoverable without this)" && cp "$SRC/CLAUDE.template.md" "$CLAUDE_FILE"
fi

mkdir -p "$AAE_DIR"
aae_write_lockfile "$AAE_DIR/INSTALLED" \
    "VERSION=$VERSION" \
    "REF=$REF" \
    "COMMIT=$COMMIT" \
    "INSTALLED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    "SOURCE=${SOURCE_URL:-local:$SCRIPT_DIR}"

echo
aae_log "Done. Installed version $VERSION (ref $REF) into $AAE_DIR"
aae_log "Run ./update.sh $TARGET later to upgrade with a diff-and-confirm step."
