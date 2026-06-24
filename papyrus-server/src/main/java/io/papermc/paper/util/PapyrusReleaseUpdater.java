package io.papermc.paper.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import io.papermc.paper.ServerBuildInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.OptionalInt;
import org.bukkit.craftbukkit.Main;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

/**
 * Checks GitHub releases for a newer Papyrus jar for the running Minecraft version and optionally
 * downloads it to apply on the next restart.
 */
@NullMarked
public final class PapyrusReleaseUpdater {
    private static final Logger LOGGER = LogUtils.getClassLogger();
    private static final String REPOSITORY = "codingsushi79/Papyrus";
    private static final String RELEASES_API = "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";
    private static final Gson GSON = new Gson();
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 300_000;

    private PapyrusReleaseUpdater() {
    }

    public static void checkAndUpdate(final boolean autoDownload) {
        final ServerBuildInfo buildInfo = ServerBuildInfo.buildInfo();
        if (!buildInfo.brandId().equals(ServerBuildInfo.BRAND_PAPYRUS_ID)) {
            return;
        }

        final Optional<String> gitCommit = buildInfo.gitCommit();
        if (gitCommit.isEmpty()) {
            LOGGER.warn("Skipping Papyrus update check: this build has no Git-Commit metadata.");
            return;
        }

        try {
            final Optional<ReleaseAsset> latest = fetchLatestReleaseAsset(buildInfo.minecraftVersionId());
            if (latest.isEmpty()) {
                LOGGER.info("No GitHub release asset found for Minecraft {}.", buildInfo.minecraftVersionId());
                return;
            }

            final ReleaseAsset release = latest.get();
            final int distance = compareToRelease(gitCommit.get(), release.tagName());
            switch (distance) {
                case -1 -> LOGGER.warn("Could not compare this build to release {}.", release.tagName());
                case 0 -> LOGGER.info("Papyrus is up to date (release {}).", release.tagName());
                default -> handleUpdateAvailable(release, distance, autoDownload);
            }
        } catch (final IOException ex) {
            LOGGER.warn("Failed to check for Papyrus updates: {}", ex.getMessage());
            LOGGER.debug("Papyrus update check failed", ex);
        }
    }

