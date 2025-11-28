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
        super("register", 1, "main", "Введите имя, которое будет отображаться в турнирных таблицах");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
            bot.getLogger().info("User {} (chatId: {}) attempting to register with displayName: {}",
                update.getMessage().getFrom(), update.getMessage().getChatId(), bot.getSession(update.getMessage().getFrom().getId()).getInputs().get(0));
        try {
            CreateUserRequest request = new CreateUserRequest(
                    update.getMessage().getFrom().getUserName(),
                    bot.getSession(update.getMessage().getFrom().getId()).getInputs().get(0),
                    update.getMessage().getFrom().getId(),
                    update.getMessage().getChatId());
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
