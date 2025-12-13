package com.alexhappytim.edhTGbot.tgBot.stateMachine.input;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Strategy pattern for handling a single input.
 * Commands will use an array of these strategies for multi-step input collection.
 */
public interface InputStrategy {
    /**
     * Get the prompt message for this input step
     */
    String getPrompt();

    /**
     * Validate and collect a single input from the user
     * @throws IllegalArgumentException if input is invalid
     * @throws IllegalStateException if preconditions not met
     */
    void collectInput(BotFacade bot, Update update) throws Exception;

    /**
     * Check if this input's preconditions are met
     * (e.g., tournament ID exists in session before asking for player name)
     */
    boolean isPreconditionMet(BotFacade bot, Update update);
}
