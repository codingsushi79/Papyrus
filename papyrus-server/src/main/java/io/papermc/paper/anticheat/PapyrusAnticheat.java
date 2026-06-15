package io.papermc.paper.anticheat;

import com.mojang.logging.LogUtils;
import io.papermc.paper.configuration.GlobalConfiguration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.slf4j.Logger;

public final class PapyrusAnticheat {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, AnticheatPlayerState> STATES = new ConcurrentHashMap<>();
    private static Set<Block> trackedOreBlocks = Set.of();

    private PapyrusAnticheat() {
    }

    public static void bindTrackedOres(final Set<Block> ores) {
        trackedOreBlocks = Set.copyOf(ores);
    }

    public static void onPlayerQuit(final ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static void onIncomingPacket(final ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }
        final GlobalConfiguration.Anticheat.Engine.Checks.Timer timer = config().checks.timer;
        if (!timer.enabled) {
            return;
        }
        final int count = state(player).recordPacket();
        if (count > timer.maxPacketsPerSecond) {
            flag(player, ViolationType.TIMER, timer.violationWeight,
                "packet-rate=" + count + "/s (max " + timer.maxPacketsPerSecond + ")");
        }
    }

    public static boolean onBlockBreakStart(final ServerPlayer player, final BlockPos pos) {
        if (!isActive(player)) {
            return true;
        }
        onIncomingPacket(player);
        if (!config().checks.reach.enabled) {
            return true;
        }
        return validateReach(player, Vec3.atCenterOf(pos), config().checks.reach.blockExtraDistance, ViolationType.REACH, "block-break");
    }

    public static void onBlockBroken(final ServerPlayer player, final Block block) {
        if (!isActive(player)) {
            return;
        }
        final GlobalConfiguration.Anticheat.Engine.Checks checkConfig = config().checks;
        final AnticheatPlayerState playerState = state(player);

        if (checkConfig.fastBreak.enabled) {
            final int breaks = playerState.recordBlockBreak();
            if (breaks > checkConfig.fastBreak.maxPerSecond) {
                flag(player, ViolationType.FAST_BREAK, checkConfig.fastBreak.violationWeight,
                    "break-rate=" + breaks + "/s (max " + checkConfig.fastBreak.maxPerSecond + ")");
            }
        }

        if (checkConfig.xray.enabled && trackedOreBlocks.contains(block)) {
            final int ores = playerState.recordValuableOre();
            final long windowMillis = checkConfig.xray.windowSeconds * 1000L;
            final int oresInWindow = playerState.valuableOresInWindow(windowMillis);
            if (oresInWindow > checkConfig.xray.suspiciousOreCount) {
                flag(player, ViolationType.XRAY, checkConfig.xray.violationWeight,
                    "ores-mined=" + oresInWindow + " in " + checkConfig.xray.windowSeconds + "s (max " + checkConfig.xray.suspiciousOreCount + ")");
            }
            final int common = playerState.commonBlocksInWindow(windowMillis);
            if (ores >= checkConfig.xray.minOresForRatioCheck
                && common > 0
                && (float) ores / common > checkConfig.xray.maxOreToStoneRatio) {
                flag(player, ViolationType.XRAY, checkConfig.xray.violationWeight,
                    "ore-ratio=" + ores + "/" + common + " (max ratio " + checkConfig.xray.maxOreToStoneRatio + ")");
            }
        } else if (checkConfig.xray.enabled && isCommonStone(block)) {
            playerState.recordCommonBlock();
        }
    }

