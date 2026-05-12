package com.gn027c.hopper.core.manager;

/**
 * Base module for the modular system.
 * Each main feature (hopper, filter, gui, economy, recipe)
 * is encapsulated into a separate module.
 */
public abstract class AbstractModule {
    private final String name;
    private boolean enabled = false;

    protected AbstractModule(String name) {
        this.name = name;
    }

    public void enable() {
        if (!enabled) {
            onEnable();
            enabled = true;
        }
    }

    public void disable() {
        if (enabled) {
            onDisable();
            enabled = false;
        }
    }

    protected abstract void onEnable();
    protected abstract void onDisable();

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }
}
