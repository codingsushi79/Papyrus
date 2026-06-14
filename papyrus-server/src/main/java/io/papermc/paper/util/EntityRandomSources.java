package io.papermc.paper.util;

import io.papermc.paper.configuration.GlobalConfiguration;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * Provides entity {@link RandomSource} instances based on server configuration.
 */
public final class EntityRandomSources {

    private static volatile boolean useVanilla;

    private EntityRandomSources() {
    }

    public static void refresh() {
        final GlobalConfiguration configuration = GlobalConfiguration.get();
        useVanilla = configuration != null
            && configuration.performance.entityRandomSource == GlobalConfiguration.Performance.EntityRandomSource.VANILLA;
    }

    public static RandomSource createForEntity() {
        if (useVanilla) {
            return RandomSource.create();
        }
        return Entity.SHARED_RANDOM;
    }
}
