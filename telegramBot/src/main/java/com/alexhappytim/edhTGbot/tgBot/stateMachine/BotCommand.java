package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import lombok.Getter;

import java.util.function.Consumer;

@Getter
public class BotCommand {
    private final String key; // e.g. createtournament
    private final String label; // Display text
    private final CommandGroup group;
    private final boolean needsInput;
    private final String inputPrompt;
    private final Consumer<String> executor; // Accepts collected input or null

    public BotCommand(String key, String label, CommandGroup group, boolean needsInput, String inputPrompt, Consumer<String> executor) {
        this.key = key;
        this.label = label;
        this.group = group;
        this.needsInput = needsInput;
        this.inputPrompt = inputPrompt;
        this.executor = executor;
    }

}