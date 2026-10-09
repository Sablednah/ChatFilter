#!/usr/bin/env bash
# Start one test server: run.sh <version>. The newest jar for its line goes in, the test config
# replaces the plugin's (the shipped one plus a SHADOW rule for "banana"), and the self-test runs.
set -euo pipefail
source "$(dirname "$0")/common.sh"
V="${1:?version, one of: ${VERSIONS[*]}}"
D="$B/$V"; [ -d "$D" ] || { echo "no $D - run setup.sh" >&2; exit 1; }
PORT=$(port_of "$V"); JAVA=$(java_of "$V"); SUFFIX=$(suffix_of "$V")
if [ -f "$D/server.pid" ] && kill -0 "$(cat "$D/server.pid")" 2>/dev/null; then echo "$V already running" >&2; exit 1; fi
rm -f "$D"/plugins/chatfilter-*.jar
cp "$B/jars/chatfilter-"*"+$SUFFIX.jar" "$D/plugins/"
mkdir -p "$D/plugins/ChatFilter" && cp "$B/bot/test-config.yml" "$D/plugins/ChatFilter/config.yml"
cd "$D"; : > server.log
# stdin on a FIFO held open for the server's life: an old server reading EOF spins its console
# thread and starves logins (MobHealth's rig learned this the hard way).
FIFO="$D/console.fifo"; rm -f "$FIFO"; mkfifo "$FIFO"
nohup "$JAVA" -Xms256M -Xmx1536M -Dchatfilter.selftest=true -jar server.jar nogui < "$FIFO" > server.log 2>&1 &
JPID=$!
nohup bash -c "exec 3>\"$FIFO\"; while kill -0 $JPID 2>/dev/null; do sleep 5; done" >/dev/null 2>&1 &
echo "$JPID" > server.pid
bash ~/dev/running.sh claim "chatfilter-$V" "$JPID" "ChatFilter Bukkit test server $V on $PORT" >/dev/null 2>&1 || true
echo "started $V (pid $JPID) on $PORT with $(basename "$D"/plugins/chatfilter-*.jar)"
