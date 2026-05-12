package com.gn027c.hopper.paper.gui;

import com.gn027c.hopper.paper.gnhopper;
import com.gn027c.hopper.paper.filter.FilterModule;
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

public class FilterGUI implements IHopperGUI {
    private final gnhopper plugin;
    private final HopperData data;
    private final HopperState state;
    private Inventory inventory;
    private final int maxSlots;

    public FilterGUI(gnhopper plugin, HopperData data, HopperState state) {
        this.plugin = plugin;
        this.data = data;
        this.state = state;
        
        // Get number of slots from config (prioritize settings.default-filter-slots)
        int defaultSlots = plugin.getMainConfig().getInt("settings.default-filter-slots", 1);
        String tier = data.getTierName();
        this.maxSlots = plugin.getMainConfig().getBoolean("tiers.enabled", false) 
                ? plugin.getMainConfig().getInt("tiers.list." + tier + ".filter-slots", defaultSlots)
                : defaultSlots;
    }

    public void open(Player player) {
        String title = "§8Filter Settings (" + data.getTierName() + ")";
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

        // Filter Slots (Center area: 10, 11, 12, 13, 14, 15, 16)
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        
        List<ItemStack> currentFilters = data.getFilterItems();
        
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            if (i < maxSlots) {
                // Allowed slots
                if (i < currentFilters.size()) {
                    inventory.setItem(slot, currentFilters.get(i).clone());
                } else {
                    inventory.setItem(slot, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Empty Slot", "§eClick with an item to set filter"));
                }
            } else {
                // Locked slots
                inventory.setItem(slot, createItem(Material.BARRIER, "§cLocked Slot", "§7Upgrade tier to unlock"));
            }
        }

        // Back button
        inventory.setItem(26, createItem(Material.ARROW, "§7Go Back", ""));
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
        int slot = event.getRawSlot();
        if (slot >= 27) return; // Ignore clicks in player inventory
        
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (slot == 26) {
            // Return to dashboard
            GUIModule guiModule = plugin.getModuleManager().getModule(GUIModule.class).orElse(null);
            if (guiModule != null) {
                guiModule.openGUI(player, new HopperGUI(plugin, data, state));
            }
            return;
        }

        // Handle clicks on filter slots
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int filterIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                filterIndex = i;
                break;
            }
        }

        if (filterIndex != -1) {
            if (filterIndex >= maxSlots) {
                player.sendMessage("§cYou need to upgrade your hopper to use this slot!");
                return;
            }

            ItemStack cursor = event.getCursor();
            List<ItemStack> filters = new ArrayList<>(data.getFilterItems());

            if (cursor != null && cursor.getType() != Material.AIR) {
                // Set new filter (Ghost item)
                ItemStack filterItem = cursor.clone();
                filterItem.setAmount(1);
                
                if (filterIndex < filters.size()) {
                    filters.set(filterIndex, filterItem);
                } else {
                    filters.add(filterItem);
                }
                player.sendMessage("§bSet filter: §f" + filterItem.getType().name());
            } else {
                // Remove filter
                if (filterIndex < filters.size()) {
                    filters.remove(filterIndex);
                    player.sendMessage("§7Filter removed.");
                }
            }

            // Save and update
            data.setFilterItems(filters);
            saveToModule();
            setupItems(); // Refresh GUI
        }
    }
    
    private void saveToModule() {
        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        if (filterModule != null) {
            filterModule.setFilter(data.getLocation(), data.getFilterItems());
        }
    }
}
