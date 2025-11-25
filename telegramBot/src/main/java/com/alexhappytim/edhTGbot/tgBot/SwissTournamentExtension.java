package com.alexhappytim.edhTGbot.tgBot;

import com.alexhappytim.mtg.dto.CreateTournamentRequest;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.USER;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

public class SwissTournamentExtension implements AbilityExtension {
    private static final Logger log = LoggerFactory.getLogger(SwissTournamentExtension.class);
    private final String restBaseUrl;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final SilentSender silent;
    public SwissTournamentExtension(String restBaseUrl, ObjectMapper objectMapper, RestTemplate restTemplate, SilentSender silent) {
        this.restBaseUrl = restBaseUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.silent = silent;
    }
    public Ability createTournament() {
        return Ability.builder()
                .name("createtournament")
                .info("Create a tournament: /createtournament <name> <maxPlayers>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(2)
                .action(this::handleCreateTournament)
                .build();
    }

    public Ability joinTournament() {
        return Ability.builder()
                .name("jointournament")
                .info("Join a tournament: /jointournament <tournamentId> <userId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleJoinTournament)
                .build();
    }
    public Ability addToTournament() {
        return Ability.builder()
                .name("addtotournament")
                .info("Add not a bot user to a tournament: /addtotournament <tournamentId> <userId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(2)
                .action(this::handleAddToTournament)
                .build();
    }



    public Ability standings() {
        return Ability.builder()
                .name("standings")
                .info("Get standings: /standings <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleStandings)
                .build();
    }



    private void handleCreateTournament(MessageContext ctx) {
        log.info("User {} creating tournament: name={}, maxPlayers={}", 
                ctx.user().getUserName(), ctx.firstArg(), ctx.secondArg());
        try {
            CreateTournamentRequest request = new CreateTournamentRequest(ctx.firstArg(),Integer.parseInt(ctx.secondArg()), ctx.user().getId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(restBaseUrl + "/tournaments", entity, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            log.info("Tournament created successfully with ID: {}", node.get("id").asText());
            sendMessage(ctx, "Tournament created! ID: " + node.get("id").asText());
        } catch (Exception e) {
            log.error("Tournament creation failed for user {}: {}", ctx.user().getUserName(), e.getMessage(), e);
            sendMessage(ctx, "Tournament creation failed: " + e.getMessage());
        }
    }

    private void handleJoinTournament(MessageContext ctx) {
        log.info("User {} joining tournament: {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(ctx.user().getId(), ctx.user().getUserName(), false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(restBaseUrl + "/tournaments/" + ctx.firstArg() + "/join", entity, String.class);
            log.info("User {} joined tournament {} successfully", ctx.user().getUserName(), ctx.firstArg());
            sendMessage(ctx, "Joined tournament! Response: " + response.getBody());
        } catch (Exception e) {
            log.error("Join tournament failed for user {}: {}", ctx.user().getUserName(), e.getMessage(), e);
            sendMessage(ctx, "Join failed: " + e.getMessage());
        }
    }
    private void handleAddToTournament(MessageContext ctx) {
        log.info("User {} adding temporary user {} to tournament {}", 
                ctx.user().getUserName(), ctx.secondArg(), ctx.firstArg());
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(ctx.user().getId(), ctx.secondArg(),true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(restBaseUrl + "/tournaments/" + ctx.firstArg() + "/join", entity, String.class);
            log.info("Temporary user {} added to tournament {} successfully", ctx.secondArg(), ctx.firstArg());
            sendMessage(ctx, "Joined tournament! Response: " + response.getBody());
        } catch (Exception e) {
            log.error("Add to tournament failed: {}", e.getMessage(), e);
            sendMessage(ctx, "Join failed: " + e.getMessage());
        }
    }
    private void handleStandings(MessageContext ctx) {
        String tournamentId = ctx.firstArg();
        log.info("User {} requesting standings for tournament {}", ctx.user().getUserName(), tournamentId);
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(restBaseUrl + "/tournaments/" + tournamentId + "/standings", String.class);
            log.debug("Standings retrieved for tournament {}", tournamentId);
            sendMessage(ctx, "Standings: " + response.getBody());
        } catch (Exception e) {
            log.error("Failed to get standings for tournament {}: {}", tournamentId, e.getMessage(), e);
            sendMessage(ctx, "Failed to get standings: " + e.getMessage());
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
