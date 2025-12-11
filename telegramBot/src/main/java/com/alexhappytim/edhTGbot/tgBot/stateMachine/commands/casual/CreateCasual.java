package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.CreateTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CreateCasual extends Command {

    public CreateCasual() {
        super("create_casual", 1, "tournament_admin_casual", "Введите название казуал турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String name = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().info("User {} creating casual tournament: {}", 
                update.getMessage().getFrom().getUserName(), name);
        try {
            CreateTournamentRequest request = new CreateTournamentRequest(name, 0, userId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual", entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().info("Casual tournament created with ID: {}", node.get("id").asText());
            bot.sendMessage(chatId, "Казуал турнир создан! ID: " + node.get("id").asText());
        } catch (Exception e) {
            bot.getLogger().error("Casual tournament creation failed for user {}: {}", 
                    update.getMessage().getFrom().getUserName(), e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка создания: " + e.getMessage());
        }
    }
}
