#!/usr/bin/env bash
# test.sh [versions...]: for each line, start the server, wait, read its self-test, run the
# three-player bot where a bot can speak the protocol, stop it. One summary line per version.
source "$(dirname "$0")/common.sh"
LIST=("$@"); [ ${#LIST[@]} -gt 0 ] || LIST=("${VERSIONS[@]}")
for V in "${LIST[@]}"; do
  "$(dirname "$0")/run.sh" "$V" >/dev/null || { echo "$V: did not start"; continue; }
  L="$B/$V/server.log"
  for i in $(seq 1 150); do grep -qE "Done \(|Exception in server|Failed to start" "$L" && break; sleep 2; done
  ST="$(grep -oE 'ChatFilter self-test (PASSED|FAILED)[^=]*' "$L" | head -1)"
  [ -n "$ST" ] || ST="self-test: no result ($(grep -m1 -E 'ChatFilter|Error|Exception' "$L" | cut -c1-120))"
  PORT=$(port_of "$V")
  case "$V" in
    26.3) BOT="bot: none speaks 26.3" ;;
    *) BOT="$(cd "$B/bot" && timeout 120 node chatfilter-bot.js 127.0.0.1 "$PORT" "$V" 2>&1 | tail -1)" ;;
  esac
  echo "$V: $ST | $BOT"
  grep -E "\] *x |self-test FAILED" "$L" | head -5 | sed 's/^/    /'
  "$(dirname "$0")/stop.sh" "$V" >/dev/null
done
