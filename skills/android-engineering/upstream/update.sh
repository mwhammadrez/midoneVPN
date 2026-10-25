#!/usr/bin/env bash
# Upgrade an already-installed copy of android-agent-engineering in an
# Android project repository, showing a diff and asking for confirmation
# before overwriting anything.
#
# Usage:
#   ./update.sh /path/to/android-project [options]
#
# Options:
#   --ref <tag|branch|commit>   Version to upgrade to (default: latest vX.Y.Z tag, else HEAD)
#   --source-url <git-url>      Fetch from a remote instead of this local checkout
#   --yes                       Don't prompt for confirmation (for scripting/CI)
#   -h, --help                  Show this help
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/scripts/lib.sh"

usage() { sed -n '2,14p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

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
TARGET="$(cd "$TARGET" && pwd)"
AAE_DIR="$TARGET/.agent-engineering"
LOCKFILE="$AAE_DIR/INSTALLED"

[[ -f "$LOCKFILE" ]] || aae_die "No $LOCKFILE found. Run install.sh first."

CURRENT_VERSION="$(aae_read_lockfile_value "$LOCKFILE" VERSION)"
CURRENT_REF="$(aae_read_lockfile_value "$LOCKFILE" REF)"
CURRENT_COMMIT="$(aae_read_lockfile_value "$LOCKFILE" COMMIT)"
aae_log "Currently installed: version $CURRENT_VERSION (ref $CURRENT_REF, commit $CURRENT_COMMIT)"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if [[ -n "$SOURCE_URL" ]]; then
    REF="${REF:-main}"
    git clone --quiet "$SOURCE_URL" "$WORKDIR/src"
    git -C "$WORKDIR/src" checkout --quiet "$REF"
    SRC="$WORKDIR/src"
    COMMIT_REPO="$WORKDIR/src"
else
    [[ -d "$SCRIPT_DIR/.git" ]] || aae_die "This checkout has no .git directory; pass --source-url to update from a remote instead."
    REF="${REF:-$(aae_latest_ref "$SCRIPT_DIR")}"
    mkdir -p "$WORKDIR/src"
    git -C "$SCRIPT_DIR" archive "$REF" | tar -x -C "$WORKDIR/src"
    SRC="$WORKDIR/src"
    COMMIT_REPO="$SCRIPT_DIR"
fi

NEW_COMMIT="$(git -C "$COMMIT_REPO" rev-parse "$REF" 2>/dev/null || echo unknown)"
NEW_VERSION="$(cat "$SRC/VERSION" 2>/dev/null || echo unknown)"

if [[ "$NEW_COMMIT" == "$CURRENT_COMMIT" ]]; then
    aae_log "Already up to date (version $CURRENT_VERSION, ref $REF)."
    exit 0
fi

aae_log "Upgrading to version $NEW_VERSION (ref $REF, commit $NEW_COMMIT)"

CHANGED_ANY=0
SKILLS_OR_WORKFLOWS_APPLIED=0
for sub in skills workflows templates; do
    dest="$AAE_DIR/$sub"
    echo
    aae_log "Changes to .agent-engineering/$sub:"
    summary="$(aae_diff_summary "$dest" "$SRC/$sub")"
    if [[ -z "$summary" ]]; then
        echo "  (no changes)"
        continue
    fi
    echo "$summary"
    CHANGED_ANY=1
    if aae_confirm "Apply these changes to .agent-engineering/$sub?"; then
        aae_replace_dir "$dest" "$SRC/$sub"
        [[ "$sub" == "skills" || "$sub" == "workflows" ]] && SKILLS_OR_WORKFLOWS_APPLIED=1
    else
        aae_warn "Skipped .agent-engineering/$sub — lock file will not be updated for it; re-run update.sh to try again"
    fi
done

if [[ "$SKILLS_OR_WORKFLOWS_APPLIED" == "1" && -d "$TARGET/.claude/skills" ]]; then
    echo
    aae_log "Re-syncing .claude/skills/ (including any newly added skills/workflows)"
    count=0
    while IFS= read -r d; do
        [[ -d "$d" ]] || continue
        name="$(basename "$d")"
        aae_replace_dir "$TARGET/.claude/skills/$name" "$d"
        count=$((count + 1))
    done < <(aae_skill_dirs "$SRC")
    aae_log "Synced $count skill/workflow directories"
fi

echo
for pair in "AGENTS.md:$SRC/AGENTS.template.md" "CLAUDE.md:$SRC/CLAUDE.template.md"; do
    file_name="${pair%%:*}"
    template="${pair#*:}"
    target_file="$TARGET/$file_name"
    if aae_file_has_block "$target_file"; then
        current_block="$(aae_extract_block "$target_file")"
        new_block="$(aae_extract_block "$template")"
        if [[ "$current_block" != "$new_block" ]]; then
            aae_log "$file_name's android-agent-engineering block has changed:"
            diff <(echo "$current_block") <(echo "$new_block") || true
            if aae_confirm "Update the block in $file_name?"; then
                aae_replace_block "$target_file" "$template"
            fi
        fi
    fi
done

if [[ "$CHANGED_ANY" == "0" ]]; then
    aae_log "Content is identical to the new version; only the version/ref pointer changed."
fi

aae_write_lockfile "$LOCKFILE" \
    "VERSION=$NEW_VERSION" \
    "REF=$REF" \
    "COMMIT=$NEW_COMMIT" \
    "INSTALLED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    "SOURCE=${SOURCE_URL:-local:$SCRIPT_DIR}"

echo
aae_log "Updated to version $NEW_VERSION (ref $REF)."
