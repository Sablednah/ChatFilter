#!/usr/bin/env bash
# One-off: build ~/mc/chatfilter-bukkit/<version>/ for each line, copying the server jar (and the
# Paper caches, so nothing is re-downloaded) from MobHealth's rig. Worlds are not copied.
set -euo pipefail
source "$(dirname "$0")/common.sh"
M=~/mc/mobhealth-bukkit
mkdir -p "$B/jars" "$B/bot"
for V in "${VERSIONS[@]}"; do
  D="$B/$V"; mkdir -p "$D/plugins"
  cp "$M/$V/server.jar" "$D/"
  for extra in cache libraries versions; do [ -d "$M/$V/$extra" ] && cp -r "$M/$V/$extra" "$D/" || true; done
  echo "eula=true" > "$D/eula.txt"
  PORT=$(port_of "$V")
  cat > "$D/server.properties" <<PROPS
server-port=$PORT
online-mode=false
enforce-secure-profile=false
white-list=false
enable-rcon=true
rcon.port=$((PORT + 100))
rcon.password=chatfilter
spawn-protection=0
spawn-monsters=false
spawn-animals=false
level-type=flat
motd=ChatFilter test $V
PROPS
  # CFStaff is the op (alerts, no chatfilter.see). Offline-mode UUID: v3 of "OfflinePlayer:<name>".
  echo '[{"uuid":"'"$(python3 -c 'import hashlib,uuid;h=bytearray(hashlib.md5(b"OfflinePlayer:CFStaff").digest());h[6]=h[6]&15|48;h[8]=h[8]&63|128;print(uuid.UUID(bytes=bytes(h)))')"'","name":"CFStaff","level":4,"bypassesPlayerLimit":false}]' > "$D/ops.json"
  echo "prepared $V on $PORT"
done
