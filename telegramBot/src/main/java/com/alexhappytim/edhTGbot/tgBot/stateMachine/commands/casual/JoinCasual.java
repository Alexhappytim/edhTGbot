package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.telegram.telegrambots.meta.api.objects.Update;

public class JoinCasual extends Command {

    public JoinCasual() {
        super("join_casual", 0, "main", true);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        String username = getUsername(update);
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: сначала введите ID турнира");
            return;
        }
        
        bot.getLogger().info("User {} joining casual tournament: {}", username, tournamentId);
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(userId, username, false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/join", 
                    entity, String.class);
            
            JsonNode responseNode = bot.getObjectMapper().readTree(response.getBody());
            String tournamentType = responseNode.get("tournamentType").asText();
            
            // Save tournament type to session
            bot.getSession(userId).setTournamentType(tournamentType);
            
            bot.getLogger().info("User {} joined casual tournament {} successfully (type: {})", username, tournamentId, tournamentType);
            bot.sendMessage(chatId, "✅ Присоединились к казуал турниру! Тип: " + tournamentType);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                // 409 Conflict: User already registered or tournament exists
                // Save tournament ID to session anyway
                bot.getSession(userId).setTournamentId(tournamentId);
                bot.getLogger().info("User {} conflict joining casual tournament {}: already registered", username, tournamentId);
                bot.sendMessage(chatId, "ℹ️ Вы уже зарегистрированы в этом турнире. ID турнира сохранен в сессию.");
            } else {
                bot.getLogger().error("Join casual tournament failed for user {}: {}", username, e.getMessage(), e);
                bot.sendMessage(chatId, "❌ Ошибка присоединения: " + e.getMessage());
            }
        } catch (Exception e) {
            bot.getLogger().error("Join casual tournament failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка присоединения: " + e.getMessage());
        }
    }
}
