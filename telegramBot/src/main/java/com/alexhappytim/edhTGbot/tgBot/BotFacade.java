package com.alexhappytim.edhTGbot.tgBot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Facade exposing limited bot operations to commands to avoid tight coupling.
 */
public interface BotFacade {
    void sendMessage(long chatID, String text);
    void sendMessage(long chatID, String text, InlineKeyboardMarkup kb);
    void editMessage(long chatID,long messageID, String text, InlineKeyboardMarkup kb);
    UserSession getSession(long userId);
    void setSession(long userId, UserSession session);

    Logger getLogger();
    String getRestBaseUrl();
    RestTemplate getRestTemplate();
    ObjectMapper getObjectMapper();

}
