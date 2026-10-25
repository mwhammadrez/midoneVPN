#!/usr/bin/env bash
# Shared shell functions for install.sh and update.sh.
# Sourced, not executed directly.

AAE_BEGIN_MARK="<!-- BEGIN android-agent-engineering -->"
AAE_END_MARK="<!-- END android-agent-engineering -->"

aae_log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
aae_warn() { printf '\033[1;33m!!\033[0m %s\n' "$*" >&2; }
aae_die()  { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# aae_confirm "question" -> 0 (yes) or 1 (no). Honors ASSUME_YES=1.
aae_confirm() {
    local prompt="$1"
    if [[ "${ASSUME_YES:-0}" == "1" ]]; then
        return 0
    fi
    local reply
    read -r -p "$prompt [y/N] " reply </dev/tty || reply="n"
    case "$reply" in
        y|Y|yes|YES) return 0 ;;
        *) return 1 ;;
    esac
}

# aae_latest_ref <repo-dir> -> latest semver tag, or HEAD if none exist.
aae_latest_ref() {
    local repo_dir="$1"
    local tag
    tag="$(git -C "$repo_dir" tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -n1 || true)"
    if [[ -n "$tag" ]]; then
        printf '%s' "$tag"
    else
        printf 'HEAD'
    fi
}

# aae_diff_summary <old-dir> <new-dir> -> prints one line per added/removed/changed file.
aae_diff_summary() {
    local old_dir="$1" new_dir="$2"
    if [[ ! -d "$old_dir" ]]; then
        echo "  (nothing currently installed here)"
        return 0
    fi
    # diff exits 1 when it finds differences (expected/normal here, not an
    # error) and 2 on a real error; under set -e + pipefail a bare exit 1
    # would abort the caller, so neutralize it with `|| true` and rely on
    # the loop itself for correctness.
    diff --recursive --brief "$old_dir" "$new_dir" 2>/dev/null | while IFS= read -r line; do
        case "$line" in
            "Only in "*": "*)
                rest="${line#Only in }"
                dir="${rest%%: *}"
                name="${rest#*: }"
                rel="$dir/$name"
                if [[ "$dir" == "$old_dir"* ]]; then
                    echo "  removed: ${rel#"$old_dir"/}"
                else
                    echo "  added:   ${rel#"$new_dir"/}"
                fi
                ;;
            "Files "*" differ")
                echo "  changed: $(echo "$line" | sed -E 's/^Files .* and (.*) differ$/\1/' | sed "s#^$new_dir/##")"
                ;;
            *) echo "  $line" ;;
        esac
    done || true
}

# aae_skill_dirs <src-root> -> one path per line, the parent directory of
# every SKILL.md found under src-root/skills and src-root/skills (both
# searched recursively, so this works whether a given skill sits directly
# under skills/ or under a grouping folder like skills/design-system/).
aae_skill_dirs() {
    local src_root="$1"
    find "$src_root/skills" "$src_root/workflows" -name SKILL.md 2>/dev/null | while IFS= read -r f; do
        dirname "$f"
    done
}

# aae_replace_dir <dest-dir> <src-dir> - dest becomes an exact copy of src.
# Only ever call this with a dest path inside .agent-engineering/ or
# .claude/skills/<our-names> - never a path that could contain
# project-owned content.
aae_replace_dir() {
    local dest="$1" src="$2"
    rm -rf "$dest"
    mkdir -p "$(dirname "$dest")"
    cp -R "$src" "$dest"
}

# aae_write_lockfile <path> key=value ...
aae_write_lockfile() {
    local path="$1"; shift
    : > "$path"
    for kv in "$@"; do
        echo "$kv" >> "$path"
    done
}

# aae_read_lockfile_value <path> <key> -> value, or empty string.
aae_read_lockfile_value() {
    local path="$1" key="$2"
    [[ -f "$path" ]] || return 0
    awk -F= -v k="$key" '$1==k{ sub(/^[^=]*=/,""); print; exit }' "$path"
}

# aae_extract_block <file> -> the BEGIN..END marked block, inclusive.
aae_extract_block() {
    local file="$1"
    awk -v begin="$AAE_BEGIN_MARK" -v end="$AAE_END_MARK" '
        $0==begin{flag=1}
        flag{print}
        $0==end{flag=0}
    ' "$file"
}

# aae_file_has_block <file> -> 0 if the marked block is present.
aae_file_has_block() {
    local file="$1"
    [[ -f "$file" ]] && grep -qF "$AAE_BEGIN_MARK" "$file"
}

# aae_replace_block <target-file> <new-block-file>
# Replaces an existing marked block in target-file with the block found
# in new-block-file, preserving everything else in target-file
# byte-for-byte. Deliberately avoids passing multi-line content through
# `awk -v` - the BWK/nawk awk shipped as /usr/bin/awk on macOS rejects an
# embedded newline in a -v value ("newline in string"), unlike gawk.
aae_replace_block() {
    local target="$1" new_block_file="$2"
    local block_tmp tmp begin_line end_line
    block_tmp="$(mktemp)"
    aae_extract_block "$new_block_file" > "$block_tmp"

    begin_line="$(grep -nF "$AAE_BEGIN_MARK" "$target" | head -n1 | cut -d: -f1)"
    end_line="$(grep -nF "$AAE_END_MARK" "$target" | head -n1 | cut -d: -f1)"

    tmp="$(mktemp)"
    if [[ "$begin_line" -gt 1 ]]; then
        head -n "$((begin_line - 1))" "$target" > "$tmp"
    else
        : > "$tmp"
    fi
    cat "$block_tmp" >> "$tmp"
    tail -n "+$((end_line + 1))" "$target" >> "$tmp"
    mv "$tmp" "$target"
    rm -f "$block_tmp"
}

# aae_append_block <target-file> <new-block-file>
aae_append_block() {
    local target="$1" new_block_file="$2"
    {
        [[ -s "$target" ]] && echo
        aae_extract_block "$new_block_file"
    } >> "$target"
}
