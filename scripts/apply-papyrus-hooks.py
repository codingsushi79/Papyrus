#!/usr/bin/env python3
"""Apply Papyrus anticheat hooks to version-specific patch files using multi-line anchors."""

from __future__ import annotations

import sys
from pathlib import Path

# Each rule: match consecutive lines in patch file, then either:
# - insert `lines` after the match block (insert_at, default len(match)), or
# - replace the match block with `replace` when provided.
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
                "             if (this.age >= 6000) {",
                "-                this.discard();",
                "+                this.discard(org.bukkit.event.entity.EntityRemoveEvent.Cause.DESPAWN); // CraftBukkit - add Bukkit remove cause",
            ],
            "replace": [
                "+            if (this.age >= level().paperConfig().environment.experienceOrbDespawnRate) { // Papyrus - configurable orb despawn rate",
                "+                this.discard(org.bukkit.event.entity.EntityRemoveEvent.Cause.DESPAWN); // CraftBukkit - add Bukkit remove cause",
            ],
        },
        {
            "match": [
                "-            if (this.age >= 6000) {",
                "-                this.discard();",
            ],
            "replace": [
                "+            if (this.age >= level().paperConfig().environment.experienceOrbDespawnRate) { // Papyrus - configurable orb despawn rate",
                "+                this.discard(org.bukkit.event.entity.EntityRemoveEvent.Cause.DESPAWN); // CraftBukkit - add Bukkit remove cause",
            ],
        },
        {
            "match": [
                "             Player nearestPlayer = this.level().getNearestPlayer(this, 8.0);",
            ],
            "replace": [
                "+            Player nearestPlayer = this.level().getNearestPlayer(this, this.level().paperConfig().environment.experienceOrbPickupRadius); // Papyrus - configurable pickup radius",
            ],
        },
        {
            "match": [
                "-            Player nearestPlayer = this.level().getNearestPlayer(this, 8.0);",
            ],
            "replace": [
                "+            Player nearestPlayer = this.level().getNearestPlayer(this, this.level().paperConfig().environment.experienceOrbPickupRadius); // Papyrus - configurable pickup radius",
            ],
        },
        {
            "match": [
                "     private static boolean tryMergeToExisting(ServerLevel level, Vec3 pos, int amount) {",
                "         AABB aabb = AABB.ofSize(pos, 1.0, 1.0, 1.0);",
            ],
            "replace": [
                "     private static boolean tryMergeToExisting(ServerLevel level, Vec3 pos, int amount) {",
                "+        if (level.paperConfig().entities.behavior.disableExperienceOrbMerge) { // Papyrus - optional orb merge disable",
                "+            return false;",
                "+        }",
                "+        // Paper - TODO some other event for this kind of merge",
                "+        final double mergeRadius = level.paperConfig().entities.spawning.experienceOrbMergeRadius; // Papyrus - configurable merge radius",
                "+        AABB aabb = AABB.ofSize(pos, mergeRadius, mergeRadius, mergeRadius);",
            ],
        },
        {
            "match": [
                "     private static boolean tryMergeToExisting(ServerLevel level, Vec3 pos, int amount) {",
                "+        // Paper - TODO some other event for this kind of merge",
                "         AABB aabb = AABB.ofSize(pos, 1.0, 1.0, 1.0);",
            ],
            "replace": [
                "     private static boolean tryMergeToExisting(ServerLevel level, Vec3 pos, int amount) {",
                "+        if (level.paperConfig().entities.behavior.disableExperienceOrbMerge) { // Papyrus - optional orb merge disable",
                "+            return false;",
                "+        }",
                "+        // Paper - TODO some other event for this kind of merge",
                "+        final double mergeRadius = level.paperConfig().entities.spawning.experienceOrbMergeRadius; // Papyrus - configurable merge radius",
                "+        AABB aabb = AABB.ofSize(pos, mergeRadius, mergeRadius, mergeRadius);",
            ],
        },
        {
            "match": [
                "     private static boolean tryMergeToExisting(final ServerLevel level, final Vec3 pos, final int value) {",
                "-        AABB box = AABB.ofSize(pos, 1.0, 1.0, 1.0);",
            ],
            "replace": [
                "     private static boolean tryMergeToExisting(final ServerLevel level, final Vec3 pos, final int value) {",
                "+        if (level.paperConfig().entities.behavior.disableExperienceOrbMerge) { // Papyrus - optional orb merge disable",
                "+            return false;",
                "+        }",
                "+        // Paper - TODO some other event for this kind of merge",
                "+        final double mergeRadius = level.paperConfig().entities.spawning.experienceOrbMergeRadius; // Papyrus - configurable merge radius",
                "+        AABB box = AABB.ofSize(pos, mergeRadius, mergeRadius, mergeRadius);",
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


def already_applied(lines: list[str], rule: dict) -> bool:
    markers = rule.get("skip_if_present", [])
    if markers:
        text = "\n".join(lines)
        return any(marker in text for marker in markers)
    insert_lines = rule.get("lines", [])
    replace_lines = rule.get("replace", [])
    candidates = insert_lines + replace_lines
    return any(line in lines for line in candidates if "Papyrus" in line or "io.papermc.paper.anticheat" in line)


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
        if already_applied(lines, rule):
            continue
        idx = find_sequence(lines, match, rule.get("prefix_before"))
        if idx is None:
            if not rule.get("optional"):
                print(f"WARNING: anchor not found in {rel}: {match[0]!r}", file=sys.stderr)
            continue

        if "replace" in rule:
            replace_lines = rule["replace"]
            del lines[idx : idx + len(match)]
            for offset, line in enumerate(replace_lines):
                lines.insert(idx + offset, line)
            changed = True
            continue

        insert_lines = rule.get("lines", [])
        if not insert_lines:
            continue
        insert_at = idx + rule.get("insert_at", len(match))
        block = "\n".join(insert_lines)
        if block in "\n".join(lines):
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
