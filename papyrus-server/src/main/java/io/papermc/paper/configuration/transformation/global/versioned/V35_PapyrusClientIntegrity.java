package io.papermc.paper.configuration.transformation.global.versioned;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

public class V35_PapyrusClientIntegrity implements ConfigurationTransformation {
    public static final V35_PapyrusClientIntegrity INSTANCE = new V35_PapyrusClientIntegrity();
    private static final int VERSION = 35;

    public static void apply(final ConfigurationTransformation.VersionedBuilder builder) {
        builder.addVersion(VERSION, INSTANCE);
    }

    @Override
    public void apply(final ConfigurationNode root) throws ConfigurateException {
        // anticheat.client-integrity uses defaults (disabled until admin enables).
    }
}
