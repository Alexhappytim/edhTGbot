package com.alexhappytim.edhTGbot.tgBot;

import com.alexhappytim.mtg.dto.CreateTournamentRequest;
import com.alexhappytim.mtg.dto.CreateUserRequest;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.function.BiConsumer;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.USER;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

public class SwissTournamentBot extends AbilityBot {
    private final String restBaseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Long adminID;
    public SwissTournamentBot(String botToken, String botUsername, String restBaseUrl, Long adminID) {
        super(new OkHttpTelegramClient(botToken), botUsername);
        this.restBaseUrl = restBaseUrl;
        this.adminID = adminID;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.onRegister();
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
                .action(ctx -> handleRegister(ctx))
                .build();
    }

    public Ability createTournament() {
        return Ability.builder()
                .name("createtournament")
                .info("Create a tournament: /createtournament <name> <maxPlayers>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(2)
                .action(ctx -> handleCreateTournament(ctx))
                .build();
    }

    public Ability joinTournament() {
        return Ability.builder()
                .name("jointournament")
                .info("Join a tournament: /jointournament <tournamentId> <userId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(ctx -> handleJoinTournament(ctx))
                .build();
    }
    public Ability addToTournament() {
        return Ability.builder()
                .name("addtotournament")
                .info("Add not a bot user to a tournament: /addtotournament <tournamentId> <userId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(2)
                .action(ctx -> handleAddToTournament(ctx))
                .build();
    }



    public Ability standings() {
        return Ability.builder()
                .name("standings")
                .info("Get standings: /standings <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(ctx -> handleStandings(ctx))
                .build();
    }

    private void handleRegister(MessageContext ctx) {

        try {
            CreateUserRequest request = new CreateUserRequest(ctx.user().getUserName(),ctx.firstArg(),ctx.user().getId(), ctx.chatId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(restBaseUrl + "/users", entity, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());

            sendMessage(ctx, "Registered! User ID: " + node.get("id").asText());
        } catch (Exception e) {
            sendMessage(ctx, "Registration failed: " + e.getMessage());
        }
    }

    private void handleCreateTournament(MessageContext ctx) {

        try {
            CreateTournamentRequest request = new CreateTournamentRequest(ctx.firstArg(),Integer.parseInt(ctx.secondArg()), ctx.user().getId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(restBaseUrl + "/tournaments", entity, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            sendMessage(ctx, "Tournament created! ID: " + node.get("id").asText());
        } catch (Exception e) {
            sendMessage(ctx, "Tournament creation failed: " + e.getMessage());
        }
    }

    private void handleJoinTournament(MessageContext ctx) {
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(ctx.user().getId(), ctx.user().getUserName(), false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(restBaseUrl + "/tournaments/" + ctx.firstArg() + "/join", entity, String.class);
            sendMessage(ctx, "Joined tournament! Response: " + response.getBody());
        } catch (Exception e) {
            sendMessage(ctx, "Join failed: " + e.getMessage());
        }
    }
    private void handleAddToTournament(MessageContext ctx) {
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(ctx.user().getId(), ctx.secondArg(),true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(restBaseUrl + "/tournaments/" + ctx.firstArg() + "/join", entity, String.class);
            sendMessage(ctx, "Joined tournament! Response: " + response.getBody());
        } catch (Exception e) {
            sendMessage(ctx, "Join failed: " + e.getMessage());
        }
    }
    private void handleStandings(MessageContext ctx) {
        String tournamentId = ctx.firstArg();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(restBaseUrl + "/tournaments/" + tournamentId + "/standings", String.class);
            sendMessage(ctx, "Standings: " + response.getBody());
        } catch (Exception e) {
            sendMessage(ctx, "Failed to get standings: " + e.getMessage());
        }
    }

    private void sendMessage(MessageContext ctx, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(ctx.chatId().toString())
                .text(text)
                .replyMarkup(new ReplyKeyboardRemove(true))
                .build();
            silent.execute(msg);
    }
}
