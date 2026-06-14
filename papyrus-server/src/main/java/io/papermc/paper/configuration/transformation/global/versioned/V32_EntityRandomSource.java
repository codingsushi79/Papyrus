package io.papermc.paper.configuration.transformation.global.versioned;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

public class V32_EntityRandomSource implements ConfigurationTransformation {
    public static final V32_EntityRandomSource INSTANCE = new V32_EntityRandomSource();
    private static final int VERSION = 32;

    public static void apply(final ConfigurationTransformation.VersionedBuilder builder) {
        builder.addVersion(VERSION, INSTANCE);
    }

    @Override
    public void apply(final ConfigurationNode root) throws ConfigurateException {
        // New performance.entity-random-source option uses defaults from GlobalConfiguration.
    }
}