    public static void applyPendingUpdate(final Path jarPath) throws IOException {
        final Path pending = pendingUpdatePath(jarPath);
        if (!Files.isRegularFile(pending)) {
            return;
        }

        final Path backup = jarPath.resolveSibling(jarPath.getFileName().toString() + ".backup");
        if (Files.exists(jarPath)) {
            Files.copy(jarPath, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(pending, jarPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        LOGGER.info("Applied pending Papyrus update from {} (backup at {}).", pending.getFileName(), backup.getFileName());
    }

    public static Path pendingUpdatePath(final Path jarPath) {
        return jarPath.resolveSibling(jarPath.getFileName().toString() + ".update");
    }

    public static Optional<Path> runningJarPath() {
        try {
            final Path path = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(path)) {
                return Optional.of(path.toAbsolutePath().normalize());
            }
        } catch (final Exception ex) {
            LOGGER.debug("Could not resolve running jar path", ex);
        }
        return Optional.empty();
    }

    private static void handleUpdateAvailable(final ReleaseAsset release, final int commitsBehind, final boolean autoDownload) throws IOException {
        LOGGER.warn("*************************************************************************************");
        LOGGER.warn("A newer Papyrus release is available for Minecraft {}: {} ({} commit(s) behind).", release.minecraftVersion(), release.tagName(), commitsBehind);
        LOGGER.warn("Download: {}", release.downloadUrl());
        LOGGER.warn("*************************************************************************************");

        if (!autoDownload) {
            return;
        }

        final Optional<Path> jarPath = runningJarPath();
        if (jarPath.isEmpty()) {
            LOGGER.warn("Could not determine running jar path; download the update manually from {}", release.downloadUrl());
            return;
        }

        final Path target = pendingUpdatePath(jarPath.get());
        if (Files.isRegularFile(target)) {
            LOGGER.info("Pending Papyrus update already downloaded at {}. Restart the server to apply it.", target.getFileName());
            return;
        }

        LOGGER.info("Downloading Papyrus {} for Minecraft {}...", release.tagName(), release.minecraftVersion());
        downloadReleaseAsset(release, target);
        LOGGER.warn("Downloaded Papyrus {} to {}. Restart the server to apply the update.", release.tagName(), target.getFileName());
    }

    public static Optional<ReleaseAsset> fetchLatestReleaseAsset(final String minecraftVersionId) throws IOException {
        final HttpURLConnection connection = openJsonConnection(RELEASES_API);
        try (final InputStream input = connection.getInputStream()) {
            final JsonObject release = GSON.fromJson(new String(input.readAllBytes()), JsonObject.class);
            final String tagName = release.get("tag_name").getAsString();
            final String assetName = "Papyrus-" + minecraftVersionId + ".jar";
            final JsonArray assets = release.getAsJsonArray("assets");
            for (final JsonElement element : assets) {
                final JsonObject asset = element.getAsJsonObject();
                if (assetName.equals(asset.get("name").getAsString())) {
                    return Optional.of(new ReleaseAsset(
                        tagName,
                        minecraftVersionId,
                        asset.get("browser_download_url").getAsString(),
                        asset.get("size").getAsLong()
                    ));
                }
            }
        } catch (final JsonSyntaxException ex) {
            throw new IOException("Invalid GitHub release response", ex);
        } finally {
            connection.disconnect();
        }
        return Optional.empty();
    }

    public static int compareToRelease(final String currentCommit, final String releaseTag) throws IOException {
        final String compareUrl = "https://api.github.com/repos/" + REPOSITORY + "/compare/" + currentCommit + "..." + releaseTag;
        final HttpURLConnection connection = openJsonConnection(compareUrl);
        try (final InputStream input = connection.getInputStream()) {
            final JsonObject compare = GSON.fromJson(new String(input.readAllBytes()), JsonObject.class);
            return switch (compare.get("status").getAsString()) {
                case "identical", "behind" -> compare.has("behind_by") ? compare.get("behind_by").getAsInt() : 0;
                case "ahead" -> 0;
                default -> -1;
            };
        } catch (final JsonSyntaxException ex) {
            throw new IOException("Invalid GitHub compare response", ex);
        } finally {
            connection.disconnect();
        }
    }

    private static void downloadReleaseAsset(final ReleaseAsset release, final Path target) throws IOException {
        Files.createDirectories(target.getParent());
        final Path temp = target.resolveSibling(target.getFileName().toString() + ".part");

        final HttpURLConnection connection = (HttpURLConnection) URI.create(release.downloadUrl()).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", userAgent());
        connection.setRequestProperty("Accept", "application/octet-stream");

        try (final InputStream input = connection.getInputStream(); final OutputStream output = Files.newOutputStream(temp)) {
            input.transferTo(output);
        } finally {
            connection.disconnect();
        }

        if (release.size() > 0 && Files.size(temp) != release.size()) {
            Files.deleteIfExists(temp);
            throw new IOException("Downloaded size mismatch for " + release.tagName());
        }

        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static HttpURLConnection openJsonConnection(final String url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(CONNECT_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", userAgent());
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        return connection;
    }

    private static String userAgent() {
        final ServerBuildInfo buildInfo = ServerBuildInfo.buildInfo();
        final OptionalInt buildNumber = buildInfo.buildNumber();
        final String version = buildInfo.minecraftVersionId() + "-" + (buildNumber.isPresent() ? buildNumber.getAsInt() : "DEV");
        return buildInfo.brandName() + "/" + version + " (Papyrus auto-update)";
    }

    public record ReleaseAsset(String tagName, String minecraftVersion, String downloadUrl, long size) {}
}
