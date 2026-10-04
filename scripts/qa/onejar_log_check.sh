#!/bin/bash
# Log check for the "one jar + Fabric API" promise: the dedicated server must not only reach
# "Done (" (scripts/qa/onejar_boot.sh) but also log no ERROR lines of its own.
#
#   scripts/qa/onejar_log_check.sh [create-jar] [fabric-api-jar]
#
# Exit 0 = booted with a clean log, 1 = boot failed or ERROR lines were logged.
#
# Failed on 594de2093f (tester round 2): porting_lib_conditions 3.1.0-beta.55 logged
#   [main/ERROR]: Registry 'porting_lib:condition_codecs' was empty after loading   (2x)
# and replaced the API-incompatible beta.47 that sophisticatedcore bundles in the owner's pack.
# Fixed by bundling beta.47 instead.
set -u
REPO=$(cd "$(dirname "$0")/../.." && pwd)
WORK=$(mktemp -d "${TMPDIR:-/tmp}/onejar-log.XXXXXX")
TMPDIR="$WORK" bash "$REPO/scripts/qa/onejar_boot.sh" "$@" || exit 1
LOG=$(ls "$WORK"/onejar.*/boot.log | head -1)
# Porting Lib's loot module registers no loot modifier serializers of its own and Create (like
# upstream) has none, so with no other Porting Lib mods installed vanilla reports the registry as
# empty. Harmless; in the owner's pack other mods fill it and the line does not appear.
# (and vanilla: a flat world with empty generator settings always logs "No key layers" once)
ERRORS=$(grep -E '/ERROR\]' "$LOG" | grep -v 'No key layers in MapLike' \
    | grep -v "Registry 'porting_lib:global_loot_modifier_serializers' was empty after loading")
if [ -n "$ERRORS" ]; then
    echo "FAIL: the one-jar server logged ERROR lines ($LOG):"
    echo "$ERRORS"
    exit 1
fi
echo "PASS: no ERROR lines in $LOG"
