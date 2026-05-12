package com.gn027c.hopper.paper.hopper;

/**
 * Transfer modes for Filter Hoppers.
 * The plugin will automatically check and select the most suitable mode when set to AUTO.
 *
 * Priority order (from most powerful to safest):
 * TASK → HYBRID → EVENT
 */
public enum TransferMode {

    /**
     * Automatically detect the most suitable mode.
     * The plugin will try TASK → HYBRID → EVENT, stopping at the first mode that works correctly.
     */
    AUTO,

    /**
     * Plugin has full control (Pull + Push).
     * Vanilla is completely locked using Cooldown 127.
     * + Highest speed (customizable transfer-amount, transfer-tick)
     * - Highest risk of update() conflicts
     */
    TASK,

    /**
     * Event filters input (Vanilla pull), Task filters output (Plugin push).
     * + Balanced between speed and stability
     * - Pull speed limited by Vanilla
     */
    HYBRID,

    /**
     * Event-only. Vanilla handles all transfers, Event only filters.
     * + Absolute stability, no conflicts
     * - Vanilla speed (8 ticks/item, 1 item at a time)
     */
    EVENT;

    /**
     * Get the next mode in the fallback chain.
     * TASK → HYBRID → EVENT → null (end of chain)
     */
    public TransferMode nextFallback() {
        return switch (this) {
            case TASK -> HYBRID;
            case HYBRID -> EVENT;
            case EVENT -> null; // No more fallback
            case AUTO -> TASK;  // AUTO starts from TASK
        };
    }

    /**
     * Parse from config string, defaults to AUTO if not recognized.
     */
    public static TransferMode fromString(String value) {
        if (value == null) return AUTO;
        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
