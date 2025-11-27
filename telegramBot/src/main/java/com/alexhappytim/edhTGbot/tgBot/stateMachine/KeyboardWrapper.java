package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Getter
@AllArgsConstructor
public class KeyboardWrapper {
    private String text;
    private InlineKeyboardMarkup keyboard;
}
