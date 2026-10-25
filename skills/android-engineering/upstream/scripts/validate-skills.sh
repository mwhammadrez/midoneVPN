#!/usr/bin/env bash
# CI guardrail: every skills/ and workflows/ entry has valid SKILL.md
# frontmatter, and no file in the repo contains an obvious secret
# pattern. This is a cheap first line of defense, not a substitute for a
# real secret scanner (see SECURITY.md).
set -uo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAIL=0

echo "== Checking SKILL.md frontmatter =="
while IFS= read -r -d '' skill_file; do
    rel="${skill_file#"$REPO_DIR"/}"
    first_line="$(head -n1 "$skill_file")"
    if [[ "$first_line" != "---" ]]; then
        echo "FAIL: $rel does not start with a '---' frontmatter block"
        FAIL=1
        continue
    fi
    frontmatter="$(awk '/^---$/{c++; next} c==1{print} c==2{exit}' "$skill_file")"
    if ! grep -qE '^name:[[:space:]]*\S+' <<< "$frontmatter"; then
        echo "FAIL: $rel is missing a 'name:' frontmatter field"
        FAIL=1
    fi
    if ! grep -qE '^description:' <<< "$frontmatter"; then
        echo "FAIL: $rel is missing a 'description:' frontmatter field"
        FAIL=1
    fi
    declared_name="$(grep -E '^name:' <<< "$frontmatter" | head -n1 | sed -E 's/^name:[[:space:]]*//')"
    dir_name="$(basename "$(dirname "$skill_file")")"
    if [[ -n "$declared_name" && "$declared_name" != "$dir_name" ]]; then
        echo "FAIL: $rel declares name '$declared_name' but lives in directory '$dir_name'"
        FAIL=1
    fi
done < <(find "$REPO_DIR/skills" "$REPO_DIR/workflows" -name SKILL.md -print0 2>/dev/null)

echo
echo "== Scanning for obvious secret patterns =="
# Best-effort only. Deliberately excludes this script itself (it needs to
# contain the patterns to look for) and .git internals.
SECRET_PATTERNS=(
    'AKIA[0-9A-Z]{16}'                    # AWS access key id
    '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----'
    '(api[_-]?key|apikey|secret|token)[[:space:]]*[:=][[:space:]]*"[A-Za-z0-9_\-]{16,}"'
)
MATCHES=0
while IFS= read -r -d '' f; do
    [[ "$f" == "$REPO_DIR/scripts/validate-skills.sh" ]] && continue
    [[ "$f" == "$REPO_DIR/SECURITY.md" ]] && continue
    for pat in "${SECRET_PATTERNS[@]}"; do
        if grep -EnI "$pat" "$f" >/tmp/aae-secret-hit.$$ 2>/dev/null; then
            echo "POSSIBLE SECRET: ${f#"$REPO_DIR"/}"
            sed 's/^/    /' /tmp/aae-secret-hit.$$
            MATCHES=1
        fi
        rm -f /tmp/aae-secret-hit.$$
    done
done < <(find "$REPO_DIR" -type f \( -name '*.md' -o -name '*.kt' -o -name '*.kts' -o -name '*.sh' -o -name '*.yml' -o -name '*.toml' \) -not -path '*/.git/*' -print0)

if [[ "$MATCHES" == "1" ]]; then
    echo "FAIL: possible secret pattern(s) found above — review before merging."
    FAIL=1
fi

echo
if [[ "$FAIL" == "0" ]]; then
    echo "All checks passed."
else
    echo "One or more checks failed."
fi
exit "$FAIL"
