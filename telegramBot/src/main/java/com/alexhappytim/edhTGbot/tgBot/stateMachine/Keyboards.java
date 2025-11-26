package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

public class Keyboards {
    public InlineKeyboardMarkup getMainKeyboard() {
        // Group: Registration
        InlineKeyboardRow registration = new InlineKeyboardRow();
        registration.add(InlineKeyboardButton.builder().text("Зарегистрироваться").callbackData("cmd:register").build());
//
//        // Group: Tournament Selection
        InlineKeyboardRow createTournament = new InlineKeyboardRow();
        createTournament.add(InlineKeyboardButton.builder().text("Создать турнир").callbackData("type:tournament_creating").build());

        InlineKeyboardRow joinTournament = new InlineKeyboardRow();
        joinTournament.add(InlineKeyboardButton.builder().text("Присоединиться к турниру").callbackData("type:tournament_join").build());
        InlineKeyboardRow adminTournament = new InlineKeyboardRow();
        adminTournament.add(InlineKeyboardButton.builder().text("Управление моими турнирами").callbackData("type:tournament_admin").build());
        InlineKeyboardRow infoTournament = new InlineKeyboardRow();
        infoTournament.add(InlineKeyboardButton.builder().text("Посмотреть инфо о турнире").callbackData("type:tournament_admin").build());

        return InlineKeyboardMarkup.builder()
                .keyboard(java.util.List.of(
                        registration,
                        createTournament,
                        joinTournament,
                        adminTournament,
                        infoTournament

                ))
                .build();
    }
}
