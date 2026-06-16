package io.papermc.paper.configuration;

import io.papermc.paper.anticheat.AnticheatSettings;
import io.papermc.paper.anticheat.PapyrusAnticheat;

/** Copies {@link GlobalConfiguration} anticheat YAML into {@link AnticheatSettings}. */
public final class AnticheatSettingsBridge {

    private AnticheatSettingsBridge() {
    }

    public static void apply(final GlobalConfiguration configuration) {
        if (configuration == null || configuration.anticheat == null) {
            return;
        }
        final GlobalConfiguration.Anticheat.Engine sourceEngine = configuration.anticheat.engine;
        final AnticheatSettings.Engine targetEngine = AnticheatSettings.ENGINE;
        targetEngine.enabled = sourceEngine.enabled;
        targetEngine.alerts.enabled = sourceEngine.alerts.enabled;
        targetEngine.alerts.console = sourceEngine.alerts.console;
        targetEngine.alerts.notifyOps = sourceEngine.alerts.notifyOps;
        targetEngine.punishments.kickEnabled = sourceEngine.punishments.kickEnabled;
        targetEngine.punishments.kickViolationLevel = sourceEngine.punishments.kickViolationLevel;
        targetEngine.punishments.kickMessage = sourceEngine.punishments.kickMessage;
        targetEngine.punishments.violationDecayPerSecond = sourceEngine.punishments.violationDecayPerSecond;

        final GlobalConfiguration.Anticheat.Engine.Checks sourceChecks = sourceEngine.checks;
        final AnticheatSettings.Checks targetChecks = targetEngine.checks;
        targetChecks.timer.enabled = sourceChecks.timer.enabled;
        targetChecks.timer.maxPacketsPerSecond = sourceChecks.timer.maxPacketsPerSecond;
        targetChecks.timer.violationWeight = sourceChecks.timer.violationWeight;
        targetChecks.reach.enabled = sourceChecks.reach.enabled;
        targetChecks.reach.blockExtraDistance = sourceChecks.reach.blockExtraDistance;
        targetChecks.reach.violationWeight = sourceChecks.reach.violationWeight;
        targetChecks.fastBreak.enabled = sourceChecks.fastBreak.enabled;
        targetChecks.fastBreak.maxPerSecond = sourceChecks.fastBreak.maxPerSecond;
        targetChecks.fastBreak.violationWeight = sourceChecks.fastBreak.violationWeight;
        targetChecks.fastPlace.enabled = sourceChecks.fastPlace.enabled;
        targetChecks.fastPlace.maxPerSecond = sourceChecks.fastPlace.maxPerSecond;
        targetChecks.fastPlace.violationWeight = sourceChecks.fastPlace.violationWeight;
        targetChecks.fastPlace.cancelAction = sourceChecks.fastPlace.cancelAction;
        targetChecks.inventory.enabled = sourceChecks.inventory.enabled;
        targetChecks.inventory.maxClicksPerSecond = sourceChecks.inventory.maxClicksPerSecond;
        targetChecks.inventory.violationWeight = sourceChecks.inventory.violationWeight;
        targetChecks.inventory.cancelAction = sourceChecks.inventory.cancelAction;
        targetChecks.handSwap.enabled = sourceChecks.handSwap.enabled;
        targetChecks.handSwap.maxSwapsPerSecond = sourceChecks.handSwap.maxSwapsPerSecond;
        targetChecks.handSwap.violationWeight = sourceChecks.handSwap.violationWeight;
        targetChecks.handSwap.cancelAction = sourceChecks.handSwap.cancelAction;
        targetChecks.movement.enabled = sourceChecks.movement.enabled;
        targetChecks.movement.maxHorizontalBlocksPerTick = sourceChecks.movement.maxHorizontalBlocksPerTick;
        targetChecks.movement.maxVerticalBlocksPerTick = sourceChecks.movement.maxVerticalBlocksPerTick;
        targetChecks.movement.leniencyMultiplier = sourceChecks.movement.leniencyMultiplier;
        targetChecks.movement.violationWeight = sourceChecks.movement.violationWeight;
        targetChecks.movement.setback = sourceChecks.movement.setback;
        targetChecks.xray.enabled = sourceChecks.xray.enabled;
        targetChecks.xray.windowSeconds = sourceChecks.xray.windowSeconds;
        targetChecks.xray.suspiciousOreCount = sourceChecks.xray.suspiciousOreCount;
        targetChecks.xray.minOresForRatioCheck = sourceChecks.xray.minOresForRatioCheck;
        targetChecks.xray.maxOreToStoneRatio = sourceChecks.xray.maxOreToStoneRatio;
        targetChecks.xray.violationWeight = sourceChecks.xray.violationWeight;
        if (!sourceChecks.xray.resolvedOres.isEmpty()) {
            PapyrusAnticheat.bindTrackedOres(sourceChecks.xray.resolvedOres);
        }

        final GlobalConfiguration.Anticheat.ClientIntegrity sourceIntegrity = configuration.anticheat.clientIntegrity;
        final AnticheatSettings.ClientIntegrity targetIntegrity = AnticheatSettings.CLIENT_INTEGRITY;
        targetIntegrity.enabled = sourceIntegrity.enabled;
        targetIntegrity.requirePapyrusClient = sourceIntegrity.requirePapyrusClient;
        targetIntegrity.requireShieldMod = sourceIntegrity.requireShieldMod;
        targetIntegrity.responseTimeoutSeconds = sourceIntegrity.responseTimeoutSeconds;
        targetIntegrity.downloadUrl = sourceIntegrity.downloadUrl;
        targetIntegrity.bannedModMessage = sourceIntegrity.bannedModMessage;
        targetIntegrity.alerts.console = sourceIntegrity.alerts.console;
        targetIntegrity.bannedModIds = java.util.List.copyOf(sourceIntegrity.bannedModIds);
    }
}
