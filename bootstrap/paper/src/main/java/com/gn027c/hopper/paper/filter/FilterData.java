package com.gn027c.hopper.paper.filter;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FilterData {
    private final Location location;
    private List<ItemStack> filterItems = new ArrayList<>();
    private String tierName;

    public FilterData(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public List<ItemStack> getFilterItems() {
        return filterItems;
    }

    public void setFilterItems(List<ItemStack> filterItems) {
        this.filterItems = filterItems;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }
}
