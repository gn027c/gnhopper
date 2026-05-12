package com.gn027c.hopper.paper.hopper;

import com.gn027c.hopper.paper.gnhopper;
import com.gn027c.hopper.paper.filter.FilterModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.logging.Logger;

/**
 * System for automatic detection and switching of Transfer Mode.
 * 
 * When mode = AUTO, ModeDetector will:
 * 1. Start with TASK mode (most powerful)
 * 2. Wait for 200 ticks (10 seconds) to check activity
 * 3. If errors detected (items stuck or bypass) → switch to next mode
 * 4. Repeat until a working mode is found, or report error
 * 
 * After determining the mode, ModeDetector continues to monitor system health
 * every 30 seconds to detect runtime issues.
 */
public class ModeDetector {
    private final gnhopper plugin;
    private final HopperModule module;
    private final Logger logger;

    private TransferMode activeMode;
    private boolean autoMode;
    private boolean detectionComplete = false;

    // Metrics for diagnosis
    private int diagnosticTicks = 0;
    private int transferSuccessCount = 0;
    private int transferAttemptCount = 0;
    private int bypassDetectedCount = 0;
    private int stuckDetectedCount = 0;

    // Diagnostic configuration
    private static final int DIAGNOSTIC_DURATION_TICKS = 200; // 10 seconds
    private static final int HEALTH_CHECK_INTERVAL_TICKS = 600; // 30 seconds
    private int healthCheckCounter = 0;

    public ModeDetector(gnhopper plugin, HopperModule module) {
        this.plugin = plugin;
        this.module = module;
        this.logger = plugin.getLogger();

        // Read mode from config
        String configMode = plugin.getMainConfig().getString("settings.transfer-mode", "AUTO");
        TransferMode requested = TransferMode.fromString(configMode);

        if (requested == TransferMode.AUTO) {
            this.autoMode = true;
            this.activeMode = TransferMode.TASK; // Start with most powerful mode
            logger.info("[GNH-Mode] AUTO mode - Starting diagnosis with TASK mode...");
        } else {
            this.autoMode = false;
            this.activeMode = requested;
            this.detectionComplete = true;
            logger.info("[GNH-Mode] Using fixed mode: " + requested.name());
        }
    }

    /**
     * Called every tick from HopperModule.
     * Manages diagnostic cycles and health checks.
     */
    public void tick() {
        if (autoMode && !detectionComplete) {
            runDiagnostic();
        } else if (detectionComplete) {
            runHealthCheck();
        }
    }

    /**
     * Initial diagnostic cycle (AUTO mode).
     * Runs for the first 200 ticks for each test mode.
     */
    private void runDiagnostic() {
        diagnosticTicks++;

        // Wait for diagnostic duration
        if (diagnosticTicks < DIAGNOSTIC_DURATION_TICKS) {
            return;
        }

        // Analyze results
        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        Map<String, HopperState> hoppers = module.getActiveHoppers();

        // If no Filter Hoppers found → cannot diagnose, keep current mode
        if (filterModule == null || hoppers.isEmpty()) {
            logger.info("[GNH-Mode] No Filter Hoppers found for testing. Keeping mode: " + activeMode.name());
            detectionComplete = true;
            return;
        }

        // Check if any Filter Hopper is actually active
        boolean hasFilterHopper = false;
        for (Map.Entry<String, HopperState> entry : hoppers.entrySet()) {
            Location loc = parseKey(entry.getKey());
            if (loc != null && filterModule.getFilter(loc) != null) {
                hasFilterHopper = true;
                break;
            }
        }

        if (!hasFilterHopper) {
            logger.info("[GNH-Mode] No active Filter Hoppers found. Keeping mode: " + activeMode.name());
            detectionComplete = true;
            return;
        }

        // Evaluate current mode
        boolean modeWorks = evaluateCurrentMode();

        if (modeWorks) {
            detectionComplete = true;
            logger.info("===========================================");
            logger.info("[GNH-Mode] ✓ Mode determined successfully: " + activeMode.name());
            logger.info("[GNH-Mode] Transfers: " + transferSuccessCount + "/" + transferAttemptCount);
            logger.info("[GNH-Mode] Bypass: " + bypassDetectedCount + " | Stuck: " + stuckDetectedCount);
            logger.info("===========================================");
        } else {
            // Try next mode
            TransferMode next = activeMode.nextFallback();
            if (next != null) {
                logger.warning("[GNH-Mode] ✗ Mode " + activeMode.name() + " is not working correctly.");
                logger.warning("[GNH-Mode] → Switching to " + next.name() + " for testing...");
                switchMode(next);
            } else {
                // No more modes to try
                detectionComplete = true;
                logger.severe("===========================================");
                logger.severe("[GNH-Mode] ✗ NO WORKING MODE FOUND!");
                logger.severe("[GNH-Mode] Filter Hoppers might be incompatible with this server.");
                logger.severe("[GNH-Mode] Please check:");
                logger.severe("[GNH-Mode]   1. Paper/Spigot version");
                logger.severe("[GNH-Mode]   2. Conflicting plugins (HopperOptimizer, WildStacker, etc.)");
                logger.severe("[GNH-Mode]   3. Set 'transfer-mode: EVENT' in config.yml for safest mode");
                logger.severe("===========================================");
                // Fallback to safest mode
                activeMode = TransferMode.EVENT;
            }
        }
    }

