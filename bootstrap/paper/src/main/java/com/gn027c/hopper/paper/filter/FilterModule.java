package com.gn027c.hopper.paper.filter;

import com.gn027c.hopper.core.manager.AbstractModule;
import com.gn027c.hopper.paper.gnhopper;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.gn027c.hopper.paper.hopper.HopperModule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterModule extends AbstractModule {
    private final gnhopper plugin;
    private final Map<String, FilterData> hopperFilters = new HashMap<>();

    public FilterModule(gnhopper plugin) {
        super("Filter");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        String mode = plugin.getMainConfig().getString("settings.filter-mode", "ITEM_FRAME");
        plugin.getLogger().info("Activating FilterModule with mode: " + mode);
        
        ItemFrameFilterListener listener = new ItemFrameFilterListener(plugin, this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        
        // Scan all ItemFrames in loaded chunks (e.g., Spawn chunks)
        listener.scanLoadedChunks();
    }

    @Override
    public void onDisable() {
        hopperFilters.clear();
    }

    public void setFilter(Location loc, List<ItemStack> items) {
        String key = toKey(loc);
        FilterData data = hopperFilters.getOrDefault(key, new FilterData(loc));
        data.setFilterItems(items);
        hopperFilters.put(key, data);
        
        plugin.getLogger().info("Filter set at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + 
            " with " + (items.isEmpty() ? "EMPTY" : items.get(0).getType()));

        // Ensure Hopper is tracked to start transfer processing
        HopperModule hopperModule = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        if (hopperModule != null) {
            hopperModule.trackHopper(loc);
        }
    }

    public void addFilter(Location loc, FilterData data) {
        String key = toKey(loc);
        hopperFilters.put(key, data);
        
        // Automatically notify HopperModule to track
        HopperModule hopperModule = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        if (hopperModule != null) {
            hopperModule.trackHopper(loc);
        }
    }

    public boolean isAllowed(Location loc, ItemStack item) {
        if (item == null) return false;
        FilterData data = getFilter(loc);
        
        // DIAGNOSTIC LOG
        boolean testing = loc.getBlock().hasMetadata("GNH_TESTING");
        if (testing) {
            plugin.getLogger().warning("[GNH-Filter] Testing: " + item.getType() + " at " + toKey(loc));
            if (data == null) {
                plugin.getLogger().warning("[GNH-Filter] -> Result: ALLOW (No filter data)");
            } else if (data.getFilterItems().isEmpty()) {
                plugin.getLogger().warning("[GNH-Filter] -> Result: ALLOW (Empty filter)");
            }
        }

        if (data == null || data.getFilterItems().isEmpty()) {
            return true;
        }

        for (ItemStack filter : data.getFilterItems()) {
            if (filter == null) continue;
            boolean match = filter.isSimilar(item);
            if (testing) {
                plugin.getLogger().warning("[GNH-Filter] -> Comparing with " + filter.getType() + ": " + (match ? "MATCH" : "NO MATCH"));
            }
            if (match) {
                return true;
            }
        }
        
        if (testing) plugin.getLogger().warning("[GNH-Filter] -> Final Result: BLOCK");
        return false;
    }

    public void removeFilter(Location loc) {
        String key = toKey(loc);
        hopperFilters.remove(key);
        plugin.getLogger().info("Filter removed at " + key);
    }

    public java.util.List<String> debugFilter(Location loc, ItemStack item) {
        java.util.List<String> logs = new java.util.ArrayList<>();
        String key = toKey(loc);
        logs.add("§d[Filter Probe] §fChecking item: §e" + item.getType());
        
        FilterData data = hopperFilters.get(key);
        if (data == null) {
            logs.add("§8- §c[!] Result: §fNo filter data found at this location.");
            return logs;
        }
        
        List<ItemStack> filters = data.getFilterItems();
        if (filters.isEmpty()) {
            logs.add("§8- §c[!] Result: §fFilter list is empty (Defaults to ALLOW ALL).");
            return logs;
        }
        
        logs.add("§8- §fNumber of filter samples: §b" + filters.size());
        for (int i = 0; i < filters.size(); i++) {
            ItemStack f = filters.get(i);
            if (f == null) continue;
            
            boolean similar = f.isSimilar(item);
            logs.add("§8- §fSample " + (i+1) + " (§b" + f.getType() + "§f): " + (similar ? "§aMATCH" : "§cNO MATCH"));
            
            if (!similar && f.getType() == item.getType()) {
                logs.add("§8  > §eError analysis: §fSame material but different Metadata (Name, Lore, or NBT).");
            }
        }
        return logs;
    }

    public FilterData getFilter(Location loc) {
        return hopperFilters.get(toKey(loc));
    }

    public String toKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown";
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
}
