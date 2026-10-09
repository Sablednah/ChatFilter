#!/usr/bin/env bash
# core/ is ChatFilter-ReForged's com.sablednah.chatfilter.core, copied VERBATIM. Change it there, then:
#   scripts/sync-core.sh           copy it here from ../ChatFilter-ReForged (its checked-out branch)
#   scripts/sync-core.sh --check   fail if the two differ (run before a release)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${CHATFILTER_FORGE:-$ROOT/../ChatFilter-ReForged}/src/main/java/com/sablednah/chatfilter/core"
DST="$ROOT/core/src/main/java/com/sablednah/chatfilter/core"
[ -d "$SRC" ] || { echo "!! No ChatFilter-ReForged core at $SRC (set CHATFILTER_FORGE)" >&2; exit 1; }
if [ "${1:-}" = "--check" ]; then
    if diff -r "$SRC" "$DST"; then echo ">> core matches ChatFilter-ReForged"; else echo "!! core differs from ChatFilter-ReForged" >&2; exit 1; fi
else
    rm -rf "$DST" && mkdir -p "$DST" && cp "$SRC"/*.java "$DST"/
    echo ">> copied $(ls "$DST" | wc -l) files from $SRC"
fi
