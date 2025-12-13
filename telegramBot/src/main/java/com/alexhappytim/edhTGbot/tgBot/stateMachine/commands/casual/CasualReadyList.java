package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.TournamentIdInputStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CasualReadyList extends Command {

    public CasualReadyList() {
        super("casual_ready_list", "tournament_admin_casual", 
              new TournamentIdInputStrategy("Введите ID казуал турнира"));
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().debug("User {} requesting ready list for tournament {}", 
                username, tournamentId);
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
