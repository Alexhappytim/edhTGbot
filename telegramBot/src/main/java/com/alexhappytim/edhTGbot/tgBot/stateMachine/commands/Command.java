package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;

import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.objects.Update;
import lombok.Getter;

/**
 * Command pattern implementation for bot actions.
 * Each command knows how to execute itself and whether it requires user input.
 */
@Getter
public abstract class Command{
    private final String key;
    private final String nextKeyboard;

    // Блок для обработки ввода. Если одним из вводов является код турнира - его не нужно прибавлять к requiredInputs
    private final int requiredInputs;
    private final String[] inputPrompts;
    private final Boolean isTournamentCodeReq;

    @SneakyThrows
    public Command(String key,
                   int requiredInputs,
                   String nextKeyboard, Boolean isTournamentCodeReq,
                   String... inputPrompts){
        this.key = key;
        this.requiredInputs = requiredInputs;
        this.nextKeyboard = nextKeyboard;
        this.isTournamentCodeReq = isTournamentCodeReq;
        this.inputPrompts = inputPrompts == null ? new String[0] : inputPrompts;
        if(requiredInputs!= inputPrompts.length){
            throw new Exception("the amount of prompts is not enough " + this.getClass());
        }
    }

    public String getInputPrompt(int stepIndex) {
        if (inputPrompts.length == 0) return "";
        return stepIndex < inputPrompts.length ? inputPrompts[stepIndex] : inputPrompts[inputPrompts.length - 1];
    }

    public boolean needsInput() { return requiredInputs > 0; }

    public abstract void execute(BotFacade bot, Update update);

    /**
     * Handle custom callbacks for this command.
     * Override this method if your command needs to handle its own callback queries.
     * @param bot The bot facade
     * @param update The callback query update
     * @param callbackData The full callback data string
     * @return true if the callback was handled, false otherwise
     */
    public boolean handleCallback(BotFacade bot, Update update, String callbackData) {
        return false; // Default: command doesn't handle callbacks
    }

    // Helper methods to handle both message and callback query updates
    protected boolean isMessage(Update update) {
        return update.hasMessage() && update.getMessage() != null;
    }

    protected boolean isCallbackQuery(Update update) {
        return update.hasCallbackQuery() && update.getCallbackQuery() != null;
    }

    protected long getUserId(Update update) {
        if (isMessage(update)) {
            return update.getMessage().getFrom().getId();
        } else if (isCallbackQuery(update)) {
            return update.getCallbackQuery().getFrom().getId();
        }
        throw new IllegalArgumentException("Update has neither message nor callback query");
    }

    protected long getChatId(Update update) {
        if (isMessage(update)) {
            return update.getMessage().getChatId();
        } else if (isCallbackQuery(update)) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        throw new IllegalArgumentException("Update has neither message nor callback query");
    }

    protected String getUsername(Update update) {
        if (isMessage(update)) {
            return update.getMessage().getFrom().getUserName();
        } else if (isCallbackQuery(update)) {
            return update.getCallbackQuery().getFrom().getUserName();
        }
        throw new IllegalArgumentException("Update has neither message nor callback query");
    }

    protected String getText(Update update) {
        if (isMessage(update)) {
            return update.getMessage().getText();
        } else if (isCallbackQuery(update)) {
            return update.getCallbackQuery().getData();
        }
        throw new IllegalArgumentException("Update has neither message nor callback query");
    }
    protected long getMessageId(Update update) {
        if (isMessage(update)) {
            return update.getMessage().getMessageId();
        } else if (isCallbackQuery(update)) {
            return update.getCallbackQuery().getMessage().getMessageId();
        }
        throw new IllegalArgumentException("Update has neither message nor callback query");
    }
}
