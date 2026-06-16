#!/usr/bin/env python3
"""Apply Papyrus anticheat hooks to version-specific patch files using multi-line anchors."""

from __future__ import annotations

import sys
from pathlib import Path

# Each rule: match consecutive lines in patch file, insert `lines` after the match block.
RULES: dict[str, list[dict]] = {
    "net/minecraft/server/players/PlayerList.java.patch": [
        {
            "match": [
                "+        // CraftBukkit end",
                "+",
                "+        // CraftBukkit start - sendAll above replaced with this loop",
            ],
            "insert_at": -1,
            "lines": [
                "+        io.papermc.paper.anticheat.client.ClientIntegrityHandler.onPlayerJoin(player); // Papyrus - client integrity",
            ],
        },
        {
            "match": [
                "+        this.cserver.getPluginManager().callEvent(playerQuitEvent);",
            ],
            "insert_at": 1,
            "lines": [
                "+        io.papermc.paper.anticheat.PapyrusAnticheat.onPlayerQuit(player); // Papyrus - anticheat",
                "+        io.papermc.paper.anticheat.client.ClientIntegrityHandler.onPlayerQuit(player); // Papyrus - client integrity",
            ],
        },
    ],
    "net/minecraft/server/level/ServerPlayerGameMode.java.patch": [
        {
            "match": [
                "+        // Paper end - Trigger bee_nest_destroyed trigger in the correct place",
                "+        // CraftBukkit end",
                "+",
            ],
            "insert_at": 3,
            "lines": [
                "+        // Papyrus start - integrated anticheat",
                "+        if (changed && block != null) {",
                "+            io.papermc.paper.anticheat.PapyrusAnticheat.onBlockBroken(this.player, block);",
                "+        }",
                "+        // Papyrus end - integrated anticheat",
                "+",
            ],
        },
    ],
    "net/minecraft/server/network/ServerGamePacketListenerImpl.java.patch": [
        {
            "match": [
                "     public void handleMovePlayer(final ServerboundMovePlayerPacket packet) {",
                "         PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());",
            ],
            "insert_at": 2,
            "lines": [
                "+        io.papermc.paper.anticheat.PapyrusAnticheat.onIncomingPacket(this.player); // Papyrus - anticheat packet budget",
            ],
        },
        {
            "match": [
                "     public void handleMovePlayer(ServerboundMovePlayerPacket packet) {",
                "         PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());",
            ],
            "insert_at": 2,
            "lines": [
                "+        io.papermc.paper.anticheat.PapyrusAnticheat.onIncomingPacket(this.player); // Papyrus - anticheat packet budget",
            ],
        },
        {
            "match": [
                "+                                zDist = targetZ - this.lastGoodZ; // Paper - diff on change, used for checking large move vectors above",
            ],
            "insert_at": 1,
            "lines": [
                "+                                io.papermc.paper.anticheat.PapyrusAnticheat.onMovement(this.player, xDist, yDist, zDist, Math.max(1, this.receivedMovePacketCount - this.knownMovePacketCount)); // Papyrus - anticheat movement",
            ],
        },
        {
            "match": [
                "+                                d5 = d2 - this.lastGoodZ; // Paper - diff on change, used for checking large move vectors above",
            ],
            "insert_at": 1,
            "lines": [
                "+                                io.papermc.paper.anticheat.PapyrusAnticheat.onMovement(this.player, d3, d4, d5, Math.max(1, this.receivedMovePacketCount - this.knownMovePacketCount)); // Papyrus - anticheat movement",
            ],
        },
        {
            "match": [
                "                 case SWAP_ITEM_WITH_OFFHAND:",
                "                     if (!this.player.isSpectator()) {",
            ],
            "insert_at": 2,
            "lines": [
                "+                        if (!io.papermc.paper.anticheat.PapyrusAnticheat.onHandSwap(this.player)) { // Papyrus - anticheat",
                "+                            return;",
                "+                        }",
            ],
        },
        {
            "match": [
                "+                    // Paper end - Don't allow digging into unloaded chunks",
            ],
            "insert_at": 1,
            "lines": [
                "+                    if (!io.papermc.paper.anticheat.PapyrusAnticheat.onBlockBreakStart(this.player, pos)) { // Papyrus - anticheat",
                "+                        this.player.connection.ackBlockChangesUpTo(packet.getSequence());",
                "+                        return;",
                "+                    }",
            ],
        },
        {
            "match": [
                "+        if (!this.checkLimit(packet.timestamp)) return; // Spigot - check limit",
            ],
            "insert_at": 1,
            "lines": [
                "+        io.papermc.paper.anticheat.PapyrusAnticheat.onIncomingPacket(this.player); // Papyrus - anticheat",
            ],
        },
        {
            "match": [
                "+                                // Paper end - Allow using signs inside spawn protection",
            ],
            "insert_at": 1,
            "lines": [
                "+                                if (!io.papermc.paper.anticheat.PapyrusAnticheat.onUseItemOn(this.player, blockHit)) { // Papyrus - anticheat",
                "+                                    return;",
                "+                                }",
            ],
        },
        {
            "match": [
                "+                            if (this.awaitingPositionFromClient == null && (serverLevel.mayInteract(this.player, blockPos) || (serverLevel.paperConfig().spawn.allowUsingSignsInsideSpawnProtection && serverLevel.getBlockState(blockPos).getBlock() instanceof net.minecraft.world.level.block.SignBlock))) { // Paper - Allow using signs inside spawn protection",
            ],
            "insert_at": 1,
            "lines": [
                "+                                if (!io.papermc.paper.anticheat.PapyrusAnticheat.onUseItemOn(this.player, blockHit)) { // Papyrus - anticheat",
                "+                                    return;",
                "+                                }",
            ],
        },
        {
            "match": [
                "+        if (this.player.isImmobile()) return; // CraftBukkit",
            ],
            "insert_at": 1,
            "lines": [
                "+        if (!io.papermc.paper.anticheat.PapyrusAnticheat.onInventoryClick(this.player)) return; // Papyrus - anticheat",
            ],
            "prefix_before": "     public void handleContainerClick",
        },
        {
            "match": [
                "     public void handleCustomPayload(final ServerboundCustomPayloadPacket packet) {",
            ],
            "insert_at": 1,
            "lines": [
                "+        if (io.papermc.paper.anticheat.client.ClientIntegrityHandler.handlePayload(this, packet.payload())) {",
                "+            return;",
                "+        }",
            ],
        },
        {
            "match": [
                "     public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {",
            ],
            "insert_at": 1,
            "lines": [
                "+        if (io.papermc.paper.anticheat.client.ClientIntegrityHandler.handlePayload(this, packet.payload())) {",
                "+            return;",
                "+        }",
            ],
        },
    ],
    "net/minecraft/world/entity/Entity.java.patch": [
        {
            "match": [
                "-    private final RandomSource random = RandomSource.create();",
            ],
            "insert_at": 1,
            "lines": [
                "+    public RandomSource random; // Paper/Papyrus - configurable entity random source; initialized in constructor",
            ],
        },
        {
            "match": [
                "+        this.level = level;",
            ],
            "insert_at": 1,
            "lines": [
                "+        this.random = io.papermc.paper.util.EntityRandomSources.createForEntity(); // Papyrus - configurable entity random source",
            ],
            "prefix_before": "     public Entity(EntityType",
        },
    ],
    "net/minecraft/world/entity/ExperienceOrb.java.patch": [
        {
            "match": [
                "-                this.discard();",
            ],
            "insert_at": 0,
            "lines": [
                "+            if (this.age >= level().paperConfig().environment.experienceOrbDespawnRate) { // Papyrus - configurable orb despawn rate",
            ],
            "suffix_after": "+                this.discard();",
        },
        {
            "match": [
                "-            Player nearestPlayer = this.level().getNearestPlayer(this, 8.0);",
            ],
            "insert_at": 1,
            "lines": [
                "+            Player nearestPlayer = this.level().getNearestPlayer(this, this.level().paperConfig().environment.experienceOrbPickupRadius); // Papyrus - configurable pickup radius",
            ],
        },
        {
            "match": [
                "     private void tryMerge(final ExperienceOrb other) {",
            ],
            "insert_at": 1,
            "lines": [
                "+        if (level.paperConfig().entities.behavior.disableExperienceOrbMerge) { // Papyrus - optional orb merge disable",
                "+            return;",
                "+        }",
                "+        final double mergeRadius = level.paperConfig().entities.spawning.experienceOrbMergeRadius; // Papyrus - configurable merge radius",
            ],
        },
    ],
}


