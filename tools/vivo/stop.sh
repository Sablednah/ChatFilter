#!/usr/bin/env bash
# stop.sh <version|all>
source "$(dirname "$0")/common.sh"
stop_one() {
  local d="$B/$1" pid
  [ -f "$d/server.pid" ] || return 0
  pid=$(cat "$d/server.pid")
  if kill -0 "$pid" 2>/dev/null; then
    [ -p "$d/console.fifo" ] && echo stop > "$d/console.fifo" 2>/dev/null
    for i in $(seq 1 30); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid"
    echo "stopped $1"
  fi
  rm -f "$d/server.pid"
}
if [ "${1:?version or all}" = all ]; then for V in "${VERSIONS[@]}"; do stop_one "$V"; done; else stop_one "$1"; fi
bash ~/dev/running.sh prune >/dev/null 2>&1 || true
