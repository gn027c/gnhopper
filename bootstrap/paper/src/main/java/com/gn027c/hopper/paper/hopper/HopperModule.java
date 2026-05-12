package com.gn027c.hopper.paper.hopper;

import com.gn027c.hopper.core.manager.AbstractModule;
import com.gn027c.hopper.paper.gnhopper;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module managing core Hopper logic: Speed, Amount, and Item Types.
 */
public class HopperModule extends AbstractModule {
    private final gnhopper plugin;
    private final Map<String, HopperState> activeHoppers = new ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> realtimeInspectors = new java.util.HashSet<>();
    private final java.util.Set<String> testingHoppers = new java.util.HashSet<>();
    private HopperTransferTask transferLogic;
    private BukkitTask transferTask;
    private ModeDetector modeDetector;
    private boolean deepDebug = false;

    public HopperModule(gnhopper plugin) {
        super("HopperModule");
        this.plugin = plugin;
    }

    public boolean isDeepDebug() {
        return deepDebug;
    }

    public void toggleDeepDebug() {
        this.deepDebug = !this.deepDebug;
    }

    @Override
    protected void onEnable() {
        // Initialize ModeDetector (read config, determine initial mode)
        this.modeDetector = new ModeDetector(plugin, this);

        // Start transfer management task every tick
        this.transferLogic = new HopperTransferTask(plugin, this);
        this.transferTask = this.transferLogic.runTaskTimer(plugin, 1L, 1L);
        plugin.getServer().getPluginManager().registerEvents(new HopperListener(plugin, this), plugin);
    }

    public HopperTransferTask getTransferLogic() {
        return transferLogic;
    }

    public ModeDetector getModeDetector() {
        return modeDetector;
    }

    @Override
    protected void onDisable() {
        if (transferTask != null) {
            transferTask.cancel();
        }
        activeHoppers.clear();
    }

    public Map<String, HopperState> getActiveHoppers() {
        return activeHoppers;
    }

    public void logTransfer(Location loc, org.bukkit.inventory.ItemStack item, int amount, String action) {
        HopperState state = activeHoppers.get(toKey(loc));
        if (state == null) return;

        String entry = action + " " + amount + "x " + item.getType().name();
        state.addHistory(entry);

        if (!realtimeInspectors.isEmpty()) {
            String msg = "§d[Inspect] §7" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " §f" + entry;
            for (java.util.UUID uuid : realtimeInspectors) {
                org.bukkit.entity.Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    org.bukkit.block.Block targetBlock = player.getTargetBlockExact(6); // Max view distance: 6 blocks
                    if (targetBlock != null && targetBlock.getLocation().equals(loc)) {
                        player.sendMessage(msg);
                    }
                }
            }
        }
    }

    public void trackHopper(Location loc) {
        String key = toKey(loc);
        if (!activeHoppers.containsKey(key)) {
            activeHoppers.put(key, new HopperState());
        }
    }

    public void untrackHopper(Location loc) {
        activeHoppers.remove(toKey(loc));
    }

    public java.util.Set<String> getTestingHoppers() {
        return testingHoppers;
    }

    public void addTestingHopper(Location loc) {
        testingHoppers.add(toKey(loc));
    }

    public void removeTestingHopper(Location loc) {
        testingHoppers.remove(toKey(loc));
    }

    public String toKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown";
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public java.util.Set<java.util.UUID> getRealtimeInspectors() {
        return realtimeInspectors;
    }
}
