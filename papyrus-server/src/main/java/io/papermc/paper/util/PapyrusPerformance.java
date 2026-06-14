package io.papermc.paper.util;

/**
 * Applies safe runtime performance defaults before the server fully boots.
 */
public final class PapyrusPerformance {

    private PapyrusPerformance() {
    }

    public static void applyRuntimeDefaults() {
        // Paper - cap per-thread NIO cache size; https://www.evanjones.ca/java-bytebuffer-leak.html
        if (System.getProperty("jdk.nio.maxCachedBufferSize") == null) {
            System.setProperty("jdk.nio.maxCachedBufferSize", "262144");
        }
        // Reduce retained direct buffer cache between Netty pool returns
        if (System.getProperty("io.netty.allocator.maxCachedBufferCapacity") == null) {
            System.setProperty("io.netty.allocator.maxCachedBufferCapacity", "32768");
        }
        // Avoid JNA native library extraction overhead when unused
        if (System.getProperty("jna.nosys") == null) {
            System.setProperty("jna.nosys", "true");
        }
    }
}
