package com.gn027c.hopper.paper;

import com.gn027c.hopper.core.config.LanguageManager;
import com.gn027c.hopper.core.config.MessageService;
import com.gn027c.hopper.core.manager.ModuleManager;
import com.gn027c.hopper.paper.command.LampCommandManager;
import com.gn027c.hopper.paper.config.PaperConfig;
import com.gn027c.hopper.paper.hopper.HopperModule;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class gnhopper extends JavaPlugin {
    private static gnhopper instance;
    private BukkitAudiences adventure;
    
    private MessageService messageService;
    private LanguageManager languageManager;
    private ModuleManager moduleManager;
    private PaperConfig mainConfig;
    private boolean internalCall = false;

    @Override
    public void onEnable() {
        instance = this;
        this.adventure = BukkitAudiences.create(this);

        try {
            getLogger().info("--- Starting gnhopper ---");

            // 1. Setup Config & Language
            saveDefaultConfig();
            this.mainConfig = new PaperConfig(getConfig());
            
            this.messageService = new MessageService();
            this.languageManager = new LanguageManager(messageService);
            loadLanguage();
            validateConfig();

            // 2. Setup Modules
            this.moduleManager = new ModuleManager();
            this.moduleManager.registerModule(new HopperModule(this));
            this.moduleManager.registerModule(new com.gn027c.hopper.paper.filter.FilterModule(this));
            this.moduleManager.registerModule(new com.gn027c.hopper.paper.gui.GUIModule(this));
            
            this.moduleManager.enableModules();

            // 3. Register Commands
            LampCommandManager commandManager = new LampCommandManager(this);
            commandManager.registerCommands();

            getLogger().info("--- gnhopper is ready! ---");
        } catch (Exception e) {
            getLogger().severe("Failed to enable gnhopper!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.moduleManager != null) {
            this.moduleManager.disableModules();
        }

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        
        instance = null;
    }

    public void loadLanguage() {
        String lang = mainConfig.getString("settings.language", "vi");
        String fileName = "languages/" + lang + ".yml";
        File file = new File(getDataFolder(), fileName);
        
        if (!file.exists()) {
            saveResource(fileName, false);
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        this.languageManager.setLangConfig(new PaperConfig(yaml));
    }

    public void reloadPlugin() {
        reloadConfig();
        this.mainConfig = new PaperConfig(getConfig());
        loadLanguage();
        validateConfig();
        getLogger().info("Plugin has been reloaded.");
    }

    public static gnhopper getInstance() {
        return instance;
    }

    public BukkitAudiences adventure() {
        return this.adventure;
    }

    public Audience getAudience(Object obj) {
        if (obj instanceof Audience) return (Audience) obj;
        return adventure().sender((org.bukkit.command.CommandSender) obj);
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public PaperConfig getMainConfig() {
        return mainConfig;
    }

    public boolean isInternalCall() { return internalCall; }
    public void setInternalCall(boolean internalCall) { this.internalCall = internalCall; }

    private void validateConfig() {
        String mode = mainConfig.getString("settings.filter-mode", "ITEM_FRAME");
        int slots = mainConfig.getInt("settings.default-filter-slots", 1);

        if (mode.equalsIgnoreCase("ITEM_FRAME")) {
            if (slots > 6) {
                String msg = languageManager.getLangConfig().getString("error.config.invalid-slots-item-frame", 
                    "ITEM_FRAME mode supports max 6 slots. Adjusting {value} to 6.");
                getLogger().warning(msg.replace("{value}", String.valueOf(slots)));
                getConfig().set("settings.default-filter-slots", 6);
                this.mainConfig = new PaperConfig(getConfig()); // Re-wrap
            }
        } else if (mode.equalsIgnoreCase("GUI")) {
            if (slots > 54 || slots <= 0) {
                int corrected = slots <= 0 ? 1 : 54;
                String msg = languageManager.getLangConfig().getString("error.config.invalid-slots-gui", 
                    "GUI mode supports 1-54 slots. Adjusting {value} to " + corrected + ".");
                getLogger().severe(msg.replace("{value}", String.valueOf(slots)));
                getConfig().set("settings.default-filter-slots", corrected);
                this.mainConfig = new PaperConfig(getConfig()); // Re-wrap
            }
        }
    }
}
