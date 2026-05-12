package com.gn027c.hopper.paper.gui;

import com.gn027c.hopper.core.manager.AbstractModule;
import com.gn027c.hopper.paper.gnhopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIModule extends AbstractModule implements Listener {
    private final gnhopper plugin;
    private final Map<UUID, IHopperGUI> activeGUIs = new HashMap<>();

    public GUIModule(gnhopper plugin) {
        super("GUI");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
        activeGUIs.clear();
    }

    public void openGUI(org.bukkit.entity.Player player, IHopperGUI gui) {
        activeGUIs.put(player.getUniqueId(), gui);
        gui.open(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        IHopperGUI gui = activeGUIs.get(event.getWhoClicked().getUniqueId());
        if (gui != null) {
            gui.handle(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        activeGUIs.remove(event.getPlayer().getUniqueId());
    }
}
