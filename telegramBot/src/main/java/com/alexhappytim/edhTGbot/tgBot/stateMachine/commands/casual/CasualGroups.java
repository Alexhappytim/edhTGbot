package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.TournamentIdInputStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CasualGroups extends Command {

    public CasualGroups() {
        super("casual_groups", "tournament_admin_casual", 
              new TournamentIdInputStrategy("Введите ID казуал турнира"));
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().debug("User {} requesting groups for tournament {}", 
                username, tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + "/groups", String.class);
            JsonNode groups = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().debug("Retrieved {} groups for tournament {}", groups.size(), tournamentId);
            
            StringBuilder sb = new StringBuilder("Текущие группы:\n");
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
            bot.getLogger().error("Get groups failed for tournament {}: {}", tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка получения групп: " + e.getMessage());
        }
    }
}
