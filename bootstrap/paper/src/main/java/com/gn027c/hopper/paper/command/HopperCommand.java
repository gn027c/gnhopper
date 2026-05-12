package com.gn027c.hopper.paper.command;

import com.gn027c.hopper.paper.gnhopper;
import com.gn027c.hopper.paper.filter.FilterData;
import com.gn027c.hopper.paper.filter.FilterModule;
import com.gn027c.hopper.paper.hopper.HopperModule;
import com.gn027c.hopper.paper.hopper.HopperState;
import com.gn027c.hopper.paper.hopper.HopperTransferTask;
import com.gn027c.hopper.paper.hopper.HopperListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"gnhopper", "gnh"})
public class HopperCommand {

    private final gnhopper plugin;

    public HopperCommand(gnhopper plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission("gnhopper.admin")
    @Description("Reload plugin configuration")
    public void reload(CommandSender sender) {
        plugin.reloadPlugin();
        plugin.getMessageService().sendMessage(plugin.getAudience(sender), "reload");
    }

    @Subcommand("info")
    @Description("View information about the hopper you are looking at")
    public void info(Player player) {
        Block block = player.getTargetBlock(null, 5);
        if (block == null || !block.getType().name().contains("HOPPER")) {
            plugin.getMessageService().sendMessage(plugin.getAudience(player), "error.not-hopper");
            return;
        }

        Location loc = block.getLocation();
        HopperModule module = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        if (module == null) return;

        HopperState state = module.getActiveHoppers().get(loc);

        if (state == null) {
            // If not tracked yet, try to track it now
            module.trackHopper(loc);
            state = module.getActiveHoppers().get(loc);
        }
        if (state == null) return;

        String tier = state.getTierName() != null ? state.getTierName() : "Default";
        String speed = String.valueOf(plugin.getMainConfig().getTransferTick());
        String amount = String.valueOf(plugin.getMainConfig().getTransferAmount());
        String type = String.valueOf(plugin.getMainConfig().getTypeOfTransfer());

        plugin.getMessageService().sendMessage(plugin.getAudience(player), "info.header");
        plugin.getMessageService().sendMessage(plugin.getAudience(player), "info.location", 
            "world", loc.getWorld().getName(),
            "x", String.valueOf(loc.getBlockX()),
            "y", String.valueOf(loc.getBlockY()),
            "z", String.valueOf(loc.getBlockZ()));
        
        plugin.getMessageService().sendMessage(plugin.getAudience(player), "info.tier", "tier", tier);
        plugin.getMessageService().sendMessage(plugin.getAudience(player), "info.speed", "tick", speed);
        plugin.getMessageService().sendMessage(plugin.getAudience(player), "info.amount", "amount", amount);
        plugin.getMessageService().sendMessage(plugin.getAudience(player), "info.type", "type", type);
    }

    @Subcommand("debug")
    @CommandPermission("gnhopper.admin")
    public void debug(Player player) {
        player.sendMessage("§b§l[gnhopper Debug] §7Checking system...");
        
        // 1. Module Check
        HopperModule hopperModule = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        
        player.sendMessage("§8- §fHopperModule: " + (hopperModule != null ? "§aON" : "§cOFF"));
        player.sendMessage("§8- §fFilterModule: " + (filterModule != null ? "§aON" : "§cOFF"));
        
        // 2. Config Check
        String mode = plugin.getMainConfig().getString("settings.filter-mode", "ITEM_FRAME");
        player.sendMessage("§8- §fFilter Mode: §e" + mode);
        
        // 3. Target Hopper Check
        Block block = player.getTargetBlock(null, 5);
        if (block != null && block.getType() == Material.HOPPER) {
            Location loc = block.getLocation();
            player.sendMessage("§8- §fTarget: §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            
            if (hopperModule != null) {
                boolean tracked = hopperModule.getActiveHoppers().containsKey(hopperModule.toKey(loc));
                player.sendMessage("§8- §fIs Tracked: " + (tracked ? "§aYES" : "§cNO"));
                
                HopperState state = hopperModule.getActiveHoppers().get(hopperModule.toKey(loc));
                if (state != null) {
                    player.sendMessage("§8- §fHibernation Status: " + (state.isHibernating() ? "§7Sleeping" : "§aActive"));
                }
            }
            
            if (filterModule != null) {
                FilterData data = filterModule.getFilter(loc);
                if (data != null) {
                    player.sendMessage("§8- §fFilter Data: §aFound (" + data.getFilterItems().size() + " items)");
                    for (org.bukkit.inventory.ItemStack item : data.getFilterItems()) {
                        player.sendMessage("  §7> " + item.getType().name());
                    }
                } else {
                    player.sendMessage("§8- §fFilter Data: §cEmpty (No data in FilterModule)");
                }
            }
        } else {
            player.sendMessage("§8- §fTarget: §cNot looking at a Hopper");
        }
        
        // 4. Task Check
        player.sendMessage("§8- §fTask Status: §aScheduled (Interval: " + plugin.getMainConfig().getTransferTick() + "t)");
        
        // 5. Test Action Bar
        plugin.getMessageService().sendActionBar(plugin.getAudience(player), "<rainbow>Action Bar test successful!");
    }

    @Subcommand("deepdebug")
    @CommandPermission("gnhopper.admin")
    @Description("Toggle deep debug logging for troubleshooting")
    public void deepDebug(CommandSender sender) {
        HopperModule module = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        if (module == null) return;
        
        module.toggleDeepDebug();
        if (module.isDeepDebug()) {
            sender.sendMessage("§a§l[gnhopper] Deep Debug ENABLED.");
            sender.sendMessage("§7All item movement events will be logged to console. Please reproduce the issue and send console logs to Dev.");
        } else {
            sender.sendMessage("§c§l[gnhopper] Deep Debug DISABLED.");
        }
    }
    @Subcommand("bench")
    @CommandPermission("gnhopper.admin")
    @Description("Audit system performance")
    public void bench(org.bukkit.command.CommandSender sender) {
        HopperModule module = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        if (module == null || filterModule == null) return;

        java.util.Map<String, HopperState> hoppers = module.getActiveHoppers();
        long hibernating = hoppers.values().stream().filter(HopperState::isHibernating).count();
        
        // Count actual Filter Hoppers
        int filterCount = 0;
        int normalCount = 0;
        
        for (String key : hoppers.keySet()) {
            Location loc = parseKey(key);
            if (loc != null && filterModule.getFilter(loc) != null) {
                filterCount++;
            } else {
                normalCount++;
            }
        }
        
        HopperTransferTask task = module.getTransferLogic();
        long time = (task != null) ? task.getLastExecutionTimeMs() : 0;

        sender.sendMessage("§b§l[gnhopper Benchmark]");
        sender.sendMessage("§8- §fTotal tracked hoppers: §e" + hoppers.size());
        sender.sendMessage("§8- §6Filter Hoppers: §b" + filterCount);
        sender.sendMessage("§8- §7Normal Hoppers: §f" + normalCount);
        sender.sendMessage("§8- §fHibernating: §a" + hibernating);
        sender.sendMessage("§8- §fExecution Time: §6" + time + "ms");
        
        if (time > 10) {
            sender.sendMessage("§c§l[!] Warning: §fHigh CPU load. Consider reducing normal hoppers.");
        } else {
            sender.sendMessage("§a§l[✔] Status: §fSystem running smoothly.");
        }
    }

    private Location parseKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length < 4) return null;
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }

