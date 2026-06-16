#!/usr/bin/env bash
# Re-apply Papyrus patch hooks on an existing papyrus/* branch.
set -euo pipefail

MC_VERSION="${1:?Usage: $0 <mc-version> <paper-ref>}"
REF="${2:?Usage: $0 <mc-version> <paper-ref>}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PATCH_FILES=(
  net/minecraft/server/network/ServerGamePacketListenerImpl.java.patch
  net/minecraft/server/level/ServerPlayerGameMode.java.patch
  net/minecraft/server/players/PlayerList.java.patch
  net/minecraft/world/entity/Entity.java.patch
  net/minecraft/world/entity/ExperienceOrb.java.patch
)

for patch_file in "${PATCH_FILES[@]}"; do
  git show "${REF}:paper-server/patches/sources/${patch_file}" > "papyrus-server/patches/sources/${patch_file}"
done

python3 scripts/apply-papyrus-hooks.py

git add papyrus-server/patches/sources/
git commit -m "Fix Papyrus anticheat patch hook placement for Minecraft ${MC_VERSION}."
