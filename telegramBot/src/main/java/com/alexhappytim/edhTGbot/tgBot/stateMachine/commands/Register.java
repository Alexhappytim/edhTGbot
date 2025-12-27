package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.ResourceAccessException;
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext;
import com.alexhappytim.mtg.dto.CreateUserRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.ConnectException;

public class Register extends Command{

    public Register() {
        super("register", 1, "main", false, "Введите имя, которое будет отображаться в турнирных таблицах");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        if (!isMessage(update)) {
            bot.getLogger().warn("Register command requires message update, got callback query");
            return;
        }

        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String displayName = bot.getSession(userId).getInputs().get(0);

        bot.getLogger().info("User {} (chatId: {}) attempting to register with displayName: {}",
                username, chatId, displayName);
        try {
            CreateUserRequest request = new CreateUserRequest(
                    username,
                    displayName,
                    userId,
                    chatId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(bot.getRestBaseUrl() + "/users", entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());

            bot.getLogger().info("User {} registered successfully with ID: {}", username, node.get("id").asText());
            bot.sendMessage(chatId, "✅ Зарегистрированы! User ID: " + node.get("id").asText());
            
            // Check if there's a pending tournament join from deep link
            String pendingTournamentId = bot.getSession(userId).getTournamentId();
            if (pendingTournamentId != null && !pendingTournamentId.isEmpty()) {
                bot.getLogger().info("User {} has pending tournament join: {}", username, pendingTournamentId);
                bot.sendMessage(chatId, "Присоединяемся к турниру...");
                bot.joinUserToTournament(userId, pendingTournamentId);
            }
        }catch (ResourceAccessException e){
            bot.getLogger().error("Registration failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Регистрация не удалась: Невозможно подключиться к серверу");
        }
        catch (Exception e) {
            bot.getLogger().error("Registration failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Регистрация не удалась: " + e.getMessage());
        }
    }
}
