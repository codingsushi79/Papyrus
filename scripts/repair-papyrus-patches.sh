#!/usr/bin/env bash
# Re-apply Papyrus patch hooks after restoring version-base patches from a Paper ref.
set -euo pipefail

MC_VERSION="${1:?Usage: $0 <mc-version> <paper-ref> [papyrus-source-ref]}"
PAPER_REF="${2:?Usage: $0 <mc-version> <paper-ref> [papyrus-source-ref]}"
PAPYRUS_SOURCE_REF="${3:-main}"
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
  git show "${PAPER_REF}:paper-server/patches/sources/${patch_file}" > "papyrus-server/patches/sources/${patch_file}"
done

git checkout "$PAPYRUS_SOURCE_REF" -- scripts/apply-papyrus-hooks.py
python3 scripts/apply-papyrus-hooks.py

echo "==> Repaired patch hooks for Minecraft ${MC_VERSION} (not committed)"
