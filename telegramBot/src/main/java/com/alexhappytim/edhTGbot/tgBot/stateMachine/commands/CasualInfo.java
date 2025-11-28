package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CasualInfo extends Command {

    public CasualInfo() {
        super("casualinfo", 1, "main", "Введите ID казуал турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().debug("User {} requesting info for tournament {}", 
                update.getMessage().getFrom().getUserName(), tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId, String.class);
            JsonNode tournament = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().debug("Retrieved info for tournament {}", tournamentId);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Турнир: ").append(tournament.get("name").asText()).append("\n");
            sb.append("Владелец: ").append(tournament.get("owner").get("displayName").asText()).append("\n");
            sb.append("Игроков: ").append(tournament.get("users").size()).append("\n");
            sb.append("Готовых: ").append(tournament.get("readyUsers").size()).append("\n");
            sb.append("Групп: ").append(tournament.get("groups").size()).append("\n");
            bot.sendMessage(chatId, sb.toString());
        } catch (Exception e) {
            bot.getLogger().error("Get info failed for tournament {}: {}", tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка получения информации: " + e.getMessage());
        }
    }
}