def find_sequence(lines: list[str], match: list[str], prefix_before: str | None = None) -> int | None:
    for i in range(len(lines) - len(match) + 1):
        if lines[i : i + len(match)] != match:
            continue
        if prefix_before:
            window = "\n".join(lines[max(0, i - 20) : i])
            if prefix_before not in window:
                continue
        return i
    return None


def apply_rules(path: Path) -> bool:
    rel = str(path.relative_to(path.parents[2])) if path.parts[-4:-1] == ("papyrus-server", "patches", "sources") else path.name
    # rel like net/minecraft/...
    rel = "/".join(path.parts[path.parts.index("sources") + 1 :])
    rules = RULES.get(rel, [])
    if not rules:
        return False

    lines = path.read_text().splitlines()
    changed = False
    for rule in rules:
        match = rule["match"]
        insert_lines = rule["lines"]
        idx = find_sequence(lines, match, rule.get("prefix_before"))
        if idx is None:
            print(f"WARNING: anchor not found in {rel}: {match[0]!r}", file=sys.stderr)
            continue
        insert_at = idx + rule.get("insert_at", len(match))
        block = "\n".join(insert_lines)
        if block in "\n".join(lines):
            continue
        if any(
            line in lines
            for line in insert_lines
            if "Papyrus" in line or "io.papermc.paper.anticheat" in line
        ):
            continue
        for offset, line in enumerate(insert_lines):
            lines.insert(insert_at + offset, line)
        changed = True

    if changed:
        path.write_text("\n".join(lines) + "\n")
    return changed


def main() -> int:
    root = Path("papyrus-server/patches/sources")
    if len(sys.argv) > 1:
        files = [root / sys.argv[1]]
    else:
        files = [root / rel for rel in RULES]
    for path in files:
        apply_rules(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
