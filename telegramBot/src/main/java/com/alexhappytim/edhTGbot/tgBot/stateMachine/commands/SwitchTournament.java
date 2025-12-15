package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.Keyboards;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.KeyboardWrapper;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SwitchTournament extends Command {

    public SwitchTournament() {
        super("switch_tournament", 0, "main", false);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        int messageId = -1;
        
        // Check if this is a callback query (edit message) or a direct command (send message)
        if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            messageId = update.getCallbackQuery().getMessage().getMessageId();
        }
        
        Map<String, String> tournaments = bot.getSession(userId).getJoinedTournaments();
        
        if (tournaments == null || tournaments.isEmpty()) {
            bot.sendMessage(chatId, "❌ Вы не присоединены ни к одному турниру");
            return;
        }
        
        // Build keyboard with tournament options
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : tournaments.entrySet()) {
            String tournamentId = entry.getKey();
            String tournamentType = entry.getValue();
            String typeDisplay = tournamentType.equalsIgnoreCase("CASUAL") ? "казуальный" : "классический";
            
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(InlineKeyboardButton.builder()
                    .text(String.format("%s (%s)", tournamentId, typeDisplay))
                    .callbackData("switch_tournament:" + tournamentId)
                    .build());
            rows.add(row);
        }
        
        // Add manual input button
        InlineKeyboardRow manualRow = new InlineKeyboardRow();
        manualRow.add(InlineKeyboardButton.builder()
                .text("Ввести ID и тип")
                .callbackData("cmd:switch_tournament_manual")
                .build());
        rows.add(manualRow);

        // Add back button
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder()
                .text("« Назад")
                .callbackData("kbd:main")
                .build());
        rows.add(backRow);
        
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
        
        // Use editMessage if callback, sendMessage if direct command
        if (messageId > 0) {
            bot.editMessage(chatId, messageId, "Выберите турнир для переключения:", markup);
        } else {
            bot.sendMessage(chatId, "Выберите турнир для переключения:", markup);
        }
    }
    @Override
    public boolean handleCallback(BotFacade bot, Update update, String callbackData) {
        if (callbackData == null || !callbackData.startsWith("switch_tournament:")) {
            return false;
        }

        long userId = update.getCallbackQuery().getFrom().getId();
        long chatId = getChatId(update);
        String tournamentId = callbackData.substring("switch_tournament:".length());

        Map<String, String> tournaments = bot.getSession(userId).getJoinedTournaments();

        if (tournaments == null || !tournaments.containsKey(tournamentId)) {
            bot.sendMessage(chatId, "❌ Турнир не найден в ваших присоединениях");
            return true;
        }

        // Switch tournament and persist session
        bot.getSession(userId).setCurrentTournament(tournamentId);
        bot.setSession(userId, bot.getSession(userId));

        // Show main keyboard after switching
        KeyboardWrapper main = Keyboards.MAIN.getKeyboard();
        bot.editMessage(chatId, update.getCallbackQuery().getMessage().getMessageId(),
            main.getText(), main.getKeyboard());

        bot.getLogger().info("User {} switched to tournament {} and returned to main keyboard", userId, tournamentId);
        return true;
    }
}
