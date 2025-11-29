package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class StartCasual extends Command {

    public StartCasual() {
        super("start_casual", 1, "tournament_admin_casual", "Введите ID казуал турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().info("User {} starting round for casual tournament {}", 
                update.getMessage().getFrom().getUserName(), tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + "/start-round",
                    null, String.class);
            JsonNode groups = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().info("Round started for tournament {}, {} groups created", tournamentId, groups.size());
            
            StringBuilder sb = new StringBuilder("Раунд начался! Группы:\n");
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
            bot.getLogger().error("Start round failed for tournament {}: {}", tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка начала раунда: " + e.getMessage());
        }
    }
}
