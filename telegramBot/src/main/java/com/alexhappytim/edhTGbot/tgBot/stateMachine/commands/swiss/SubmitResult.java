package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.KeyboardBuilder;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.KeyboardWrapper;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.SubmitMatchResultRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class SubmitResult extends Command {

    public SubmitResult() {
        super("submit_result", 0, "tournament_admin", true);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        // Show inline keyboard with all valid BO3 results
        String[][] buttons = new String[][]{
                {"2-0:submit_score:2:0", "2-1:submit_score:2:1"},
                {"1-2:submit_score:1:2", "0-2:submit_score:0:2"},
                {"1-1:submit_score:1:1", "1-0:submit_score:1:0"},
                {"0-1:submit_score:0:1", "0-0:submit_score:0:0"},
                {"« Назад:kbd:tournament_admin"}
        };

        KeyboardWrapper kb = KeyboardBuilder.build("Выберите счет матча (BO3)", buttons);
        bot.editMessage(chatId,getMessageId(update), kb.getText(), kb.getKeyboard());
    }

    @Override
    public boolean handleCallback(BotFacade bot, Update update, String callbackData) {
        if (callbackData == null) {
            return false;
        }
        
        if (!callbackData.startsWith("submit_score:")) {
            return false;
        }

        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getTournamentId();

        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return true;
        }

        String[] parts = callbackData.split(":");
        if (parts.length != 3) {
            bot.sendMessage(chatId, "❌ Неверные данные счета");
            return true;
        }

        try {
            int scoreA = Integer.parseInt(parts[1]);
            int scoreB = Integer.parseInt(parts[2]);

            if (!isValidBo3(scoreA, scoreB)) {
                bot.sendMessage(chatId, "❌ Неверный счет. Допустимые для BO3: 0-0, 1-0, 0-1, 1-1, 2-0, 0-2, 2-1, 1-2");
                return true;
            }

            SubmitMatchResultRequest request = new SubmitMatchResultRequest();
            request.setScoreA(scoreA);
            request.setScoreB(scoreB);
            request.setSubmitterTelegramId(userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SubmitMatchResultRequest> entity = new HttpEntity<>(request, headers);

            bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/swiss/submit",
                    entity, Void.class);

            bot.getLogger().info("Match result submitted: {} vs {} for user {}", scoreA, scoreB, username);
            

        } catch (Exception e) {
            bot.getLogger().error("Submit result failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка отправки результата: " + e.getMessage());
        }
        return true;
    }

    private boolean isValidBo3(int scoreA, int scoreB) {
        return (scoreA == 0 && scoreB == 0) ||
                (scoreA == 1 && scoreB == 0) ||
                (scoreA == 0 && scoreB == 1) ||
                (scoreA == 1 && scoreB == 1) ||
                (scoreA == 2 && scoreB == 0) ||
                (scoreA == 0 && scoreB == 2) ||
                (scoreA == 2 && scoreB == 1) ||
                (scoreA == 1 && scoreB == 2);
    }
}
