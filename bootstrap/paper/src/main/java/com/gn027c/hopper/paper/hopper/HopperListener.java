package com.gn027c.hopper.paper.hopper;

import com.gn027c.hopper.paper.gnhopper;
import com.gn027c.hopper.paper.filter.FilterModule;
import com.gn027c.hopper.paper.gui.GUIModule;
import com.gn027c.hopper.paper.gui.HopperGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener handling hopper-related events.
 * Behavior changes based on the TransferMode:
 * 
 * - TASK mode:   Cancel 100% events for Filter Hoppers (Task handles all)
 * - HYBRID mode: Cancel output events (Task handles push), filter input events (Vanilla handles pull)
 * - EVENT mode:  Filter both input and output events (Vanilla handles transfer, Event only filters)
 */
public class HopperListener implements Listener {
    private final gnhopper plugin;
    private final HopperModule module;
    private int lockdownInterceptions = 0;
    private int totalMoveEvents = 0;

    public HopperListener(gnhopper plugin, HopperModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    // ========== ITEM MOVE EVENTS ==========

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (plugin.isInternalCall()) return; // Passport: Allow internal calls to bypass
        totalMoveEvents++;
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();

        boolean sourceIsHopper = source.getType() == InventoryType.HOPPER;
        boolean destinationIsHopper = destination.getType() == InventoryType.HOPPER;

        if (!sourceIsHopper && !destinationIsHopper) return;

        Location sourceLoc = normalize(source);
        Location destLoc = normalize(destination);

        String sourceKey = toKey(sourceLoc);
        String destKey = toKey(destLoc);

        boolean sourceTracked = sourceIsHopper && sourceLoc != null && module.getActiveHoppers().containsKey(sourceKey);
        boolean destTracked = destinationIsHopper && destLoc != null && module.getActiveHoppers().containsKey(destKey);

        if (!sourceTracked && !destTracked) return;

        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        TransferMode mode = getCurrentMode();

        // ---- Check SOURCE (Source = Filter Hopper pushing out) ----
        if (sourceTracked && filterModule != null && filterModule.getFilter(sourceLoc) != null) {
            switch (mode) {
                case TASK:
                    // TASK mode: Cancel 100% — Task handles push
                    event.setCancelled(true);
                    lockdownInterceptions++;
                    return;

                case HYBRID:
                    // HYBRID mode: Cancel 100% — Task handles push
                    event.setCancelled(true);
                    lockdownInterceptions++;
                    return;

                case EVENT:
                    // EVENT mode: Check output filter → cancel if not allowed
                    if (plugin.getMainConfig().getBoolean("settings.filter-actions.block-move-out", true)) {
                        if (!filterModule.isAllowed(sourceLoc, event.getItem())) {
                            event.setCancelled(true);
                            lockdownInterceptions++;
                            return;
                        }
                    }
                    // Item matches filter → let Vanilla push normally
                    break;

                default:
                    break;
            }
        }

        // ---- Check DESTINATION (Destination = Filter Hopper pulling in) ----
        if (destTracked && filterModule != null && filterModule.getFilter(destLoc) != null) {
            switch (mode) {
                case TASK:
                    // TASK mode: Cancel 100% — Task handles pull
                    event.setCancelled(true);
                    lockdownInterceptions++;
                    return;

                case HYBRID:
                case EVENT:
                    // HYBRID/EVENT mode: Check input filter → block disallowed items
                    if (!filterModule.isAllowed(destLoc, event.getItem())) {
                        event.setCancelled(true);
                        lockdownInterceptions++;
                        return;
                    }
                    // Item matches filter → let Vanilla pull normally
                    break;

                default:
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItemMonitor(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();

        boolean sourceIsHopper = source.getType() == InventoryType.HOPPER;
        boolean destinationIsHopper = destination.getType() == InventoryType.HOPPER;

        if (!sourceIsHopper && !destinationIsHopper) return;

        Location sourceLoc = normalize(source);
        Location destLoc = normalize(destination);

        String sourceKey = toKey(sourceLoc);
        String destKey = toKey(destLoc);

        boolean sourceTracked = sourceIsHopper && sourceLoc != null && module.getActiveHoppers().containsKey(sourceKey);
        boolean destTracked = destinationIsHopper && destLoc != null && module.getActiveHoppers().containsKey(destKey);

        if (!sourceTracked && !destTracked) return;

        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        TransferMode mode = getCurrentMode();

        // [Deep Debug]
        if (module.isDeepDebug()) {
            plugin.getLogger().info("[DeepDebug] MoveEvent(Monitor) | Source: " + source.getType() + " (Tracked:" + sourceTracked + ") -> Dest: " + destination.getType() + " (Tracked:" + destTracked + ")");
            plugin.getLogger().info("   > Item: " + event.getItem().getType() + " x" + event.getItem().getAmount());
            plugin.getLogger().info("   > Cancelled by other plugins: " + event.isCancelled());
        }

        // Log PUSH if Vanilla handles it (EVENT mode only)
        if (sourceTracked && filterModule != null && filterModule.getFilter(sourceLoc) != null) {
            if (mode == TransferMode.EVENT) {
                module.logTransfer(sourceLoc, event.getItem(), event.getItem().getAmount(), "PUSH (Vanilla)");
                ModeDetector detector = module.getModeDetector();
                if (detector != null) detector.reportTransferSuccess();
            }
        }

        // Log PULL if Vanilla handles it (EVENT and HYBRID modes)
        if (destTracked && filterModule != null && filterModule.getFilter(destLoc) != null) {
            if (mode == TransferMode.EVENT || mode == TransferMode.HYBRID) {
                module.logTransfer(destLoc, event.getItem(), event.getItem().getAmount(), "PULL (Vanilla)");
                ModeDetector detector = module.getModeDetector();
                if (detector != null) detector.reportTransferSuccess();
            }
        }
    }

    // ========== ITEM PICKUP EVENTS ==========

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        Inventory destination = event.getInventory();
        if (destination.getType() != InventoryType.HOPPER) return;

        Location loc = normalize(destination);
        if (loc == null) return;

        String key = toKey(loc);
        if (!module.getActiveHoppers().containsKey(key)) return;

        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        if (filterModule == null || filterModule.getFilter(loc) == null) return;

        TransferMode mode = getCurrentMode();

        if (mode == TransferMode.TASK) {
            // TASK mode: Cancel pickup — Task handles it
            event.setCancelled(true);
            return;
        }

        // HYBRID/EVENT mode: Check filter
        if (!filterModule.isAllowed(loc, event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // ========== PLAYER INTERACTION EVENTS ==========

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.HOPPER) return;
        if (!event.getPlayer().isSneaking()) return;

        if (event.getItem() != null && event.getItem().getType() != Material.AIR) return;

        String filterMode = plugin.getMainConfig().getString("settings.filter-mode", "ITEM_FRAME");
        if (filterMode.equalsIgnoreCase("ITEM_FRAME")) return;

        event.setCancelled(true);

        Location loc = event.getClickedBlock().getLocation();
        HopperState state = module.getActiveHoppers().get(toKey(loc));
        if (state == null) {
            module.trackHopper(loc);
            state = module.getActiveHoppers().get(toKey(loc));
        }

        GUIModule guiModule = plugin.getModuleManager().getModule(GUIModule.class).orElse(null);
        if (guiModule != null) {
            HopperData data = new HopperData(loc);
            FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
            if (filterModule != null) {
                com.gn027c.hopper.paper.filter.FilterData filterData = filterModule.getFilter(loc);
                if (filterData != null) {
                    data.setFilterItems(new java.util.ArrayList<>(filterData.getFilterItems()));
                }
            }
            guiModule.openGUI(event.getPlayer(), new HopperGUI(plugin, data, state));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.HOPPER) {
            if (plugin.getMainConfig().getBoolean("settings.filter-actions.block-player-interact", false)) {
                ItemStack itemToCheck = null;
                if (event.getRawSlot() < event.getInventory().getSize()) {
                    itemToCheck = event.getCursor();
                } else if (event.getClick().isShiftClick()) {
                    itemToCheck = event.getCurrentItem();
                }

                if (itemToCheck != null && itemToCheck.getType() != Material.AIR) {
                    FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
                    if (filterModule != null && !filterModule.isAllowed(event.getInventory().getLocation(), itemToCheck)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            wakeUp(event.getInventory().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() == InventoryType.HOPPER) {
            if (plugin.getMainConfig().getBoolean("settings.filter-actions.block-player-interact", false)) {
                FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
                if (filterModule == null) return;

                for (int slot : event.getRawSlots()) {
                    if (slot < event.getInventory().getSize()) {
                        if (!filterModule.isAllowed(event.getInventory().getLocation(), event.getOldCursor())) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
            wakeUp(event.getInventory().getLocation());
        }
    }

    // ========== BLOCK PLACE/BREAK EVENTS ==========

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType().name().contains("HOPPER")) {
            module.trackHopper(event.getBlockPlaced().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().name().contains("HOPPER")) {
            module.untrackHopper(event.getBlock().getLocation());
        }
    }

    // ========== UTILITY ==========

    private TransferMode getCurrentMode() {
        ModeDetector detector = module.getModeDetector();
        if (detector != null) {
            return detector.getActiveMode();
        }
        return TransferMode.EVENT; // Safe fallback
    }

    private void wakeUp(Location loc) {
        if (loc == null) return;
        HopperState state = module.getActiveHoppers().get(toKey(loc));
        if (state != null && state.isHibernating()) {
            state.setHibernating(false);
        }
    }

    private Location normalize(Inventory inv) {
        Location loc = inv.getLocation();
        if (loc == null && inv.getHolder() instanceof org.bukkit.inventory.BlockInventoryHolder) {
            loc = ((org.bukkit.inventory.BlockInventoryHolder) inv.getHolder()).getBlock().getLocation();
        }
        if (loc == null && inv.getHolder() instanceof org.bukkit.block.BlockState) {
            loc = ((org.bukkit.block.BlockState) inv.getHolder()).getLocation();
        }
        if (loc == null) return null;
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public int getLockdownInterceptions() { return lockdownInterceptions; }
    public void resetLockdownInterceptions() {
        lockdownInterceptions = 0;
        totalMoveEvents = 0;
    }
    public int getTotalMoveEvents() { return totalMoveEvents; }

    private boolean isTesting(Inventory inv) {
        Location loc = normalize(inv);
        return loc != null && module.getTestingHoppers().contains(toKey(loc));
    }

    private String toKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown";
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
}
