package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CasualInfo extends Command {

    public CasualInfo() {
        super("casual_info", 0, "tournament_admin_casual", true);
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
        
        bot.getLogger().debug("User {} requesting info for tournament {}", 
                username, tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId, String.class);
            JsonNode tournament = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().debug("Retrieved info for tournament {}", tournamentId);
            
            StringBuilder sb = new StringBuilder();
            sb.append("📋 Турнир: ").append(tournament.get("name").asText()).append("\n");
            sb.append("👑 Владелец: ").append(tournament.get("owner").get("displayName").asText()).append("\n");
            sb.append("\n👥 Игроки:\n");
            
            JsonNode users = tournament.get("users");
            JsonNode readyUsers = tournament.get("readyUsers");
            java.util.Set<Long> readyIds = new java.util.HashSet<>();
            for (JsonNode ready : readyUsers) {
                readyIds.add(ready.get("id").asLong());
            }
            
            int position = 1;
            for (JsonNode user : users) {
                String readyMark = readyIds.contains(user.get("id").asLong()) ? "✅" : "⏳";
                sb.append(String.format("%d. %s %s\n", position, user.get("displayName").asText(), readyMark));
                position++;
            }
            
            sb.append("\n📊 Статистика:\n");
            sb.append("Всего игроков: ").append(users.size()).append("\n");
            sb.append("Готовых: ").append(readyUsers.size()).append("\n");
            sb.append("Групп: ").append(tournament.get("groups").size()).append("\n");
            bot.sendMessage(chatId, sb.toString());
        } catch (Exception e) {
            bot.getLogger().error("Get info failed for tournament {}: {}", tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка получения информации: " + e.getMessage());
        }
    }
}
