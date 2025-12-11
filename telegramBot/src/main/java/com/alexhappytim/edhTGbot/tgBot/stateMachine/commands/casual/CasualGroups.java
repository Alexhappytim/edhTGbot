package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CasualGroups extends Command {

    public CasualGroups() {
        super("casual_groups", 1, "tournament_admin_casual", "Введите ID казуал турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().debug("User {} requesting groups for tournament {}", 
                update.getMessage().getFrom().getUserName(), tournamentId);
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
