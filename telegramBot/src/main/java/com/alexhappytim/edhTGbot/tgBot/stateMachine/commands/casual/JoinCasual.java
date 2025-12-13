package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.TournamentIdInputStrategy;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class JoinCasual extends Command {

    public JoinCasual() {
        super("join_casual", "main", 
              new TournamentIdInputStrategy("Введите ID казуал турнира"));
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().info("User {} joining casual tournament: {}", username, tournamentId);
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(userId, username, false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + "/join", 
                    entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().info("User {} joined casual tournament {} successfully", username, tournamentId);
            bot.sendMessage(chatId, "Присоединились к казуал турниру! Игрок: " + node.get("displayName").asText());
        } catch (Exception e) {
            bot.getLogger().error("Join casual tournament failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка присоединения: " + e.getMessage());
        }
    }
}
