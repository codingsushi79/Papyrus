#!/usr/bin/env bash
# Recommended production startup script for Papyrus by SushiMC.
# Adjust -Xms/-Xmx for your host; keeping them equal avoids heap resize pauses.

JAVA="${JAVA:-java}"
JAR="${JAR:-papyrus-paperclip.jar}"

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
