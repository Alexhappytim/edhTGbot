package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.SimpleInputStrategy;
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
        super("register", "main", 
              new SimpleInputStrategy("Введите имя, которое будет отображаться в турнирных таблицах"));
    }

    @Override
    public void execute(BotFacade bot, Update update) {
            long userId = getUserId(update);
            long chatId = getChatId(update);
            String displayName = bot.getSession(userId).getInputs().get(0);
            String username = getUsername(update);
            
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

            bot.getLogger().info("User {} registered successfully with ID: {}", update.getMessage().getFrom().getUserName(), node.get("id").asText());
            bot.sendMessage(update.getMessage().getChatId(), "Registered! User ID: " + node.get("id").asText());
        }catch (ResourceAccessException e){
            bot.getLogger().error("Registration failed for user {}: {}", update.getMessage().getFrom().getUserName(), e.getMessage(), e);
            bot.sendMessage(update.getMessage().getChatId(), "Registration failed: " + "Couldn't connect to server");
        }
        catch (Exception e) {
            bot.getLogger().error("Registration failed for user {}: {}", update.getMessage().getFrom().getUserName(), e.getMessage(), e);
            bot.sendMessage(update.getMessage().getChatId(), "Registration failed: " + e.getMessage());
        }
    }
}
