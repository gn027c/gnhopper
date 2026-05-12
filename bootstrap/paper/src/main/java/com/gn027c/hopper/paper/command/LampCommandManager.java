package com.gn027c.hopper.paper.command;

import com.gn027c.hopper.paper.gnhopper;
import revxrsal.commands.bukkit.BukkitCommandHandler;

public class LampCommandManager {

    private final gnhopper plugin;
    private BukkitCommandHandler handler;

    public LampCommandManager(gnhopper plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        // Initialize Lamp handler
        this.handler = BukkitCommandHandler.create(plugin);

        // Suggestions for tiers
        handler.getAutoCompleter().registerSuggestion("tiers", (args, sender, command) -> {
            if (plugin.getMainConfig().getConfig().getConfigurationSection("tiers.list") == null) {
                return java.util.Collections.emptyList();
            }
            return new java.util.ArrayList<>(plugin.getMainConfig().getConfig().getConfigurationSection("tiers.list").getKeys(false));
        });

        // Register command classes
        handler.register(new HopperCommand(plugin));
    }

    public BukkitCommandHandler getHandler() {
        return handler;
    }
}
