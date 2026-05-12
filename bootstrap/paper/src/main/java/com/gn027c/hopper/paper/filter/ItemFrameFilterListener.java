package com.gn027c.hopper.paper.filter;

import com.gn027c.hopper.paper.gnhopper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemFrameFilterListener implements Listener {
    private final gnhopper plugin;
    private final FilterModule module;

    public ItemFrameFilterListener(gnhopper plugin, FilterModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    private boolean isItemFrameMode() {
        return plugin.getMainConfig().getString("settings.filter-mode", "ITEM_FRAME").equalsIgnoreCase("ITEM_FRAME");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!isItemFrameMode()) return;
        if (!(event.getRightClicked() instanceof ItemFrame)) return;
        
        ItemFrame frame = (ItemFrame) event.getRightClicked();
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshHopperFilter(frame.getLocation().getBlock().getRelative(frame.getAttachedFace()), event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!isItemFrameMode()) return;
        if (!(event.getEntity() instanceof ItemFrame)) return;
        
        ItemFrame frame = (ItemFrame) event.getEntity();
        org.bukkit.entity.Player player = event.getDamager() instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) event.getDamager() : null;
        
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshHopperFilter(frame.getLocation().getBlock().getRelative(frame.getAttachedFace()), player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(HangingBreakEvent event) {
        if (!isItemFrameMode()) return;
        if (!(event.getEntity() instanceof ItemFrame)) return;
        ItemFrame frame = (ItemFrame) event.getEntity();
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshHopperFilter(frame.getLocation().getBlock().getRelative(frame.getAttachedFace()), null));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(org.bukkit.event.hanging.HangingPlaceEvent event) {
        if (!isItemFrameMode()) return;
        if (!(event.getEntity() instanceof ItemFrame)) return;
        ItemFrame frame = (ItemFrame) event.getEntity();
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshHopperFilter(frame.getLocation().getBlock().getRelative(frame.getAttachedFace()), event.getPlayer()));
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        if (!isItemFrameMode()) return;
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemFrame) {
                refreshHopperFilter(entity.getLocation().getBlock().getRelative(((ItemFrame) entity).getAttachedFace()), null);
            }
        }
    }

    public void scanLoadedChunks() {
        if (!isItemFrameMode()) return;
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof ItemFrame) {
                        refreshHopperFilter(entity.getLocation().getBlock().getRelative(((ItemFrame) entity).getAttachedFace()), null);
                    }
                }
            }
        }
    }

    private void refreshHopperFilter(Block hopperBlock, org.bukkit.entity.Player player) {
        if (hopperBlock.getType() != Material.HOPPER) return;

        List<ItemStack> items = new ArrayList<>();
        // Scan all 6 faces of the hopper for ItemFrames
        for (BlockFace face : BlockFace.values()) {
            if (!face.isCartesian()) continue; // Only North, South, East, West, Up, Down
            
            Block faceBlock = hopperBlock.getRelative(face);
            for (Entity entity : faceBlock.getWorld().getNearbyEntities(faceBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
                if (entity instanceof ItemFrame) {
                    ItemFrame frame = (ItemFrame) entity;
                    // Check if this frame is actually attached to our hopper
                    if (hopperBlock.equals(frame.getLocation().getBlock().getRelative(frame.getAttachedFace()))) {
                        ItemStack frameItem = frame.getItem();
                        if (frameItem != null && frameItem.getType() != Material.AIR) {
                            items.add(frameItem.clone());
                        }
                    }
                }
            }
        }

        if (items.isEmpty()) {
            module.removeFilter(hopperBlock.getLocation());
            if (player != null) {
                plugin.getMessageService().sendActionBar(plugin.getAudience(player), 
                        "<red><b>[!]</b> Hopper filter removed (No frames found)");
            }
        } else {
            module.setFilter(hopperBlock.getLocation(), items);
            if (player != null) {
                plugin.getMessageService().sendActionBar(plugin.getAudience(player), 
                        "<green><b>[!]</b> Filter updated: <white>" + items.size() + " items active");
            }
        }
    }
}
