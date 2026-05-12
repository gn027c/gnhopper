package com.gn027c.hopper.core.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages the lifecycle of modules.
 */
public class ModuleManager {
    private final List<AbstractModule> modules = new ArrayList<>();

    public void registerModule(AbstractModule module) {
        modules.add(module);
    }

    public void enableModules() {
        modules.forEach(AbstractModule::enable);
    }

    public void disableModules() {
        // Disable in reverse order
        for (int i = modules.size() - 1; i >= 0; i--) {
            modules.get(i).disable();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractModule> Optional<T> getModule(Class<T> clazz) {
        return modules.stream()
                .filter(clazz::isInstance)
                .map(m -> (T) m)
                .findFirst();
    }

    public List<AbstractModule> getModules() {
        return modules;
    }
}
