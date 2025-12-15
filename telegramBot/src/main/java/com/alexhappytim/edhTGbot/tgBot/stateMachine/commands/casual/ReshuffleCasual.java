package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ReshuffleCasual extends Command {

    public ReshuffleCasual() {
        super("reshuffle_casual",0, "tournament_admin_casual", true);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(adminId).getTournamentId();
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        bot.getLogger().info("Admin {} reshuffling ready users in tournament {}", 
                username, tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/reshuffle?adminId=" + adminId;
            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(url, null, String.class);
            JsonNode groups = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().info("Reshuffle completed for tournament {}, {} new groups", 
                    tournamentId, groups.size());
            
            StringBuilder sb = new StringBuilder("🎲 Перетасовка! Новые группы:\n");
            for (JsonNode group : groups) {
                sb.append("Группа ").append(group.get("groupNumber").asInt()).append(": ");
                for (JsonNode player : group.get("players")) {
                    sb.append(player.get("displayName").asText()).append(", ");
                }
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }
            bot.sendMessage(chatId, sb.toString());
        } catch (Exception e) {
            bot.getLogger().error("Reshuffle failed for tournament {}: {}", tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка перетасовки: " + e.getMessage());
        }
    }
}