    /**
     * Evaluate if the current mode is working correctly.
     */
    private boolean evaluateCurrentMode() {
        // If bypass detected → mode is incorrect
        if (bypassDetectedCount > 0) {
            logger.warning("[GNH-Mode] Detected " + bypassDetectedCount + " bypass(es) in " + activeMode.name() + " mode.");
            return false;
        }

        // If attempts exist but zero success → stuck items
        if (transferAttemptCount > 5 && transferSuccessCount == 0) {
            logger.warning("[GNH-Mode] Detected stuck items in " + activeMode.name() + " mode" + 
                " (" + transferAttemptCount + " attempts, 0 success)");
            return false;
        }

        // If too many stuck detections → unstable
        if (stuckDetectedCount > 3) {
            logger.warning("[GNH-Mode] Detected " + stuckDetectedCount + " stuck instances in " + activeMode.name() + " mode.");
            return false;
        }

        return true;
    }

    /**
     * Switch to a new mode and reset metrics.
     */
    private void switchMode(TransferMode newMode) {
        TransferMode oldMode = this.activeMode;
        this.activeMode = newMode;
        resetMetrics();

        // IMPORTANT: Reset cooldown for all Filter Hoppers when switching FROM TASK mode
        // TASK mode sets cooldown 127 + update() → cooldown gets "stuck" permanently
        if (oldMode == TransferMode.TASK) {
            resetAllHopperCooldowns();
        }
    }

    /**
     * Reset cooldown of all Filter Hoppers to 0 to allow Vanilla functionality.
     */
    private void resetAllHopperCooldowns() {
        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        if (filterModule == null) return;

        int resetCount = 0;
        for (Map.Entry<String, HopperState> entry : module.getActiveHoppers().entrySet()) {
            Location loc = parseKey(entry.getKey());
            if (loc == null) continue;
            if (filterModule.getFilter(loc) == null) continue;

            org.bukkit.block.Block block = loc.getBlock();
            if (block.getType() != org.bukkit.Material.HOPPER) continue;

            org.bukkit.block.BlockState bState = block.getState();
            if (bState instanceof Hopper) {
                Hopper hopper = (Hopper) bState;
                hopper.setTransferCooldown(1); // Reset to 1 (will become 0 in next tick)
                hopper.update(true, false);
                resetCount++;
            }
        }
        logger.info("[GNH-Mode] Reset cooldown for " + resetCount + " Filter Hoppers.");
    }

    /**
     * Periodically check system health (after mode selection).
     */
    private void runHealthCheck() {
        if (!autoMode) return; // Only health-check if in AUTO mode

        healthCheckCounter++;
        if (healthCheckCounter < HEALTH_CHECK_INTERVAL_TICKS) return;
        healthCheckCounter = 0;

        // If too many bypasses or stuck items → try downgrade
        if (bypassDetectedCount > 5 || stuckDetectedCount > 10) {
            TransferMode next = activeMode.nextFallback();
            if (next != null) {
                logger.warning("[GNH-Mode] Health check FAILED! Bypass: " + bypassDetectedCount + 
                    ", Stuck: " + stuckDetectedCount);
                logger.warning("[GNH-Mode] Automatically switching from " + activeMode.name() + " → " + next.name());
                switchMode(next);
            }
        }

        // Reset metrics for next cycle
        resetMetrics();
    }

    /**
     * Reset all diagnostic metrics.
     */
    private void resetMetrics() {
        diagnosticTicks = 0;
        transferSuccessCount = 0;
        transferAttemptCount = 0;
        bypassDetectedCount = 0;
        stuckDetectedCount = 0;
    }

    // === Metrics Reporting API ===

    /** Call when a transfer succeeds (Task or Event). */
    public void reportTransferSuccess() {
        transferSuccessCount++;
    }

    /** Call on any transfer attempt. */
    public void reportTransferAttempt() {
        transferAttemptCount++;
    }

    /** Call when an item bypasses the filter. */
    public void reportBypass() {
        bypassDetectedCount++;
    }

    /** Call when an item is detected as stuck in a hopper. */
    public void reportStuck() {
        stuckDetectedCount++;
    }

    // === Getters ===

    public TransferMode getActiveMode() {
        return activeMode;
    }

    public boolean isDetectionComplete() {
        return detectionComplete;
    }

    public String getStatusString() {
        String status = activeMode.name();
        if (autoMode && !detectionComplete) {
            status += " (testing...)";
        } else if (autoMode) {
            status += " (auto-detected)";
        }
        return status;
    }

    private Location parseKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length < 4) return null;
            org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
            if (world == null) return null;
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }
}
