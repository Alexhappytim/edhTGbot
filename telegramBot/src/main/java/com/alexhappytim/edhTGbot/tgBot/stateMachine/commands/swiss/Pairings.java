package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

public class Pairings extends Command {

    public Pairings() {
        super("pairings", 0, "tournament_admin", false);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        String username = getUsername(update);
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        bot.getLogger().info("User {} viewing pairings for tournament {}", username, tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/swiss/pairings",
                    String.class);
            
            JsonNode pairings = bot.getObjectMapper().readTree(response.getBody());
            
            if (pairings.size() == 0) {
                bot.sendMessage(chatId, "ℹ️ Нет активных пар. Турнир еще не начался или раунд завершен.");
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("📋 Текущие пары\n");
            sb.append("```───────────────────────────────────────────────────\n");
            sb.append(String.format("%-5s %-35.35s %5s%n", "Матч", "Игроки", "Счёт"));
            sb.append("───────────────────────────────────────────────────\n");
            
            int matchNum = 1;
            for (JsonNode pairing : pairings) {
                Long matchId = pairing.get("matchId").asLong();
                String playerATag = pairing.get("playerA").isNull() ? null : pairing.get("playerA").asText();
                String playerADisplay = pairing.get("playerADisplayName").isNull() ? null : pairing.get("playerADisplayName").asText();
                String playerBTag = pairing.get("playerB").isNull() ? null : pairing.get("playerB").asText();
                String playerBDisplay = pairing.get("playerBDisplayName").isNull() ? null : pairing.get("playerBDisplayName").asText();
                Integer scoreA = pairing.get("scoreA").isNull() ? null : pairing.get("scoreA").asInt();
                Integer scoreB = pairing.get("scoreB").isNull() ? null : pairing.get("scoreB").asInt();
                boolean completed = pairing.get("completed").asBoolean();

                String playerAName;
                if (playerATag != null && playerADisplay != null) {
                    if (playerATag.isEmpty()) {
                        playerAName = playerADisplay;
                    } else {
                        playerAName = playerADisplay + " (@" + playerATag + ")";
                    }
                } else {
                    playerAName = "bye";
                }
                
                String playerBName;
                if (playerBTag != null && playerBDisplay != null) {
                    if (playerBTag.isEmpty()) {
                        playerBName = playerBDisplay;
                    } else {
                        playerBName = playerBDisplay + " (@" + playerBTag + ")";
                    }
                } else {
                    playerBName = "bye";
                }
                
                String matchup = playerAName + " vs " + playerBName;
                
                String score;
                if (scoreA != null && scoreB != null) {
                    score = scoreA + "-" + scoreB + (completed ? "✅" : "⏳");
                } else {
                    score = "-";
                }
                
                sb.append(String.format("%-5d %-35.35s %5s%n", matchNum, matchup, score));
                matchNum++;
            }
            
            sb.append("───────────────────────────────────────────────────```");

            InlineKeyboardRow backRow = new InlineKeyboardRow();
            backRow.add(InlineKeyboardButton.builder()
                    .text("« Назад")
                    .callbackData("kbd:tournament_admin")
                    .build());
            InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                    .keyboard(java.util.List.of(backRow))
                    .build();
            if (isCallbackQuery(update)) {
                int messageId = update.getCallbackQuery().getMessage().getMessageId();
                bot.editMessage(chatId, messageId, sb.toString(), kb);
            } else {
                bot.sendMessage(chatId, sb.toString(), kb);
            }
        } catch (Exception e) {
            bot.getLogger().error("Get pairings failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка получения пар: " + e.getMessage());
        }
    }
}
