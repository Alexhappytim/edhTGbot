package com.alexhappytim.edhTGbot.tgBot;

import com.alexhappytim.mtg.dto.CreateTournamentRequest;
import com.alexhappytim.mtg.dto.CreateUserRequest;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.USER;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

public class MagicBot extends AbilityBot {
    private static final Logger log = LoggerFactory.getLogger(MagicBot.class);
    private final String restBaseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Long adminID;

    public MagicBot(String botToken, String botUsername, String restBaseUrl, Long adminID) {
        super(new OkHttpTelegramClient(botToken), botUsername);
        this.restBaseUrl = restBaseUrl;
        this.adminID = adminID;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        log.info("Initializing MagicBot with username: {}, REST URL: {}", botUsername, restBaseUrl);

        this.addExtension(new SwissTournamentExtension(restBaseUrl, objectMapper, restTemplate, silent));
        this.addExtension(new CasualTournamentExtension(restBaseUrl, objectMapper, restTemplate, silent));

        this.onRegister();
        log.info("MagicBot initialized successfully");
    }

    @Override
    public long creatorId() {
        return adminID; // Set your Telegram user ID for admin abilities
    }

    public Ability register() {
        return Ability.builder()
                .name("register")
                .info("Register a new user: /register <username> <displayName>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleRegister)
                .build();
    }

    private void handleRegister(MessageContext ctx) {
        log.info("User {} (chatId: {}) attempting to register with displayName: {}", 
                ctx.user().getUserName(), ctx.chatId(), ctx.firstArg());
        try {
            CreateUserRequest request = new CreateUserRequest(ctx.user().getUserName(), ctx.firstArg(), ctx.user().getId(), ctx.chatId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(restBaseUrl + "/users", entity, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());

            log.info("User {} registered successfully with ID: {}", ctx.user().getUserName(), node.get("id").asText());
            sendMessage(ctx, "Registered! User ID: " + node.get("id").asText());
        } catch (Exception e) {
            log.error("Registration failed for user {}: {}", ctx.user().getUserName(), e.getMessage(), e);
            sendMessage(ctx, "Registration failed: " + e.getMessage());
        }
    }

    private void sendMessage(MessageContext ctx, String text) {
        silent.execute(SendMessage.builder()
                .chatId(ctx.chatId().toString())
                .text(text)
                .replyMarkup(new ReplyKeyboardRemove(true))
                .build());

    }
}
