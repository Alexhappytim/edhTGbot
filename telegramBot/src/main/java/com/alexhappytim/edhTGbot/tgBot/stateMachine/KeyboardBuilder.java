package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилитный класс для простого создания клавиатур.
 * Использование:
 * - KeyboardBuilder.build("Text", "Button1:callback1", "Button2:callback2")
 * - KeyboardBuilder.build("Text", new String[][]{{"Btn1:cb1", "Btn2:cb2"}, {"Btn3:cb3"}})
 */
public class KeyboardBuilder {
    
    /**
     * Создает клавиатуру с одной кнопкой в каждой строке.
     * Формат кнопок: "текст:callbackData"
     */
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

    /**
     * Создает клавиатуру с несколькими кнопками в строке.
     * Каждый массив = одна строка кнопок.
     * Формат: new String[][]{{"текст1:cb1", "текст2:cb2"}, {"текст3:cb3"}}
     */
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
