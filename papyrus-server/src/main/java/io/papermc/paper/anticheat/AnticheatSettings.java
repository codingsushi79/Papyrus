package io.papermc.paper.anticheat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Runtime anticheat settings. On current Paper/Papyrus releases these are copied from
 * {@code paper-global.yml}; on older backports the defaults below are used.
 */
public final class AnticheatSettings {

    public static final Engine ENGINE = new Engine();
    public static final ClientIntegrity CLIENT_INTEGRITY = new ClientIntegrity();

    static {
        resolveDefaultOres();
    }

    private AnticheatSettings() {
    }

    private static void resolveDefaultOres() {
        final Set<Block> blocks = new HashSet<>();
        for (final String id : List.of(
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:deepslate_emerald_ore",
            "minecraft:ancient_debris",
            "minecraft:gold_ore",
            "minecraft:deepslate_gold_ore",
            "minecraft:nether_gold_ore",
            "minecraft:lapis_ore",
            "minecraft:deepslate_lapis_ore",
            "minecraft:redstone_ore",
            "minecraft:deepslate_redstone_ore"
        )) {
            final Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                continue;
            }
            final Block block = BuiltInRegistries.BLOCK.getValue(identifier);
            if (block != null && block != Blocks.AIR) {
                blocks.add(block);
            }
        }
        PapyrusAnticheat.bindTrackedOres(Set.copyOf(blocks));
    }

    public static final class Engine {
        public boolean enabled = true;
        public final Alerts alerts = new Alerts();
        public final Punishments punishments = new Punishments();
        public final Checks checks = new Checks();
    }

    public static final class Alerts {
        public boolean enabled = true;
        public boolean console = true;
        public boolean notifyOps = false;
    }

    public static final class Punishments {
        public boolean kickEnabled = true;
        public float kickViolationLevel = 40.0F;
        public Component kickMessage = Component.text("Unfair advantage detected");
        public float violationDecayPerSecond = 2.0F;
    }

    public static final class Checks {
        public final Xray xray = new Xray();
        public final RateLimit fastPlace = new RateLimit(12, 4.0F);
        public final RateLimit fastBreak = new RateLimit(8, 4.0F);
        public final Inventory inventory = new Inventory();
        public final HandSwap handSwap = new HandSwap();
        public final Reach reach = new Reach();
        public final Movement movement = new Movement();
        public final Timer timer = new Timer();
    }

    public static final class Xray {
        public boolean enabled = true;
        public int windowSeconds = 300;
        public int suspiciousOreCount = 14;
        public int minOresForRatioCheck = 6;
        public float maxOreToStoneRatio = 0.35F;
        public float violationWeight = 8.0F;
    }

    public static final class RateLimit {
        public boolean enabled = true;
        public int maxPerSecond;
        public float violationWeight;
        public boolean cancelAction = true;

        public RateLimit(final int maxPerSecond, final float violationWeight) {
            this.maxPerSecond = maxPerSecond;
            this.violationWeight = violationWeight;
        }
    }

    public static final class Inventory {
        public boolean enabled = true;
        public int maxClicksPerSecond = 25;
        public float violationWeight = 3.0F;
        public boolean cancelAction = true;
    }

    public static final class HandSwap {
        public boolean enabled = true;
        public int maxSwapsPerSecond = 8;
        public float violationWeight = 3.0F;
        public boolean cancelAction = true;
    }

    public static final class Reach {
        public boolean enabled = true;
        public double blockExtraDistance = 0.35;
        public float violationWeight = 10.0F;
    }

    public static final class Movement {
        public boolean enabled = true;
        public double maxHorizontalBlocksPerTick = 0.85;
        public double maxVerticalBlocksPerTick = 1.2;
        public double leniencyMultiplier = 1.15;
        public float violationWeight = 5.0F;
        public boolean setback = true;
    }

    public static final class Timer {
        public boolean enabled = true;
        public int maxPacketsPerSecond = 140;
        public float violationWeight = 6.0F;
    }

    public static final class ClientIntegrity {
        public boolean enabled = false;
        public boolean requirePapyrusClient = true;
        public boolean requireShieldMod = true;
        public int responseTimeoutSeconds = 15;
        public String downloadUrl = "https://docs.sushii.dev/papyrus-client/download";
        public String bannedModMessage = "Banned client mod detected: {mod}";
        public final Alerts alerts = new Alerts();
        public List<String> bannedModIds = List.of(
            "meteor-client", "wurst", "aristois", "impact", "inertia", "liquidbounce", "sigma", "future",
            "kami", "rusherhack", "phobos", "seppuku", "wwe", "pyro", "konas", "gamesense", "salhack"
        );
    }
}
