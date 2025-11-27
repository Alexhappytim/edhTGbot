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
public class Register extends Command{

    public Register(CommandGroup group, boolean needsInput, String inputPrompt) {
        super("register", true, "Введите имя, которое будет отображаться в турнирных таблицах");
    }

    @Override
    public void execute(BotFacade bot, MessageContext ctx, UserSessionAdapter session) {
                bot.getLogger().info("User {} (chatId: {}) attempting to register with displayName: {}",
                ctx.user().getUserName(), ctx.chatId(), ctx.firstArg());
        try {
            CreateUserRequest request = new CreateUserRequest(ctx.user().getUserName(), ctx.firstArg(), ctx.user().getId(), ctx.chatId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(bot.getRestBaseUrl() + "/users", entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());

            bot.getLogger().info("User {} registered successfully with ID: {}", ctx.user().getUserName(), node.get("id").asText());
            bot.sendMessage(ctx.chatId(), "Registered! User ID: " + node.get("id").asText());
        } catch (Exception e) {
            bot.getLogger().error("Registration failed for user {}: {}", ctx.user().getUserName(), e.getMessage(), e);
            bot.sendMessage(ctx.chatId(), "Registration failed: " + e.getMessage());
        }
    }
}
