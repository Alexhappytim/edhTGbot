package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;


public class KeyboardBuilder {

    public static KeyboardWrapper build(String text, String... buttons) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (String button : buttons) {
            String[] parts = button.split(":", 2);
            if (parts.length == 2) {
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(InlineKeyboardButton.builder()
                        .text(parts[0])
                        .callbackData(parts[1])
                        .build());
                rows.add(row);
            }
        }
        return new KeyboardWrapper(text, InlineKeyboardMarkup.builder().keyboard(rows).build());
    }

    public static KeyboardWrapper build(String text, String[][] buttonRows) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (String[] buttonRow : buttonRows) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            for (String button : buttonRow) {
                String[] parts = button.split(":", 2);
                if (parts.length == 2) {
                    row.add(InlineKeyboardButton.builder()
                            .text(parts[0])
                            .callbackData(parts[1])
                            .build());
                }
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        return new KeyboardWrapper(text, InlineKeyboardMarkup.builder().keyboard(rows).build());
    }
}
