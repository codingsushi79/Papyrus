package io.papermc.paper.configuration.transformation.global.versioned;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

public class V33_PapyrusPerformanceOptions implements ConfigurationTransformation {
    public static final V33_PapyrusPerformanceOptions INSTANCE = new V33_PapyrusPerformanceOptions();
    private static final int VERSION = 33;

    public static void apply(final ConfigurationTransformation.VersionedBuilder builder) {
        builder.addVersion(VERSION, INSTANCE);
    }

    @Override
    public void apply(final ConfigurationNode root) throws ConfigurateException {
        // New performance.apply-runtime-jvm-defaults and performance.netty-threads use defaults.
    }
}
