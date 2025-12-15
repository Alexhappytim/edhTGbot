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
        super("create_casual", 1, "tournament_admin_casual", false,"Введите название казуал турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String name = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().info("User {} creating casual tournament: {}", username, name);
        try {
            CreateTournamentRequest request = new CreateTournamentRequest(name, 0, userId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual", entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());
            String tournamentId = node.get("id").asText();
            
            // Save tournament ID and type to session
            bot.getSession(userId).setTournamentId(tournamentId);
            bot.getSession(userId).setTournamentType("CASUAL");
            
            bot.getLogger().info("Casual tournament created with ID: {}", tournamentId);
            bot.sendMessage(chatId, "✅ Казуал турнир создан! ID: " + tournamentId);
        } catch (Exception e) {
            bot.getLogger().error("Casual tournament creation failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка создания: " + e.getMessage());
        }
    }
}
