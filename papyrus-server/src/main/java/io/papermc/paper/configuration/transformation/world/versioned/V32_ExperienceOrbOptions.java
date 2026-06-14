package io.papermc.paper.configuration.transformation.world.versioned;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

public class V32_ExperienceOrbOptions implements ConfigurationTransformation {
    public static final V32_ExperienceOrbOptions INSTANCE = new V32_ExperienceOrbOptions();
    private static final int VERSION = 32;

    public static void apply(final ConfigurationTransformation.VersionedBuilder builder) {
        builder.addVersion(VERSION, INSTANCE);
    }

    @Override
    public void apply(final ConfigurationNode root) throws ConfigurateException {
        // New experience orb options use defaults from WorldConfiguration.
    }
}
