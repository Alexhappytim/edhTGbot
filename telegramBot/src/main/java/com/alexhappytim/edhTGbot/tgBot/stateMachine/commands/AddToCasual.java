package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AddToCasual extends Command {

    public AddToCasual() {
        super("add_to_casual", 2, "tournament_admin_casual",
              "Введите ID казуал турнира", 
              "Введите имя игрока");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        String displayName = bot.getSession(userId).getInputs().get(1);
        
        bot.getLogger().info("User {} adding temporary user {} to casual tournament {}", 
                update.getMessage().getFrom().getUserName(), displayName, tournamentId);
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(userId, displayName, true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + "/join", 
                    entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().info("Temporary user {} added to casual tournament {}", displayName, tournamentId);
            bot.sendMessage(chatId, "Добавлен временный игрок: " + node.get("displayName").asText());
        } catch (Exception e) {
            bot.getLogger().error("Add to casual tournament failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка добавления: " + e.getMessage());
        }
    }
}
