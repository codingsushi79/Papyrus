package io.papermc.paper.anticheat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import io.papermc.paper.configuration.GlobalConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.event.player.PlayerKickEvent;
import org.slf4j.Logger;

public final class ClientIntegrityHandler {

    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("papyrus", "integrity");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, PendingCheck> PENDING = new ConcurrentHashMap<>();

    private ClientIntegrityHandler() {
    }

    public static void onPlayerJoin(final ServerPlayer player) {
        final GlobalConfiguration.Anticheat.ClientIntegrity config = config();
        if (!config.enabled || player.getBukkitEntity().hasPermission("papyrus.client.bypass")) {
            return;
        }
        PENDING.put(player.getUUID(), new PendingCheck(System.currentTimeMillis() + config.responseTimeoutSeconds * 1000L));
        sendRequest(player);
        final ServerPlayer tracked = player;
        java.util.concurrent.CompletableFuture.delayedExecutor(config.responseTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .execute(() -> net.minecraft.server.MinecraftServer.getServer().execute(() -> timeoutCheck(tracked)));
    }

    public static void onPlayerQuit(final ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static boolean handlePayload(final ServerGamePacketListenerImpl connection, final net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (!(payload instanceof DiscardedPayload discarded)) {
            return false;
        }
        if (!CHANNEL.equals(discarded.id())) {
            return false;
        }
        final ServerPlayer player = connection.player;
        final byte[] data = discarded.data();
        processReport(player, new String(data, StandardCharsets.UTF_8));
        return true;
    }

    private static void processReport(final ServerPlayer player, final String json) {
        final GlobalConfiguration.Anticheat.ClientIntegrity config = config();
        if (!config.enabled) {
            return;
        }
        try {
            final JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.get("v") == null || root.get("v").getAsInt() != 1) {
                rejectMissing(player);
                return;
            }
            if (config.requirePapyrusClient && (!root.has("client") || !"papyrus-client".equals(root.get("client").getAsString()))) {
                rejectMissing(player);
                return;
            }
            if (config.requireShieldMod && (!root.has("shield") || root.get("shield").isJsonNull())) {
                rejectMissing(player);
                return;
            }

            final Set<String> modIds = new HashSet<>();
            if (root.has("mods") && root.get("mods").isJsonArray()) {
                final JsonArray mods = root.getAsJsonArray("mods");
                for (int i = 0; i < mods.size(); i++) {
                    final String entry = mods.get(i).getAsString().toLowerCase(Locale.ROOT);
                    modIds.add(entry.split("@")[0]);
                }
            }

            for (final String banned : config.bannedModIds) {
                final String needle = banned.toLowerCase(Locale.ROOT);
                for (final String modId : modIds) {
                    if (modId.contains(needle)) {
                        reject(player, Component.text(config.bannedModMessage.replace("{mod}", banned), NamedTextColor.RED));
                        return;
                    }
                }
            }

            PENDING.remove(player.getUUID());
            if (config.alerts.console) {
                LOGGER.info("[Papyrus-Client] {} passed integrity check ({} mods reported)", player.getScoreboardName(), modIds.size());
            }
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse client integrity payload from {}", player.getScoreboardName(), ex);
            rejectMissing(player);
        }
    }

    private static void timeoutCheck(final ServerPlayer player) {
        if (PENDING.remove(player.getUUID()) != null) {
            rejectMissing(player);
        }
    }

    private static void reject(final ServerPlayer player, final Component message) {
        PENDING.remove(player.getUUID());
        player.connection.disconnect(message, PlayerKickEvent.Cause.ILLEGAL_ACTION);
    }

    private static void rejectMissing(final ServerPlayer player) {
        final GlobalConfiguration.Anticheat.ClientIntegrity config = config();
        final Component message = Component.text()
            .append(Component.text("This server requires ", NamedTextColor.RED))
            .append(Component.text("Papyrus Client", NamedTextColor.GOLD))
            .append(Component.text(" with client integrity.\nDownload: ", NamedTextColor.RED))
            .append(Component.text(config.downloadUrl, NamedTextColor.AQUA))
            .build();
        reject(player, message);
    }

    private static void sendRequest(final ServerPlayer player) {
        final byte[] payload = "{\"action\":\"request\"}".getBytes(StandardCharsets.UTF_8);
        player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
            new DiscardedPayload(CHANNEL, payload)
        ));
    }

    private static GlobalConfiguration.Anticheat.ClientIntegrity config() {
        return GlobalConfiguration.get().anticheat.clientIntegrity;
    }

    private record PendingCheck(long startedAtMillis) {
    }
}
