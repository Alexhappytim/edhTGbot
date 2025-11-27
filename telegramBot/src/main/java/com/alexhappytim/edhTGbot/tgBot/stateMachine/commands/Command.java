package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;

import org.telegram.telegrambots.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.meta.api.objects.Update;
import lombok.Getter;

/**
 * Command pattern implementation for bot actions.
 * Each command knows how to execute itself and whether it requires user input.
 */
@Getter
public abstract class Command{
    private final String key;          // unique key, used in callbackData after cmd:
//    private final CommandGroup group;  // logical grouping
    private final boolean needsInput;  // whether we must collect a single line input first
    private final String inputPrompt;  // prompt to show user when awaiting input


    public Command(String key,
                      boolean needsInput,
                      String inputPrompt) {
        this.key = key;
//        this.group = group;
        this.needsInput = needsInput;
        this.inputPrompt = inputPrompt;
    }

    public abstract void execute(BotFacade bot, MessageContext ctx, UserSessionAdapter session);
}
