package com.gn027c.hopper.paper.hopper;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent data of a hopper (saved to file/PDC).
 */
public class HopperData {
    private final Location location;
    private String tierName = "basic";
    private List<ItemStack> filterItems = new ArrayList<>();

    public HopperData(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public List<ItemStack> getFilterItems() {
        return filterItems;
    }

    public void setFilterItems(List<ItemStack> filterItems) {
        this.filterItems = filterItems;
    }
}
