#!/usr/bin/env bash
# Recommended production startup script for Papyrus by SushiMC.
# Adjust -Xms/-Xmx for your host; keeping them equal avoids heap resize pauses.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA="${JAVA:-java}"
JAR="${JAR:-papyrus-paperclip.jar}"
PAPYRUS_AUTO_UPDATE="${PAPYRUS_AUTO_UPDATE:-1}"

apply_pending_update() {
  local jar="$1"
  local pending="${jar}.update"
  if [[ ! -f "$pending" ]]; then
    return 0
  fi

  echo "Applying pending Papyrus update from ${pending}..."
  if [[ -f "$jar" ]]; then
    cp -f "$jar" "${jar}.backup"
  fi
  mv -f "$pending" "$jar"
  echo "Papyrus update applied."
}

if [[ -f "$JAR" ]]; then
  apply_pending_update "$JAR"
  if [[ "$PAPYRUS_AUTO_UPDATE" != "0" ]]; then
    "$SCRIPT_DIR/update-papyrus.sh" "$JAR" || echo "Papyrus auto-update check failed; continuing with current jar." >&2
  fi
fi

exec "$JAVA" \
  -Xms8G -Xmx8G \
  -XX:+UseG1GC \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+DisableExplicitGC \
  -XX:+AlwaysPreTouch \
  -XX:G1NewSizePercent=30 \
  -XX:G1MaxNewSizePercent=40 \
  -XX:G1HeapRegionSize=8M \
  -XX:G1ReservePercent=20 \
  -XX:InitiatingHeapOccupancyPercent=15 \
  -XX:MaxTenuringThreshold=1 \
  -XX:+PerfDisableSharedMem \
  -Dusing.aikars.flags=https://github.com/codingsushi79/Papyrus \
  -jar "$JAR" nogui "$@"
