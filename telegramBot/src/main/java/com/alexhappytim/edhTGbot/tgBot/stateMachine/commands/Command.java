package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.InputStrategy;
import org.telegram.telegrambots.meta.api.objects.Update;
import lombok.Getter;

/**
 * Command pattern implementation for bot actions.
 * Each command knows how to execute itself and uses an array of InputStrategies for input handling.
 */
@Getter
public abstract class Command {
    private final String key;
    private final String nextKeyboard;
    private final InputStrategy[] inputStrategies;

    public Command(String key, String nextKeyboard, InputStrategy... inputStrategies) {
        this.key = key;
        this.nextKeyboard = nextKeyboard;
        this.inputStrategies = inputStrategies == null ? new InputStrategy[0] : inputStrategies;
    }

    public int getRequiredInputs() {
        return inputStrategies.length;
    }

    public String getInputPrompt(int stepIndex) {
        if (inputStrategies.length == 0 || stepIndex >= inputStrategies.length) {
            return "";
        }
        return inputStrategies[stepIndex].getPrompt();
    }

    public boolean needsInput() {
        return inputStrategies.length > 0;
    }

    public InputStrategy[] getInputStrategies() {
        return inputStrategies;
    }

    /**
     * Extract user ID from update (handles both Message and CallbackQuery)
     */
    protected long getUserId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId();
        }
        throw new IllegalArgumentException("Update does not contain user information");
    }

    /**
     * Extract chat ID from update (handles both Message and CallbackQuery)
     */
    protected long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        throw new IllegalArgumentException("Update does not contain chat information");
    }

    /**
     * Extract username from update (handles both Message and CallbackQuery)
     */
    protected String getUsername(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getUserName();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getUserName();
        }
        throw new IllegalArgumentException("Update does not contain user information");
    }

    public abstract void execute(BotFacade bot, Update update);
}
