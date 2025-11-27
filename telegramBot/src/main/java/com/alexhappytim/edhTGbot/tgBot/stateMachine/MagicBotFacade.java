package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Facade exposing limited bot operations to commands to avoid tight coupling.
 */
public interface MagicBotFacade {
    void send(long chatId, String text);
    void send(long chatId, String text, InlineKeyboardMarkup kb);
}
