package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.SubmitMatchResultRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

public class EditResult extends Command {

    public EditResult() {
        super("edit_result", 0, "tournament_admin", true);
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
        
        bot.getLogger().info("Admin {} requesting matches to edit in tournament {}", username, tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/pairings",
                    String.class);

            JsonNode pairings = bot.getObjectMapper().readTree(response.getBody());
            if (pairings.size() == 0) {
                bot.sendMessage(chatId, "ℹ️ Нет активных матчей для редактирования");
                return;
            }

            InlineKeyboardMarkup markup = buildMatchesKeyboard(pairings);
            bot.sendMessage(chatId, "Выберите матч для редактирования", markup);
        } catch (Exception e) {
            bot.getLogger().error("Edit result fetch failed for admin {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка загрузки матчей: " + e.getMessage());
        }
    }

    @Override
    public boolean handleCallback(BotFacade bot, Update update, String callbackData) {
        if (callbackData == null) return false;

        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getTournamentId();

        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return true;
        }

        try {
            if (callbackData.startsWith("edit_match:")) {
                // edit_match:<matchId>
                String[] parts = callbackData.split(":");
                if (parts.length != 2) {
                    bot.sendMessage(chatId, "❌ Неверные данные матча");
                    return true;
                }
                long matchId = Long.parseLong(parts[1]);
                InlineKeyboardMarkup kb = buildScoreKeyboard(matchId);
                int messageId = update.getCallbackQuery().getMessage().getMessageId();
                bot.editMessage(chatId, messageId, "Выберите новый счет (BO3)", kb);
                return true;
            }

            if (callbackData.equals("edit_back_to_matches")) {
                // Return to match list
                try {
                    ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                            bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/pairings",
                            String.class);
                    JsonNode pairings = bot.getObjectMapper().readTree(response.getBody());
                    if (pairings.size() == 0) {
                        bot.sendMessage(chatId, "ℹ️ Нет активных матчей для редактирования");
                        return true;
                    }
                    InlineKeyboardMarkup markup = buildMatchesKeyboard(pairings);
                    int messageId = update.getCallbackQuery().getMessage().getMessageId();
                    bot.editMessage(chatId, messageId, "Выберите матч для редактирования", markup);
                } catch (Exception e) {
                    bot.sendMessage(chatId, "❌ Ошибка загрузки матчей: " + e.getMessage());
                }
                return true;
            }

            if (callbackData.startsWith("edit_score:")) {
                // edit_score:<matchId>:<a>:<b>
                String[] parts = callbackData.split(":");
                if (parts.length != 4) {
                    bot.sendMessage(chatId, "❌ Неверные данные счета");
                    return true;
                }
                long matchId = Long.parseLong(parts[1]);
                int scoreA = Integer.parseInt(parts[2]);
                int scoreB = Integer.parseInt(parts[3]);

                if (!isValidBo3(scoreA, scoreB)) {
                    bot.sendMessage(chatId, "❌ Неверный счет. Допустимые для BO3: 0-0, 1-0, 0-1, 1-1, 2-0, 0-2, 2-1, 1-2");
                    return true;
                }

                SubmitMatchResultRequest request = new SubmitMatchResultRequest();
                request.setMatchId(matchId);
                request.setScoreA(scoreA);
                request.setScoreB(scoreB);
                request.setAdminId(userId);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<SubmitMatchResultRequest> entity = new HttpEntity<>(request, headers);

                bot.getRestTemplate().postForEntity(
                        bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/edit-result",
                        entity, Void.class);

                bot.getLogger().info("Match result edited by admin {}: match {} -> {}-{}", username, matchId, scoreA, scoreB);
                
                // Return to match list
                ResponseEntity<String> pairingsResponse = bot.getRestTemplate().getForEntity(
                        bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/pairings",
                        String.class);
                JsonNode pairings = bot.getObjectMapper().readTree(pairingsResponse.getBody());
                if (pairings.size() == 0) {
                    bot.sendMessage(chatId, "✅ Результат изменён! Нет активных матчей.");
                    return true;
                }
                InlineKeyboardMarkup markup = buildMatchesKeyboard(pairings);
                bot.editMessage(chatId,update.getCallbackQuery().getMessage().getMessageId(), "✅ Результат изменён! Выберите следующий матч", markup);
                return true;
            }
        } catch (Exception e) {
            bot.getLogger().error("Edit result callback failed for admin {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка редактирования результата: " + e.getMessage());
            return true;
        }

        return false;
    }

    private InlineKeyboardMarkup buildMatchesKeyboard(JsonNode pairings) {
        java.util.List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
        
        // Sort pairings by matchId to maintain consistent order
        java.util.List<JsonNode> sortedPairings = new java.util.ArrayList<>();
        pairings.forEach(sortedPairings::add);
        sortedPairings.sort((a, b) -> Long.compare(
            a.get("matchId").asLong(),
            b.get("matchId").asLong()
        ));
        
        int idx = 1;
        for (JsonNode pairing : sortedPairings) {
            long matchId = pairing.get("matchId").asLong();
            String pA = pairing.get("playerADisplayName").asText();
            String pATag = pairing.get("playerA").isNull() ? "" : pairing.get("playerA").asText();
            String pB = pairing.get("playerBDisplayName").isNull() ? "BYE" : pairing.get("playerBDisplayName").asText();
            String pBTag = pairing.get("playerB").isNull() ? "" : pairing.get("playerB").asText();
            String labelA = pATag == null || pATag.isEmpty() ? pA : pA + " (@" + pATag + ")";
            String labelB = pBTag == null || pBTag.isEmpty() ? pB : pB + " (@" + pBTag + ")";
            Integer scoreA = pairing.get("scoreA").isNull() ? null : pairing.get("scoreA").asInt();
            Integer scoreB = pairing.get("scoreB").isNull() ? null : pairing.get("scoreB").asInt();
            String score = (scoreA != null && scoreB != null) ? scoreA + "-" + scoreB : "-";

            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(InlineKeyboardButton.builder()
                    .text(String.format("%d) %s vs %s [%s]", idx, labelA, labelB, score))
                    .callbackData("edit_match:" + matchId)
                    .build());
            rows.add(row);
            idx++;
        }
        // Add back button
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder()
                .text("« Назад")
                .callbackData("kbd:tournament_admin")
                .build());
        rows.add(backRow);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup buildScoreKeyboard(long matchId) {
        java.util.List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
        rows.add(row("2-0", matchId, 2, 0, "2-1", matchId, 2, 1));
        rows.add(row("1-2", matchId, 1, 2, "0-2", matchId, 0, 2));
        rows.add(row("1-1", matchId, 1, 1, "1-0", matchId, 1, 0));
        rows.add(row("0-1", matchId, 0, 1, "0-0", matchId, 0, 0));
        // Add back button
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder()
                .text("« Назад")
                .callbackData("edit_back_to_matches")
                .build());
        rows.add(backRow);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardRow row(String label1, long matchId1, int a1, int b1,
                                  String label2, long matchId2, int a2, int b2) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder()
                .text(label1)
                .callbackData("edit_score:" + matchId1 + ":" + a1 + ":" + b1)
                .build());
        row.add(InlineKeyboardButton.builder()
                .text(label2)
                .callbackData("edit_score:" + matchId2 + ":" + a2 + ":" + b2)
                .build());
        return row;
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
