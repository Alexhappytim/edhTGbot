package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.springframework.http.HttpEntity;
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext;
import com.alexhappytim.mtg.dto.CreateUserRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import org.telegram.telegrambots.meta.api.objects.Update;

public class Register extends Command{

    public Register() {
        super("register", true, "Введите имя, которое будет отображаться в турнирных таблицах");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
                bot.getLogger().info("User {} (chatId: {}) attempting to register with displayName: {}",
                update.getCallbackQuery().getFrom(), update.getMessage().getChatId(), bot.getSession(update.getCallbackQuery().getFrom().getId()).getInput());
        try {
            CreateUserRequest request = new CreateUserRequest(
                    update.getCallbackQuery().getFrom().getUserName(),
                    bot.getSession(update.getCallbackQuery().getFrom().getId()).getInput(),
                    update.getCallbackQuery().getFrom().getId(),
                    update.getMessage().getChatId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(bot.getRestBaseUrl() + "/users", entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());

            bot.getLogger().info("User {} registered successfully with ID: {}", update.getCallbackQuery().getFrom().getUserName(), node.get("id").asText());
            bot.sendMessage(update.getMessage().getChatId(), "Registered! User ID: " + node.get("id").asText());
        } catch (Exception e) {
            bot.getLogger().error("Registration failed for user {}: {}", update.getCallbackQuery().getFrom().getUserName(), e.getMessage(), e);
            bot.sendMessage(update.getMessage().getChatId(), "Registration failed: " + e.getMessage());
        }
    }
}
