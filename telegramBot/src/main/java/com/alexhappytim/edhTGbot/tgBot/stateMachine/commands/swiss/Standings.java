package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

public class Standings extends Command {

    public Standings() {
        super("standings", 0, "tournament_admin", true);
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
        
        bot.getLogger().info("User {} requesting standings for tournament {}", 
                username, tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/standings", String.class);
            bot.getLogger().debug("Standings retrieved for tournament {}", tournamentId);
            
            // Parse and format standings nicely
            JsonNode standings = bot.getObjectMapper().readTree(response.getBody());
            
            // Convert to list and sort by points (desc) then tiebreaker (desc)
            java.util.List<JsonNode> standingsList = new java.util.ArrayList<>();
            standings.forEach(standingsList::add);
            standingsList.sort(java.util.Comparator
                    .comparingInt((JsonNode s) -> s.get("points").asInt()).reversed()
                    .thenComparingInt((JsonNode s) -> s.get("tieBreaker").asInt()).reversed());
            StringBuilder sb = new StringBuilder();

            sb.append("📊 Турнирная таблица\n");
            sb.append("```");
            sb.append("───────────────────────────────────────────────────────────────────────\n");
            sb.append(String.format("%5s %-30.30s %4s %7s %6s %6s%n",
                    "Место", "Имя", "Очки", "OMW%", "GW%", "OGW%"));
            sb.append("───────────────────────────────────────────────────────────────────────\n");

            int rank = 1;
            for (JsonNode standing : standingsList.reversed()) {
                String displayName = standing.get("displayName").asText();
                String userTag = standing.get("userTag").asText();
                int points = standing.get("points").asInt();
                double omwPercentage = standing.has("omwPercentage") ? standing.get("omwPercentage").asDouble() : 0.0;
                double gwPercentage = standing.has("gwPercentage") ? standing.get("gwPercentage").asDouble() : 0.0;
                double ogwPercentage = standing.has("ogwPercentage") ? standing.get("ogwPercentage").asDouble() : 0.0;

                String fullName;
                if (userTag == null || userTag.isEmpty()) {
                    fullName = displayName;
                } else {
                    fullName = displayName + " (@" + userTag + ")";
                }

                sb.append(String.format("%-5d %-30.30s %4d %7.2f%% %6.2f%% %6.2f%%%n",
                        rank, fullName, points, omwPercentage * 100, gwPercentage * 100, ogwPercentage * 100));
                rank++;
            }

            sb.append("───────────────────────────────────────────────────────────────────────\n");
            sb.append("OMW% = Opponents Match Win %\n");
            sb.append("GW%  = Game Win %\n");
            sb.append("OGW% = Opponents Game Win %```");

            // Create back button keyboard
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
            bot.getLogger().error("Failed to get standings for tournament {}: {}", tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка получения таблицы: " + e.getMessage());
        }
    }
}
