package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registry for keyboards. Each keyboard is produced on demand via a Supplier
 * so dynamic command labels are always fresh.
 */

//TODO: а что если сделать это энамчиком?
public class KeyboardRegistry {
    private final Map<String, KeyboardWrapper> keyboards = new ConcurrentHashMap<>();

    public KeyboardRegistry() {
        keyboards.put("main", getMainKeyboard());
        keyboards.put("tournament_creating", getTournamentCreatingKeyboard());
    }



    public KeyboardWrapper getKeyboard(String key) {
        return keyboards.get(key);
    }
    private KeyboardWrapper getMainKeyboard() {
        // Group: Registration
        InlineKeyboardRow registration = new InlineKeyboardRow();
        registration.add(InlineKeyboardButton.builder().text("Зарегистрироваться").callbackData("cmd:register").build());

        // Group: Tournament Selection
        InlineKeyboardRow createTournament = new InlineKeyboardRow();
        createTournament.add(InlineKeyboardButton.builder().text("Создать турнир").callbackData("kbd:tournament_creating").build());

        InlineKeyboardRow joinTournament = new InlineKeyboardRow();
        joinTournament.add(InlineKeyboardButton.builder().text("Присоединиться к турниру").callbackData("kbd:tournament_join").build());
        InlineKeyboardRow adminTournament = new InlineKeyboardRow();
        adminTournament.add(InlineKeyboardButton.builder().text("Управление моими турнирами").callbackData("kbd:tournament_admin").build());
        InlineKeyboardRow infoTournament = new InlineKeyboardRow();
        infoTournament.add(InlineKeyboardButton.builder().text("Посмотреть инфо о турнире").callbackData("kbd:tournament_info").build());

        return new KeyboardWrapper(
                "Привет! Выбери действие:",
                InlineKeyboardMarkup.builder()
                .keyboard(java.util.List.of(
                        registration,
                        createTournament,
                        joinTournament,
                        adminTournament,
                        infoTournament

                ))
                .build());
    }
    private KeyboardWrapper getTournamentCreatingKeyboard(){
        InlineKeyboardRow tournamentType = new InlineKeyboardRow();
        tournamentType.add(InlineKeyboardButton.builder().text("Классический").callbackData("cmd:tournament_creating").build());
        tournamentType.add(InlineKeyboardButton.builder().text("Казуальный EDH").callbackData("cmd:tournament_creating").build());
        InlineKeyboardRow backToMain = new InlineKeyboardRow();
        backToMain.add(InlineKeyboardButton.builder().text("Назад").callbackData("kbd:main").build());
        return new KeyboardWrapper(
                "Выбери вариант турнира:",
                InlineKeyboardMarkup.builder()
                        .keyboard(java.util.List.of(
                                tournamentType,
                                backToMain

                        ))
                        .build());
    }
}
