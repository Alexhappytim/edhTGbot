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
    private final int requiredInputs;  // number of inputs to collect
    private final String[] inputPrompts;  // prompts for each input step
    private final String nextKeyboard; // keyboard to show after execution

    public Command(String key,
                   int requiredInputs,
                   String nextKeyboard,
                   String... inputPrompts) {
        this.key = key;
        this.requiredInputs = requiredInputs;
        this.nextKeyboard = nextKeyboard;
        this.inputPrompts = inputPrompts == null ? new String[0] : inputPrompts;
    }

    public String getInputPrompt(int stepIndex) {
        if (inputPrompts.length == 0) return "";
        return stepIndex < inputPrompts.length ? inputPrompts[stepIndex] : inputPrompts[inputPrompts.length - 1];
    }

    public int getRequiredInputs() { return requiredInputs; }

    public boolean needsInput() { return requiredInputs > 0; }

    public abstract void execute(BotFacade bot, Update update);
}
