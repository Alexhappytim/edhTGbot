package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.CreateTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CreateTournament extends Command {

    public CreateTournament() {
        super("create_tournament", 2, "main",
              "Введите название турнира", 
              "Введите максимальное количество игроков");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String name = bot.getSession(userId).getInputs().get(0);
        String maxPlayersStr = bot.getSession(userId).getInputs().get(1);
        
        bot.getLogger().info("User {} creating tournament: name={}, maxPlayers={}", 
                update.getMessage().getFrom().getUserName(), name, maxPlayersStr);
        try {
            int maxPlayers = Integer.parseInt(maxPlayersStr);
            CreateTournamentRequest request = new CreateTournamentRequest(name, maxPlayers, userId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournaments", entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().info("Tournament created successfully with ID: {}", node.get("id").asText());
            bot.sendMessage(chatId, "Турнир создан! ID: " + node.get("id").asText());
        } catch (NumberFormatException e) {
            bot.getLogger().error("Invalid maxPlayers value: {}", maxPlayersStr);
            bot.sendMessage(chatId, "Ошибка: максимальное количество игроков должно быть числом");
        } catch (Exception e) {
            bot.getLogger().error("Tournament creation failed for user {}: {}", 
                    update.getMessage().getFrom().getUserName(), e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка создания турнира: " + e.getMessage());
        }
    }
}
