package com.gn027c.hopper.core.config;

import com.gn027c.hopper.api.IHopperConfig;

/**
 * Language Manager — loads language files and provides them to MessageService.
 */
public class LanguageManager {
    private final MessageService messageService;
    private IHopperConfig langConfig;

    public LanguageManager(MessageService messageService) {
        this.messageService = messageService;
    }

    public void setLangConfig(IHopperConfig langConfig) {
        this.langConfig = langConfig;
        this.messageService.setLangConfig(langConfig);
    }

    public IHopperConfig getLangConfig() {
        return langConfig;
    }

    public MessageService getMessageService() {
        return messageService;
    }
}
