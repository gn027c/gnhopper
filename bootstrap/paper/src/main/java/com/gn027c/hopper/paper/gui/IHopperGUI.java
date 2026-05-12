package com.gn027c.hopper.paper.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface IHopperGUI {
    void open(Player player);
    void handle(InventoryClickEvent event);
}
