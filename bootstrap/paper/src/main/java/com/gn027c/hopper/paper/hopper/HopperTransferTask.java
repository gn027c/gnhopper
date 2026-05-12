package com.gn027c.hopper.paper.hopper;

import com.gn027c.hopper.paper.gnhopper;
import com.gn027c.hopper.paper.filter.FilterModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Task running every tick, handling item transfer for Filter Hoppers.
 * Behavior changes based on the current TransferMode:
 * 
 * - TASK mode: Plugin controls both Pull + Push
 * - HYBRID mode: Plugin only controls Push
 * - EVENT mode: Task does nothing (Event handles all)
 */
public class HopperTransferTask extends BukkitRunnable {
    private final gnhopper plugin;
    private final HopperModule module;
    private final FilterModule filterModule;
    private long lastExecutionTimeMs = 0;

    public HopperTransferTask(gnhopper plugin, HopperModule module) {
        this.plugin = plugin;
        this.module = module;
        this.filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();

        // Tick ModeDetector (diagnosis & health check)
        ModeDetector detector = module.getModeDetector();
        if (detector != null) {
            detector.tick();
        }

        TransferMode mode = getCurrentMode();

        // EVENT mode: Task does nothing, everything is handled by Vanilla + Event
        if (mode == TransferMode.EVENT) {
            lastExecutionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            return;
        }

        // TASK or HYBRID mode: Task handles Filter Hoppers
        Map<String, HopperState> hoppers = module.getActiveHoppers();
        if (hoppers.isEmpty()) {
            lastExecutionTimeMs = 0;
            return;
        }

        int maxPerServer = plugin.getMainConfig().getInt("performance.limits.max-per-server", -1);
        int serverProcessedCount = 0;

        for (Map.Entry<String, HopperState> entry : hoppers.entrySet()) {
            String key = entry.getKey();
            HopperState state = entry.getValue();

            Location loc = parseKey(key);
            if (loc == null) continue;

            Block block = loc.getBlock();
            if (block.getType() != Material.HOPPER) continue;

            // Only process Hoppers with Filter
            if (filterModule == null || filterModule.getFilter(loc) == null) {
                continue;
            }

            // Check transfer tick interval
            int targetTick = plugin.getMainConfig().getTransferTick();
            state.incrementTick();
            if (state.getTickCounter() < targetTick) continue;
            state.resetTick();

            // Server limit
            if (maxPerServer > 0 && serverProcessedCount >= maxPerServer) break;

            // Get BlockState
            BlockState bState = block.getState();
            if (!(bState instanceof Hopper)) continue;
            Hopper hopper = (Hopper) bState;

            serverProcessedCount++;

            int amount = plugin.getMainConfig().getTransferAmount();
            int typesLimit = plugin.getMainConfig().getTypeOfTransfer();

            if (mode == TransferMode.TASK) {
                // === TASK MODE: Full Control ===
                // Lock Vanilla cooldown
                hopper.setTransferCooldown(127);
                hopper.update(true, false);

                // Direct item transfer logic
                processPull(block, amount, typesLimit);
                processTransfer(block, amount, typesLimit);

                if (detector != null) {
                    detector.reportTransferAttempt();
                    detector.reportTransferSuccess(); // Assume success for detector cycle
                }

            } else if (mode == TransferMode.HYBRID) {
                // === HYBRID MODE: Push only ===
                processTransfer(block, amount, typesLimit);

                if (detector != null) {
                    detector.reportTransferAttempt();
                    detector.reportTransferSuccess();
                }
            }
        }

        lastExecutionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
    }

    private TransferMode getCurrentMode() {
        ModeDetector detector = module.getModeDetector();
        if (detector != null) {
            return detector.getActiveMode();
        }
        String configMode = plugin.getMainConfig().getString("settings.transfer-mode", "EVENT");
        return TransferMode.fromString(configMode);
    }

