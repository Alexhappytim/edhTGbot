package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CasualReadyList extends Command {

    public CasualReadyList() {
        super("casualreadylist", 1, "main", "Введите ID казуал турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().debug("User {} requesting ready list for tournament {}", 
                update.getMessage().getFrom().getUserName(), tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + "/ready-users", String.class);
            JsonNode users = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().debug("Retrieved {} ready users for tournament {}", users.size(), tournamentId);
            
            StringBuilder sb = new StringBuilder("Готовые игроки:\n");
            for (JsonNode user : users) {
                sb.append("- ").append(user.get("displayName").asText()).append("\n");
            }
            bot.sendMessage(chatId, sb.toString());
        } catch (Exception e) {
            bot.getLogger().error("Get ready users failed for tournament {}: {}", tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка получения списка: " + e.getMessage());
        }
    }
}
