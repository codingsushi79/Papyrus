package io.papermc.paper.anticheat;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

final class AnticheatPlayerState {

    private final UUID playerId;
    private final RollingCounter packetsPerSecond = new RollingCounter(1_000L);
    private final RollingCounter blockBreaksPerSecond = new RollingCounter(1_000L);
    private final RollingCounter blockPlacesPerSecond = new RollingCounter(1_000L);
    private final RollingCounter inventoryClicksPerSecond = new RollingCounter(1_000L);
    private final RollingCounter handSwapsPerSecond = new RollingCounter(1_000L);
    private final RollingCounter valuableOresWindow = new RollingCounter(600_000L);
    private final RollingCounter commonBlocksWindow = new RollingCounter(600_000L);

    private float violationLevel;
    private long lastViolationDecayMillis = System.currentTimeMillis();

    AnticheatPlayerState(final ServerPlayer player) {
        this.playerId = player.getUUID();
    }

    UUID playerId() {
        return this.playerId;
    }

    int recordPacket() {
        return this.packetsPerSecond.record();
    }

    int recordBlockBreak() {
        return this.blockBreaksPerSecond.record();
    }

    int recordBlockPlace() {
        return this.blockPlacesPerSecond.record();
    }

    int recordInventoryClick() {
        return this.inventoryClicksPerSecond.record();
    }

    int recordHandSwap() {
        return this.handSwapsPerSecond.record();
    }

    int recordValuableOre() {
        return this.valuableOresWindow.record();
    }

    int recordCommonBlock() {
        return this.commonBlocksWindow.record();
    }

    int packetsPerSecond() {
        return this.packetsPerSecond.count();
    }

    int blockBreaksPerSecond() {
        return this.blockBreaksPerSecond.count();
    }

    int blockPlacesPerSecond() {
        return this.blockPlacesPerSecond.count();
    }

    int inventoryClicksPerSecond() {
        return this.inventoryClicksPerSecond.count();
    }

    int handSwapsPerSecond() {
        return this.handSwapsPerSecond.count();
    }

    int valuableOresInWindow(final long windowMillis) {
        return this.valuableOresWindow.countWithin(windowMillis);
    }

    int commonBlocksInWindow(final long windowMillis) {
        return this.commonBlocksWindow.countWithin(windowMillis);
    }

    float violationLevel() {
        return this.violationLevel;
    }

    float addViolation(final float amount) {
        this.decayViolations();
        this.violationLevel += amount;
        return this.violationLevel;
    }

    void decayViolations() {
        final long now = System.currentTimeMillis();
        final float decayPerSecond = io.papermc.paper.configuration.GlobalConfiguration.get().anticheat.engine.punishments.violationDecayPerSecond;
        if (decayPerSecond <= 0F) {
            this.lastViolationDecayMillis = now;
            return;
        }
        final float elapsedSeconds = (now - this.lastViolationDecayMillis) / 1000F;
        if (elapsedSeconds > 0F) {
            this.violationLevel = Math.max(0F, this.violationLevel - decayPerSecond * elapsedSeconds);
            this.lastViolationDecayMillis = now;
        }
    }
}