    private Location parseKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length < 4) return null;
            org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
            if (world == null) return null;
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }

    public long getLastExecutionTimeMs() {
        return lastExecutionTimeMs;
    }

    private void processPull(Block hopperBlock, int amount, int typesLimit) {
        Block above = hopperBlock.getRelative(org.bukkit.block.BlockFace.UP);
        Inventory source = getInventorySafe(above);
        Inventory destination = getInventorySafe(hopperBlock);

        if (source == null || destination == null) return;

        executeTransfer(source, destination, amount, typesLimit, hopperBlock.getLocation(), "PULL");
    }

    private void processTransfer(Block hopperBlock, int amount, int typesLimit) {
        Inventory source = getInventorySafe(hopperBlock);
        if (source == null) return;

        org.bukkit.block.data.type.Hopper hopperData = (org.bukkit.block.data.type.Hopper) hopperBlock.getBlockData();
        Block target = hopperBlock.getRelative(hopperData.getFacing());
        Inventory destination = getInventorySafe(target);

        if (destination == null) return;

        executeTransfer(source, destination, amount, typesLimit, hopperBlock.getLocation(), "PUSH");
    }

    private boolean executeTransfer(Inventory source, Inventory destination, int maxAmount, int typesLimit, Location hopperLoc, String action) {
        int movedCount = 0;
        Set<Material> transferredTypes = new HashSet<>();
        boolean movedAny = false;

        for (int i = 0; i < source.getSize(); i++) {
            ItemStack item = source.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            // 1. Check Hopper's own filter
            if (action.equals("PULL")) {
                // Pull: Always check filter of receiving Hopper
                if (filterModule != null && !filterModule.isAllowed(hopperLoc, item)) continue;
            } else {
                // Push: Check block-move-out setting
                if (plugin.getMainConfig().getBoolean("settings.filter-actions.block-move-out", true)) {
                    if (filterModule != null && !filterModule.isAllowed(hopperLoc, item)) continue;
                }
            }

            // 2. Check destination filter (if destination is also a Filter Hopper)
            if (action.equals("PUSH") && plugin.getMainConfig().getBoolean("settings.filter-actions.block-move-in", true)) {
                Location destLoc = destination.getLocation();
                if (destLoc != null && filterModule != null && filterModule.getFilter(destLoc) != null) {
                    if (!filterModule.isAllowed(destLoc, item)) continue;
                }
            }

            // 3. Check Type Limit
            if (typesLimit > 0 && !transferredTypes.contains(item.getType())) {
                if (transferredTypes.size() >= typesLimit) continue;
            }

            int toMove = Math.min(item.getAmount(), maxAmount - movedCount);
            if (toMove <= 0) break;

            ItemStack clone = item.clone();
            clone.setAmount(toMove);

            // 4. IMPORTANT: Fire event to ask permission from protection plugins (LWC, Towny, WorldGuard...)
            plugin.setInternalCall(true);
            InventoryMoveItemEvent event = new InventoryMoveItemEvent(source, clone, destination, true);
            plugin.getServer().getPluginManager().callEvent(event);
            plugin.setInternalCall(false);

            if (event.isCancelled()) {
                if (module.isDeepDebug()) {
                    StringBuilder culprits = new StringBuilder();
                    for (org.bukkit.plugin.RegisteredListener rl : event.getHandlers().getRegisteredListeners()) {
                        culprits.append(rl.getPlugin().getName()).append(" (").append(rl.getPriority()).append("), ");
                    }
                    plugin.getLogger().warning("[DeepDebug] [TASK] " + action + " BLOCKED by another plugin!");
                    plugin.getLogger().warning("[DeepDebug] [TASK] Possible culprits: " + culprits.toString());
                }
                continue; 
            }

            // 5. If everything is okay -> Perform direct transfer
            Map<Integer, ItemStack> leftover = destination.addItem(clone);
            int actuallyAdded = toMove - (leftover.isEmpty() ? 0 : leftover.get(0).getAmount());

            if (actuallyAdded > 0) {
                item.setAmount(item.getAmount() - actuallyAdded);
                source.setItem(i, item.getAmount() > 0 ? item : null);

                movedCount += actuallyAdded;
                transferredTypes.add(item.getType());
                movedAny = true;

                // Log realtime for Inspect
                ItemStack logItem = clone.clone();
                logItem.setAmount(actuallyAdded);
                logTransfer(hopperLoc, logItem, actuallyAdded, action);

                if (movedCount >= maxAmount) break;
            }
        }
        return movedAny;
    }

    private Inventory getInventorySafe(Block block) {
        if (block == null) return null;
        BlockState state = block.getState();
        
        // DoubleChest support
        if (state instanceof org.bukkit.block.Chest) {
            return ((org.bukkit.block.Chest) state).getInventory();
        }
        
        if (state instanceof InventoryHolder) {
            return ((InventoryHolder) state).getInventory();
        }
        return null;
    }

    private void logTransfer(Location loc, ItemStack item, int amount, String action) {
        module.logTransfer(loc, item, amount, action);
    }

    private String toKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown";
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public void forceTick(Location loc) {
        HopperState state = module.getActiveHoppers().get(toKey(loc));
        if (state == null) return;

        Block block = loc.getBlock();
        if (!(block.getState() instanceof Hopper)) return;

        processPull(block, plugin.getMainConfig().getTransferAmount(), 99);
        processTransfer(block, plugin.getMainConfig().getTransferAmount(), 99);
    }

    public java.util.List<String> mockPull(Hopper hopper) {
        java.util.List<String> logs = new java.util.ArrayList<>();
        Block above = hopper.getBlock().getRelative(org.bukkit.block.BlockFace.UP);
        logs.add("§8- §fBlock above: §e" + above.getType());

        if (!(above.getState() instanceof Container)) {
            logs.add("§8- §c[!] ERROR: §fBlock above is not a Container.");
            return logs;
        }

        Inventory source = ((Container) above.getState()).getInventory();
        logs.add("§8- §fInventory: §e" + source.getType() + " (" + source.getSize() + " slots)");

        boolean foundAny = false;
        for (ItemStack item : source.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            foundAny = true;
            boolean allowed = filterModule == null || filterModule.isAllowed(hopper.getLocation(), item);
            logs.add("§8- §fItem §e" + item.getType() + "§f: " + (allowed ? "§a[ALLOWED]" : "§c[BLOCKED]"));
        }

        if (!foundAny) logs.add("§8- §c[!] Warning: §fContainer is empty.");

        return logs;
    }
}
