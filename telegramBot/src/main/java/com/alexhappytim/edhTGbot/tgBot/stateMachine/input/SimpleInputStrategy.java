package com.alexhappytim.edhTGbot.tgBot.stateMachine.input;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.UserSession;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Simple input strategy for a single input with no validation
 */
public class SimpleInputStrategy implements InputStrategy {
    private final String prompt;

    public SimpleInputStrategy(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    @Override
    public void collectInput(BotFacade bot, Update update) throws Exception {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            throw new IllegalArgumentException("Update does not contain a message with text");
        }
        
        long userId = update.getMessage().getFrom().getId();
        UserSession session = bot.getSession(userId);
        
        String input = update.getMessage().getText();
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be empty");
        }
        
        session.getInputs().add(input);
    }

    @Override
    public boolean isPreconditionMet(BotFacade bot, Update update) {
        return false;
    }
}