    public static boolean onUseItemOn(final ServerPlayer player, final BlockHitResult hit) {
        if (!isActive(player)) {
            return true;
        }
        onIncomingPacket(player);

        if (config().checks.reach.enabled) {
            if (!validateReach(player, hit.getLocation(), config().checks.reach.blockExtraDistance, ViolationType.REACH, "block-interact")) {
                return false;
            }
        }

        if (config().checks.fastPlace.enabled) {
            final int places = state(player).recordBlockPlace();
            if (places > config().checks.fastPlace.maxPerSecond) {
                flag(player, ViolationType.FAST_PLACE, config().checks.fastPlace.violationWeight,
                    "place-rate=" + places + "/s (max " + config().checks.fastPlace.maxPerSecond + ")");
                if (config().checks.fastPlace.cancelAction) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean onInventoryClick(final ServerPlayer player) {
        if (!isActive(player)) {
            return true;
        }
        onIncomingPacket(player);
        final GlobalConfiguration.Anticheat.Engine.Checks.Inventory inventory = config().checks.inventory;
        if (!inventory.enabled) {
            return true;
        }
        final int clicks = state(player).recordInventoryClick();
        if (clicks > inventory.maxClicksPerSecond) {
            flag(player, ViolationType.INVENTORY, inventory.violationWeight,
                "inventory-clicks=" + clicks + "/s (max " + inventory.maxClicksPerSecond + ")");
            return !inventory.cancelAction;
        }
        return true;
    }

    public static boolean onHandSwap(final ServerPlayer player) {
        if (!isActive(player)) {
            return true;
        }
        onIncomingPacket(player);
        final GlobalConfiguration.Anticheat.Engine.Checks.HandSwap handSwap = config().checks.handSwap;
        if (!handSwap.enabled) {
            return true;
        }
        final int swaps = state(player).recordHandSwap();
        if (swaps > handSwap.maxSwapsPerSecond) {
            flag(player, ViolationType.HAND_SWAP, handSwap.violationWeight,
                "hand-swaps=" + swaps + "/s (max " + handSwap.maxSwapsPerSecond + ")");
            return !handSwap.cancelAction;
        }
        return true;
    }

    public static void onMovement(final ServerPlayer player, final double deltaX, final double deltaY, final double deltaZ, final int movePackets) {
        if (!isActive(player)) {
            return;
        }
        final GlobalConfiguration.Anticheat.Engine.Checks.Movement movement = config().checks.movement;
        if (!movement.enabled || player.isCreative() || player.isSpectator()) {
            return;
        }

        final double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        final double vertical = Math.abs(deltaY);
        final float packetBudget = Math.max(1, movePackets);

        final double maxHorizontal = movement.maxHorizontalBlocksPerTick * packetBudget * movement.leniencyMultiplier;
        final double maxVertical = movement.maxVerticalBlocksPerTick * packetBudget * movement.leniencyMultiplier;

        if (horizontal > maxHorizontal || vertical > maxVertical) {
            flag(player, ViolationType.MOVEMENT, movement.violationWeight,
                "delta=" + String.format("%.3f", horizontal) + "h/" + String.format("%.3f", vertical) + "v"
                    + " (max " + maxHorizontal + "h/" + maxVertical + "v)");
            if (movement.setback) {
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            }
        }
    }

    private static boolean validateReach(
        final ServerPlayer player,
        final Vec3 target,
        final double extraDistance,
        final ViolationType type,
        final String context
    ) {
        if (!target.isFinite()) {
            flag(player, ViolationType.INVALID_PACKET, config().checks.reach.violationWeight, context + " invalid-hit-vector");
            return false;
        }
        final Vec3 eye = player.getEyePosition();
        final double distanceSq = eye.distanceToSqr(target);
        final double maxReach = player.blockInteractionRange() + extraDistance;
        if (distanceSq > maxReach * maxReach) {
            flag(player, type, config().checks.reach.violationWeight,
                context + " distance=" + String.format("%.2f", Math.sqrt(distanceSq)) + " (max " + maxReach + ")");
            return false;
        }
        return true;
    }

    private static void flag(final ServerPlayer player, final ViolationType type, final float weight, final String detail) {
        if (!isActive(player)) {
            return;
        }
        final AnticheatPlayerState playerState = state(player);
        playerState.decayViolations();
        final float projectedLevel = playerState.violationLevel() + weight;
        final GlobalConfiguration.Anticheat.Engine engine = config();

        final io.papermc.paper.event.player.PlayerAnticheatViolationEvent event =
            new io.papermc.paper.event.player.PlayerAnticheatViolationEvent(
                player.getBukkitEntity(), type.name(), weight, projectedLevel, detail
            );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        final float level = playerState.addViolation(weight);

        if (engine.alerts.enabled) {
            final String message = "[Papyrus-AC] " + player.getScoreboardName() + " failed " + type.name()
                + " (" + detail + ") VL=" + String.format("%.1f", level);
            if (engine.alerts.console) {
                LOGGER.warn(message);
            }
            if (engine.alerts.notifyOps) {
                Bukkit.getOnlinePlayers().stream()
                    .filter(Player::isOp)
                    .forEach(op -> op.sendMessage(message));
            }
        }

        final GlobalConfiguration.Anticheat.Engine.Punishments punishments = engine.punishments;
        if (punishments.kickEnabled && level >= punishments.kickViolationLevel) {
            player.connection.disconnect(punishments.kickMessage, PlayerKickEvent.Cause.ILLEGAL_ACTION);
            STATES.remove(player.getUUID());
        }
    }

    private static boolean isCommonStone(final Block block) {
        final String key = block.getDescriptionId();
        return key.contains("stone") || key.contains("deepslate") || key.contains("netherrack") || key.contains("andesite")
            || key.contains("diorite") || key.contains("granite") || key.contains("tuff");
    }

    private static AnticheatPlayerState state(final ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new AnticheatPlayerState(player));
    }

    private static GlobalConfiguration.Anticheat.Engine config() {
        return GlobalConfiguration.get().anticheat.engine;
    }

    private static boolean isActive(final ServerPlayer player) {
        final GlobalConfiguration configuration = GlobalConfiguration.get();
        if (configuration == null || !configuration.anticheat.engine.enabled) {
            return false;
        }
        return !player.getBukkitEntity().hasPermission("papyrus.anticheat.bypass");
    }
}
