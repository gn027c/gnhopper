package com.gn027c.hopper.core.config;

import com.gn027c.hopper.api.IHopperConfig;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Service for managing multi-language messages.
 * Platform-independent.
 */
public class MessageService {
    private IHopperConfig langConfig;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public void setLangConfig(IHopperConfig langConfig) {
        this.langConfig = langConfig;
    }

    public void sendMessage(Audience audience, String path) {
        sendMessage(audience, path, new String[0]);
    }

    public void sendActionBar(Audience audience, String message) {
        audience.sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message));
    }

    public void sendMessage(Audience audience, String path, String... replacements) {
        String message = getRawMessage("messages." + path);
        
        // Add prefix
        String prefix = getRawMessage("messages.prefix");
        if (prefix != null && !path.contains("header")) {
            message = prefix + message;
        }

        // Replace placeholders
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }

        audience.sendMessage(serializer.deserialize(message));
    }

    private String getRawMessage(String path) {
        if (langConfig == null) return path;
        String msg = langConfig.getString(path);
        return msg != null ? msg : path;
    }

    public Component parse(String text) {
        return serializer.deserialize(text);
    }
}
