#!/bin/bash
# Production boot check for the "one jar" promise: a dedicated server with ONLY
# build/libs/create-fabric-*.jar and Fabric API must reach "Done (".
#
#   scripts/qa/onejar_boot.sh [create-jar] [fabric-api-jar]
#
# Exit 0 = booted, 1 = crashed / never reached Done. Needs network for the Fabric
# server launcher and (first run) the vanilla server jar. Takes ~1-2 minutes.
#
# Failed on 3ec980de36 (fixed by bundling porting_lib_conditions beta.55): Registrate-Fabric nests porting_lib_data/conditions
# 3.1.0-beta.39, whose classTweaker is in the "named" namespace, and nothing else in
# the jar provides a newer porting_lib_conditions, so Fabric Loader aborts with
# "Failed to read classTweaker file from mod porting_lib_conditions: Namespace (named)
# does not match current runtime namespace (intermediary)". The owner's pack only
# boots because sophisticatedcore bundles porting_lib_conditions 3.1.0-beta.47.
set -u
REPO=$(cd "$(dirname "$0")/../.." && pwd)
JAR=${1:-$(ls -t "$REPO"/build/libs/create-fabric-*+mc1.21.1.jar | grep -v sources | head -1)}
FAPI=${2:-}
LOADER=${LOADER:-0.19.5}
DIR=$(mktemp -d "${TMPDIR:-/tmp}/onejar.XXXXXX")
mkdir -p "$DIR/mods"
if [ -z "$FAPI" ]; then
    # the pack's Fabric API; the bundled Porting Lib needs >= 0.116.1 (fabric.mod.json
    # only asks for >= 0.115.1, so 0.115.x fails with "can't be loaded due to other
    # constraints")
    FAPI="$DIR/fabric-api-0.116.17+1.21.1.jar"
    curl -sSfL -o "$FAPI" "https://api.modrinth.com/maven/maven/modrinth/fabric-api/0.116.17+1.21.1/fabric-api-0.116.17+1.21.1.jar" || exit 1
fi
cp "$JAR" "$FAPI" "$DIR/mods/"
# EXTRA_MODS="a.jar b.jar" adds jars, e.g. to confirm a workaround
for m in ${EXTRA_MODS:-}; do cp "$m" "$DIR/mods/"; done
curl -sSfL -o "$DIR/server.jar" "https://meta.fabricmc.net/v2/versions/loader/1.21.1/$LOADER/1.1.0/server/jar" || exit 1
echo "eula=true" > "$DIR/eula.txt"
printf 'online-mode=false\nlevel-type=minecraft\\:flat\nserver-port=25599\n' > "$DIR/server.properties"
cd "$DIR"
# stdin from /dev/null: no feeder process can outlive the script and hold the caller's pipe
timeout 650 java -Xmx2G -jar server.jar nogui < /dev/null > boot.log 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null' EXIT
for _ in $(seq 1 120); do
    sleep 5
    if grep -q 'Done (' boot.log; then echo "PASS: booted with $(basename "$JAR") + $(basename "$FAPI")${EXTRA_MODS:+ + extra mods}"; kill $PID 2>/dev/null; exit 0; fi
    if ! kill -0 $PID 2>/dev/null || grep -qE 'An exception occurred when launching|Exception in server tick loop|Incompatible mods|Mod resolution failed|Crash report saved' boot.log; then break; fi
done
kill $PID 2>/dev/null
echo "FAIL: server did not start with only $(basename "$JAR") + $(basename "$FAPI") (log: $DIR/boot.log)"
grep -m3 -E 'Caused by|An exception occurred|Incompatible' boot.log
exit 1
