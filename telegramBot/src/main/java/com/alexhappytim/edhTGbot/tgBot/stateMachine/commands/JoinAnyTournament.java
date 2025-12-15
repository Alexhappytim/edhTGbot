package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.telegram.telegrambots.meta.api.objects.Update;

public class JoinAnyTournament extends Command {

    public JoinAnyTournament() {
        super("join_any_tournament", 1, "main", false, "Введите ID турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String tournamentId = bot.getSession(userId).getInputs().getFirst();
        String username = getUsername(update);

        bot.getLogger().info("User {} joining tournament: {}", username, tournamentId);
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(userId, username, false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/join",
                    entity, String.class);

            JsonNode jsonResponse = bot.getObjectMapper().readTree(response.getBody());
            String tournamentType = jsonResponse.get("tournamentType").asText();
            boolean joined = jsonResponse.get("joined").asBoolean();

            // Save tournament info to session
            bot.getSession(userId).setTournamentId(tournamentId);
            bot.getSession(userId).setTournamentType(tournamentType);
            bot.getSession(userId).addTournament(tournamentId, tournamentType);

            bot.getLogger().info("User {} joined tournament {} (type: {})", username, tournamentId, tournamentType);
            
            if (joined) {
                String typeDisplay = tournamentType.equalsIgnoreCase("CASUAL") ? "казуальный" : "классический";
                bot.sendMessage(chatId, String.format("✅ Вы успешно присоединились к турниру!\n\n" +
                        "🎯 ID турнира: %s\n" +
                        "📋 Тип: %s", tournamentId, typeDisplay));
            } else {
                bot.sendMessage(chatId, "ℹ️ Вы уже зарегистрированы в этом турнире.");
            }
        } catch (HttpClientErrorException e) {
            bot.getLogger().error("Join tournament failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка присоединения к турниру: " + e.getMessage());
        } catch (Exception e) {
            bot.getLogger().error("Join tournament failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка присоединения к турниру: " + e.getMessage());
        }
    }
}
