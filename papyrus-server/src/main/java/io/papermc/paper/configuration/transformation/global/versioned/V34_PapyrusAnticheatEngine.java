package io.papermc.paper.configuration.transformation.global.versioned;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

public class V34_PapyrusAnticheatEngine implements ConfigurationTransformation {
    public static final V34_PapyrusAnticheatEngine INSTANCE = new V34_PapyrusAnticheatEngine();
    private static final int VERSION = 34;

    public static void apply(final ConfigurationTransformation.VersionedBuilder builder) {
        builder.addVersion(VERSION, INSTANCE);
    }

    @Override
    public void apply(final ConfigurationNode root) throws ConfigurateException {
        // anticheat.engine section uses defaults for new installs and upgrades.
    }
}