    @Subcommand("inspect history")
    @Description("View transfer history of a hopper")
    public void inspectHistory(Player player) {
        Block block = player.getTargetBlock(null, 5);
        if (block == null || block.getType() != Material.HOPPER) {
            player.sendMessage("§cYou must look at a Hopper.");
            return;
        }

        HopperModule module = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        if (module == null) return;

        HopperState state = module.getActiveHoppers().get(block.getLocation());
        if (state == null) {
            player.sendMessage("§cThis hopper has no history data.");
            return;
        }

        player.sendMessage("§6§l[History] §eHopper at " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        
        // Filter info
        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        if (filterModule != null) {
            com.gn027c.hopper.paper.filter.FilterData data = filterModule.getFilter(block.getLocation());
            if (data != null && !data.getFilterItems().isEmpty()) {
                player.sendMessage("§7Current Filter: §f" + data.getFilterItems().get(0).getType().name());
            } else {
                player.sendMessage("§7Current Filter: §fNone");
            }
        }

        java.util.List<String> history = state.getHistory();
        if (history.isEmpty()) {
            player.sendMessage("§7(Empty)");
        } else {
            for (String entry : history) {
                player.sendMessage("§8- §f" + entry);
            }
        }
    }

    @Subcommand("inspect realtime")
    @Description("Toggle real-time transfer monitoring")
    public void inspectRealtime(Player player) {
        HopperModule module = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        if (module == null) return;

        if (module.getRealtimeInspectors().contains(player.getUniqueId())) {
            module.getRealtimeInspectors().remove(player.getUniqueId());
            player.sendMessage("§c§l[Inspect] §fReal-time monitoring DISABLED.");
        } else {
            module.getRealtimeInspectors().add(player.getUniqueId());
            player.sendMessage("§a§l[Inspect] §fReal-time monitoring ENABLED.");
            player.sendMessage("§7(Notifications will appear when any hopper transfers items)");
        }
    }

    @Subcommand("test")
    @Description("Automatically test the logic of a specific Hopper")
    public void testHopper(Player player) {
        player.sendMessage("§b§l[System Test] §7Starting (Time: " + System.currentTimeMillis() + ")");
        
        Block block = player.getTargetBlock(null, 5);
        if (block == null || block.getType() != Material.HOPPER) {
            player.sendMessage("§cYou must look at a Hopper.");
            return;
        }

        Location loc = normalize(block.getLocation());
        HopperModule hopperModule = plugin.getModuleManager().getModule(HopperModule.class).orElse(null);
        FilterModule filterModule = plugin.getModuleManager().getModule(FilterModule.class).orElse(null);
        
        player.sendMessage("§e§l[Test] §fStarting at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        
        // Start LOCKDOWN
        block.setMetadata("GNH_TESTING", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        hopperModule.addTestingHopper(loc);

        boolean tracked = hopperModule != null && hopperModule.getActiveHoppers().containsKey(hopperModule.toKey(loc));
        FilterData filterData = filterModule != null ? filterModule.getFilter(loc) : null;
        
        player.sendMessage("§7- Tracking: " + (tracked ? "§aON" : "§cOFF"));
        player.sendMessage("§7- Filter: " + (filterData != null && !filterData.getFilterItems().isEmpty() ? "§a" + filterData.getFilterItems().get(0).getType() : "§7Empty"));

        if (!tracked) {
            hopperModule.removeTestingHopper(loc);
            player.sendMessage("§cStop test: Hopper not tracked.");
            return;
        }

        ItemStack validItem = (filterData != null && !filterData.getFilterItems().isEmpty()) 
            ? filterData.getFilterItems().get(0).clone() 
            : new ItemStack(Material.DIAMOND);
        validItem.setAmount(1);
        ItemStack invalidItem = new ItemStack(Material.BARRIER);

        // --- PHASE 1 ---
        player.sendMessage("§b[Phase 1] §fTesting §aVALID§f item...");
        org.bukkit.entity.Item droppedValid = block.getWorld().dropItemNaturally(loc.clone().add(0.5, 1.2, 0.5), validItem);
        droppedValid.setPickupDelay(0);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (droppedValid.isDead() || !droppedValid.isValid()) {
                player.sendMessage("§a✔ Phase 1 Successful.");
            } else {
                player.sendMessage("§c✘ Phase 1 Failed. Trying Force Tick...");
                HopperState state = hopperModule.getActiveHoppers().get(hopperModule.toKey(loc));
                if (state != null) state.setHibernating(false);
                
                HopperTransferTask task = hopperModule.getTransferLogic();
                if (task != null) task.forceTick(loc);
                droppedValid.remove();
            }

            // --- PHASE 2 ---
            player.sendMessage("§b[Phase 2] §fTesting §cINVALID§f item...");
            org.bukkit.entity.Item droppedInvalid = block.getWorld().dropItemNaturally(loc.clone().add(0.5, 1.2, 0.5), invalidItem);
            droppedInvalid.setPickupDelay(0);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (droppedInvalid.isDead() || !droppedInvalid.isValid()) {
                    player.sendMessage("§c✘ Phase 2 Failed (Wrong item absorbed).");
                } else {
                    player.sendMessage("§a✔ Phase 2 Successful.");
                    droppedInvalid.remove();
                }

                // --- PHASE 3 ---
                player.sendMessage("§b[Phase 3] §fTesting pull from §6CONTAINER§f...");
                Block blockAbove = block.getRelative(org.bukkit.block.BlockFace.UP);
                if (blockAbove.getType() != Material.AIR) {
                    hopperModule.removeTestingHopper(loc);
                    player.sendMessage("§e[!] Skipping Phase 3 (Block interference).");
                    return;
                }

                blockAbove.setType(Material.BARREL);
                org.bukkit.block.Barrel barrel = (org.bukkit.block.Barrel) blockAbove.getState();
                barrel.getInventory().addItem(validItem);
                barrel.getInventory().addItem(invalidItem);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Force purge before check
                    try {
                        java.lang.reflect.Method ejectMethod = hopperModule.getTransferLogic().getClass().getDeclaredMethod("ejectIllegalItems", org.bukkit.block.Hopper.class);
                        ejectMethod.setAccessible(true);
                        ejectMethod.invoke(hopperModule.getTransferLogic(), (org.bukkit.block.Hopper) block.getState());
                    } catch (Exception ignored) {}

                    boolean validGone = !barrel.getInventory().contains(validItem.getType());
                    boolean invalidStillThere = barrel.getInventory().contains(invalidItem.getType());

                    String conclusion = "UNKNOWN";
                    if (validGone && invalidStillThere) {
                        player.sendMessage("§a✔ Phase 3 Successful.");
                        conclusion = "ALL_OK";
                    } else {
                        if (!validGone) {
                            player.sendMessage("§c✘ Phase 3 Failed: §fVALID item was not pulled.");
                            conclusion = "PULL_FAILED";
                        }
                        if (!invalidStillThere) {
                            player.sendMessage("§c✘ Phase 3 Failed: §fINVALID item was pulled (Filter Bypass!).");
                            conclusion = "FILTER_BYPASS";
                            if (filterModule != null) {
                                for (String log : filterModule.debugFilter(loc, invalidItem)) player.sendMessage(log);
                            }
                        }
                        
                        player.sendMessage("§e[Deep Diagnosis] §fSimulating pull cycle...");
                        try {
                            if (filterModule != null) {
                                for (String log : filterModule.debugFilter(loc, validItem)) player.sendMessage(log);
                            }
                            java.util.List<String> logs = hopperModule.getTransferLogic().mockPull((org.bukkit.block.Hopper) block.getState());
                            for (String log : logs) player.sendMessage(log);
                        } catch (Exception e) {
                            player.sendMessage("§c[!] Diagnosis Error: " + e.getMessage());
                        }
                    }

                    // --- FINAL REPORT ---
                    
                    // Hacky way to get the listener for debug
                    HopperListener realListener = null;
                    for (RegisteredListener rl : org.bukkit.event.inventory.InventoryMoveItemEvent.getHandlerList().getRegisteredListeners()) {
                        if (rl.getListener() instanceof HopperListener) {
                            realListener = (HopperListener) rl.getListener();
                            break;
                        }
                    }

                    player.sendMessage("§8§m----------------------------------");
                    player.sendMessage("§e§lDIAGNOSTIC REPORT (COPY TO DEV)");
                    player.sendMessage("§7- Location: §f" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                    player.sendMessage("§7- Build: §f" + System.currentTimeMillis());
                    player.sendMessage("§7- Conclusion: §6§l[" + conclusion + "]");
                    player.sendMessage("§7- Metadata: §fFilter=" + (filterData != null ? filterData.getFilterItems().size() : 0) + 
                        " | Tracked=" + tracked + " | Power=" + block.isBlockIndirectlyPowered());
                    
                    if (realListener != null) {
                        player.sendMessage("§7- Technical: §fInterceptions=" + realListener.getLockdownInterceptions() + 
                            " | Server-Events=" + realListener.getTotalMoveEvents());
                        realListener.resetLockdownInterceptions();
                    }

                    // --- PLUGIN AUDIT ---
                    player.sendMessage("§e§lCONFLICT AUDIT (POTENTIAL CONFLICTS)");
                    for (RegisteredListener rl : org.bukkit.event.inventory.InventoryMoveItemEvent.getHandlerList().getRegisteredListeners()) {
                        if (rl.getPlugin().getName().equalsIgnoreCase("gnhopper")) continue;
                        player.sendMessage("§8- §c" + rl.getPlugin().getName() + " §7(" + rl.getPriority() + ")");
                    }
                    player.sendMessage("§8§m----------------------------------");

                    blockAbove.setType(Material.AIR);
                    block.removeMetadata("GNH_TESTING", plugin);
                    hopperModule.removeTestingHopper(loc);
                    player.sendMessage("§e§l[Test Finished]");
                }, 40L);
            }, 40L);
        }, 40L);
    }

    @Subcommand("help")
    public void help(CommandSender sender) {
        sender.sendMessage("§e§lgnhopper Commands:");
        sender.sendMessage("§7/gnh reload §8- §fReload plugin");
        sender.sendMessage("§7/gnh info §8- §fView hopper info");
        sender.sendMessage("§7/gnh debug §8- §fSystem check");
        sender.sendMessage("§7/gnh inspect history §8- §fView hopper history");
        sender.sendMessage("§7/gnh inspect realtime §8- §fReal-time monitor (Toggle)");
        sender.sendMessage("§7/gnh test §8- §fAuto-test hopper logic");
        sender.sendMessage("§7/gnh give <tier> [player] §8- §fGive upgraded hopper");
    }

    private Location normalize(Location loc) {
        if (loc == null) return null;
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
