package com.gn027c.hopper.paper.gui;

import com.gn027c.hopper.paper.gnhopper;
import com.gn027c.hopper.paper.hopper.HopperData;
import com.gn027c.hopper.paper.hopper.HopperState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HopperGUI implements IHopperGUI {
    private final gnhopper plugin;
    private final HopperData data;
    private final HopperState state;
    private Inventory inventory;

    public HopperGUI(gnhopper plugin, HopperData data, HopperState state) {
        this.plugin = plugin;
        this.data = data;
        this.state = state;
    }

    public void open(Player player) {
        String title = "§8Hopper Manager";
        this.inventory = Bukkit.createInventory(null, 27, title);
        
        setupItems();
        player.openInventory(inventory);
    }

    private void setupItems() {
        // Decor
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        // Info Item
        inventory.setItem(13, createItem(Material.HOPPER, "§6§lHopper Info", 
            "§7Tier: §f" + data.getTierName(),
            "§7Speed: §f" + plugin.getMainConfig().getTransferTick() + " ticks",
            "§7Amount: §f" + plugin.getMainConfig().getTransferAmount(),
            "",
            "§eClick to upgrade!"));

        // Filter Item
        inventory.setItem(11, createItem(Material.HOPPER_MINECART, "§b§lFilter Settings", 
            "§7Mode: §f" + plugin.getMainConfig().getString("settings.filter-mode"),
            "",
            "§eClick to manage filters"));

        // Close Item
        inventory.setItem(26, createItem(Material.BARRIER, "§cClose", ""));
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> list = new ArrayList<>();
        for (String s : lore) list.add(s);
        meta.setLore(list);
        item.setItemMeta(meta);
        return item;
    }

    public void handle(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 11) {
            // Open Filter GUI
            GUIModule guiModule = plugin.getModuleManager().getModule(GUIModule.class).orElse(null);
            if (guiModule != null) {
                guiModule.openGUI((Player) event.getWhoClicked(), new FilterGUI(plugin, data, state));
            }
        } else if (slot == 13) {
            // Open Upgrade GUI (To be implemented)
            event.getWhoClicked().sendMessage("§eUpgrade GUI is coming soon!");
        } else if (slot == 26) {
            event.getWhoClicked().closeInventory();
        }
    }
}
