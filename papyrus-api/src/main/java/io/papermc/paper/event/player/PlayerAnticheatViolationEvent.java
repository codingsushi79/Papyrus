package io.papermc.paper.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Called when Papyrus integrated anticheat detects suspicious player behavior.
 */
@NullMarked
public class PlayerAnticheatViolationEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final String checkType;
    private final float addedViolationLevel;
    private final float totalViolationLevel;
    private final String detail;
    private boolean cancelled;

    @ApiStatus.Internal
    public PlayerAnticheatViolationEvent(
        final Player player,
        final String checkType,
        final float addedViolationLevel,
        final float totalViolationLevel,
        final String detail
    ) {
        super(player);
        this.checkType = checkType;
        this.addedViolationLevel = addedViolationLevel;
        this.totalViolationLevel = totalViolationLevel;
        this.detail = detail;
    }

    public String getCheckType() {
        return this.checkType;
    }

    public float getAddedViolationLevel() {
        return this.addedViolationLevel;
    }

    public float getTotalViolationLevel() {
        return this.totalViolationLevel;
    }

    public String getDetail() {
        return this.detail;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
