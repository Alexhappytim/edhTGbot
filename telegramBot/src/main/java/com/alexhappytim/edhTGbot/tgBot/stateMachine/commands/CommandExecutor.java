package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Functional interface used by BotCommand to perform execution.
 */
@FunctionalInterface
public interface CommandExecutor {
    void execute(BotFacade bot, Update update, UserSessionAdapter session, String input);
}
