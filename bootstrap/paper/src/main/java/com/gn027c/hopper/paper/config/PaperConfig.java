package com.gn027c.hopper.paper.config;

import com.gn027c.hopper.api.IHopperConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Wrapper cho Bukkit ConfigurationSection, implement IHopperConfig.
 */
public class PaperConfig implements IHopperConfig {
    private final ConfigurationSection config;

    public PaperConfig(ConfigurationSection config) {
        this.config = config;
    }

    @Override
    public int getTransferTick() {
        return config.getInt("hopper.transfer-tick", 8);
    }

    @Override
    public int getTransferAmount() {
        return config.getInt("hopper.transfer-amount", 1);
    }

    @Override
    public int getTypeOfTransfer() {
        return config.getInt("hopper.type-of-transfer", 1);
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    @Override
    public int getInt(String path) {
        return config.getInt(path);
    }

    @Override
    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    @Override
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    @Override
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    @Override
    public Object get(String path) {
        return config.get(path);
    }

    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    public ConfigurationSection getConfig() {
        return config;
    }
}
