#!/usr/bin/env bash
# Adds one commit to the `upstream-gjf` branch: the given upstream Create commit,
# reformatted with the same formatter this fork uses (google-java-format --aosp).
#
# Why: Blockfield reformatted the whole codebase (tabs -> gjf AOSP). Merging raw
# upstream conflicts in ~every Java file. Merging the formatted copy instead keeps
# conflicts to real code changes. Each upstream-gjf commit has two parents:
# the previous upstream-gjf commit and the raw upstream commit, so `git merge
# upstream-gjf` always finds the previous formatted step as its merge base.
#
# usage: scripts/upstream-gjf.sh <upstream-commit-or-tag>
#        git merge --no-ff upstream-gjf
set -euo pipefail

REF="$1"
ROOT="$(git rev-parse --show-toplevel)"
GJF_DIR="$ROOT/.gradle/gjf"
GJF="$GJF_DIR/google-java-format-1.36.1-all-deps.jar"
GJF_SHA=25b400f003089d23cc5320cdaf1a16cabee19b8aa3434d0ff021b3d9f42154b4

if [ ! -f "$GJF" ]; then
    mkdir -p "$GJF_DIR"
    curl -sSL -o "$GJF" \
        https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/1.36.1/google-java-format-1.36.1-all-deps.jar
fi
echo "$GJF_SHA  $GJF" | sha256sum -c --quiet

RAW="$(git rev-parse "$REF^{commit}")"
WT="$(mktemp -d "${TMPDIR:-/tmp}/upstream-gjf.XXXXXX")"
git worktree add -q --detach "$WT" "$RAW"
(
    cd "$WT"
    # gjf skips files it cannot parse (it prints an error and leaves them as-is)
    find src -name '*.java' -print0 | xargs -0 -n 400 java -jar "$GJF" --aosp --replace 2>&1 \
        | grep -v '^Picked up JAVA_TOOL_OPTIONS' || true
    git add -A src
)
TREE="$(git -C "$WT" write-tree)"
git worktree remove --force "$WT"

PARENTS=()
if git rev-parse -q --verify refs/heads/upstream-gjf >/dev/null; then
    PARENTS+=(-p "$(git rev-parse refs/heads/upstream-gjf)")
fi
PARENTS+=(-p "$RAW")

VERSION="$(git show "$RAW:gradle.properties" | sed -n 's/^mod_version *= *//p')"
COMMIT="$(git commit-tree "$TREE" "${PARENTS[@]}" \
    -m "upstream Create $VERSION ($(git rev-parse --short "$RAW")), formatted with google-java-format --aosp")"
git update-ref refs/heads/upstream-gjf "$COMMIT"
echo "upstream-gjf -> $COMMIT (Create $VERSION)"
