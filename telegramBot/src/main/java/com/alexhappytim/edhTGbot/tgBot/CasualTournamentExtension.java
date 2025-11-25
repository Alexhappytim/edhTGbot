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

public class CasualTournamentExtension implements AbilityExtension {
    private static final Logger log = LoggerFactory.getLogger(CasualTournamentExtension.class);
    private final String restBaseUrl;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final SilentSender silent;

    public CasualTournamentExtension(String restBaseUrl, ObjectMapper objectMapper, RestTemplate restTemplate, SilentSender silent) {
        this.restBaseUrl = restBaseUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.silent = silent;
    }

    // Create a casual tournament
    public Ability createCasualTournament() {
        return Ability.builder()
                .name("createcasual")
                .info("Create a casual tournament: /createcasual <name>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleCreateCasualTournament)
                .build();
    }

    // Join a casual tournament
    public Ability joinCasualTournament() {
        return Ability.builder()
                .name("joincasual")
                .info("Join a casual tournament: /joincasual <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleJoinCasualTournament)
                .build();
    }

    // Add a temporary user to a casual tournament (admin only)
    public Ability addToCasualTournament() {
        return Ability.builder()
                .name("addtocasual")
                .info("Add a temporary user to casual tournament: /addtocasual <tournamentId> <playerName>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(2)
                .action(this::handleAddToCasualTournament)
                .build();
    }

    // Start a round (shuffle all users into groups of 4)
    public Ability startCasualRound() {
        return Ability.builder()
                .name("startcasual")
                .info("Start a casual round: /startcasual <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleStartCasualRound)
                .build();
    }

    // Mark yourself as ready
    public Ability readyCasual() {
        return Ability.builder()
                .name("readycasual")
                .info("Mark yourself as ready: /readycasual <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleReadyCasual)
                .build();
    }

    // Admin marks a user as ready
    public Ability adminReadyCasual() {
        return Ability.builder()
                .name("adminready")
                .info("Admin marks user as ready: /adminready <tournamentId> <userId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(2)
                .action(this::handleAdminReadyCasual)
                .build();
    }

    // Admin marks a user as not ready
    public Ability adminNotReadyCasual() {
        return Ability.builder()
                .name("adminnotready")
                .info("Admin marks user as not ready: /adminnotready <tournamentId> <userId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(2)
                .action(this::handleAdminNotReadyCasual)
                .build();
    }

    // Reshuffle ready users into new groups (admin only)
    public Ability reshuffleCasual() {
        return Ability.builder()
                .name("reshufflecasual")
                .info("Reshuffle ready users: /reshufflecasual <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleReshuffleCasual)
                .build();
    }

    // Get current groups
    public Ability casualGroups() {
        return Ability.builder()
                .name("casualgroups")
                .info("Get current groups: /casualgroups <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleCasualGroups)
                .build();
    }

    // Get ready users
    public Ability casualReady() {
        return Ability.builder()
                .name("casualreadylist")
                .info("Get ready users: /casualreadylist <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleCasualReadyList)
                .build();
    }

    // Get tournament info
    public Ability casualInfo() {
        return Ability.builder()
                .name("casualinfo")
                .info("Get casual tournament info: /casualinfo <tournamentId>")
                .privacy(PUBLIC)
                .locality(USER)
                .input(1)
                .action(this::handleCasualInfo)
                .build();
    }

    // --- Handlers ---

    private void handleCreateCasualTournament(MessageContext ctx) {
        log.info("User {} creating casual tournament: {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            CreateTournamentRequest request = new CreateTournamentRequest(ctx.firstArg(), 0, ctx.user().getId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(restBaseUrl + "/tournamentsCasual", entity, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            log.info("Casual tournament created with ID: {}", node.get("id").asText());
            sendMessage(ctx, "Casual tournament created! ID: " + node.get("id").asText());
        } catch (Exception e) {
            log.error("Casual tournament creation failed for user {}: {}", ctx.user().getUserName(), e.getMessage(), e);
            sendMessage(ctx, "Creation failed: " + e.getMessage());
        }
    }

    private void handleJoinCasualTournament(MessageContext ctx) {
        log.info("User {} joining casual tournament: {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(ctx.user().getId(), ctx.user().getUserName(), false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/join", entity, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            log.info("User {} joined casual tournament {} successfully", ctx.user().getUserName(), ctx.firstArg());
            sendMessage(ctx, "Joined casual tournament! User: " + node.get("displayName").asText());
        } catch (Exception e) {
            log.error("Join casual tournament failed for user {}: {}", ctx.user().getUserName(), e.getMessage(), e);
            sendMessage(ctx, "Join failed: " + e.getMessage());
        }
    }

    private void handleAddToCasualTournament(MessageContext ctx) {
        log.info("User {} adding temporary user {} to casual tournament {}", 
                ctx.user().getUserName(), ctx.secondArg(), ctx.firstArg());
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(ctx.user().getId(), ctx.secondArg(), true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/join", entity, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            log.info("Temporary user {} added to casual tournament {}", ctx.secondArg(), ctx.firstArg());
            sendMessage(ctx, "Added temporary user: " + node.get("displayName").asText());
        } catch (Exception e) {
            log.error("Add to casual tournament failed: {}", e.getMessage(), e);
            sendMessage(ctx, "Add failed: " + e.getMessage());
        }
    }

    private void handleStartCasualRound(MessageContext ctx) {
        log.info("User {} starting round for casual tournament {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/start-round",
                    null,
                    String.class);
            JsonNode groups = objectMapper.readTree(response.getBody());
            log.info("Round started for tournament {}, {} groups created", ctx.firstArg(), groups.size());
            StringBuilder sb = new StringBuilder("Round started! Groups:\n");
            for (JsonNode group : groups) {
                sb.append("Group ").append(group.get("groupNumber").asInt()).append(": ");
                for (JsonNode player : group.get("players")) {
                    sb.append(player.get("displayName").asText()).append(", ");
                }
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }
            sendMessage(ctx, sb.toString());
        } catch (Exception e) {
            log.error("Start round failed for tournament {}: {}", ctx.firstArg(), e.getMessage(), e);
            sendMessage(ctx, "Start round failed: " + e.getMessage());
        }
    }

    private void handleReadyCasual(MessageContext ctx) {
        log.info("User {} marking as ready in tournament {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            String url = restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/ready?userId=" + ctx.user().getId() + "&requesterId=" + ctx.user().getId();
            restTemplate.postForEntity(url, null, String.class);
            log.info("User {} marked as ready in tournament {}", ctx.user().getUserName(), ctx.firstArg());
            sendMessage(ctx, "You are now marked as ready!");
        } catch (Exception e) {
            log.error("Ready failed for user {} in tournament {}: {}", ctx.user().getUserName(), ctx.firstArg(), e.getMessage(), e);
            sendMessage(ctx, "Ready failed: " + e.getMessage());
        }
    }

    private void handleAdminReadyCasual(MessageContext ctx) {
        log.info("Admin {} marking user {} as ready in tournament {}", 
                ctx.user().getUserName(), ctx.secondArg(), ctx.firstArg());
        try {
            String url = restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/ready?userId=" + ctx.secondArg() + "&requesterId=" + ctx.user().getId();
            restTemplate.postForEntity(url, null, String.class);
            log.info("User {} marked as ready by admin {} in tournament {}", ctx.secondArg(), ctx.user().getUserName(), ctx.firstArg());
            sendMessage(ctx, "User " + ctx.secondArg() + " is now marked as ready!");
        } catch (Exception e) {
            log.error("Admin ready failed: {}", e.getMessage(), e);
            sendMessage(ctx, "Admin ready failed: " + e.getMessage());
        }
    }

    private void handleAdminNotReadyCasual(MessageContext ctx) {
        log.info("Admin {} marking user {} as not ready in tournament {}", 
                ctx.user().getUserName(), ctx.secondArg(), ctx.firstArg());
        try {
            String url = restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/not-ready?userId=" + ctx.secondArg() + "&adminId=" + ctx.user().getId();
            restTemplate.postForEntity(url, null, String.class);
            log.info("User {} marked as not ready by admin {} in tournament {}", ctx.secondArg(), ctx.user().getUserName(), ctx.firstArg());
            sendMessage(ctx, "User " + ctx.secondArg() + " is now marked as not ready!");
        } catch (Exception e) {
            log.error("Admin not ready failed: {}", e.getMessage(), e);
            sendMessage(ctx, "Admin not ready failed: " + e.getMessage());
        }
    }

    private void handleReshuffleCasual(MessageContext ctx) {
        log.info("Admin {} reshuffling ready users in tournament {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            String url = restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/reshuffle?adminId=" + ctx.user().getId();
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            JsonNode groups = objectMapper.readTree(response.getBody());
            log.info("Reshuffle completed for tournament {}, {} new groups", ctx.firstArg(), groups.size());
            StringBuilder sb = new StringBuilder("Reshuffled! New groups:\n");
            for (JsonNode group : groups) {
                sb.append("Group ").append(group.get("groupNumber").asInt()).append(": ");
                for (JsonNode player : group.get("players")) {
                    sb.append(player.get("displayName").asText()).append(", ");
                }
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }
            sendMessage(ctx, sb.toString());
        } catch (Exception e) {
            log.error("Reshuffle failed for tournament {}: {}", ctx.firstArg(), e.getMessage(), e);
            sendMessage(ctx, "Reshuffle failed: " + e.getMessage());
        }
    }

    private void handleCasualGroups(MessageContext ctx) {
        log.debug("User {} requesting groups for tournament {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/groups", String.class);
            JsonNode groups = objectMapper.readTree(response.getBody());
            log.debug("Retrieved {} groups for tournament {}", groups.size(), ctx.firstArg());
            StringBuilder sb = new StringBuilder("Current groups:\n");
            for (JsonNode group : groups) {
                sb.append("Group ").append(group.get("groupNumber").asInt()).append(": ");
                for (JsonNode player : group.get("players")) {
                    sb.append(player.get("displayName").asText()).append(", ");
                }
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }
            sendMessage(ctx, sb.toString());
        } catch (Exception e) {
            log.error("Get groups failed for tournament {}: {}", ctx.firstArg(), e.getMessage(), e);
            sendMessage(ctx, "Get groups failed: " + e.getMessage());
        }
    }

    private void handleCasualReadyList(MessageContext ctx) {
        log.debug("User {} requesting ready list for tournament {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(restBaseUrl + "/tournamentsCasual/" + ctx.firstArg() + "/ready-users", String.class);
            JsonNode users = objectMapper.readTree(response.getBody());
            log.debug("Retrieved {} ready users for tournament {}", users.size(), ctx.firstArg());
            StringBuilder sb = new StringBuilder("Ready users:\n");
            for (JsonNode user : users) {
                sb.append("- ").append(user.get("displayName").asText()).append("\n");
            }
            sendMessage(ctx, sb.toString());
        } catch (Exception e) {
            log.error("Get ready users failed for tournament {}: {}", ctx.firstArg(), e.getMessage(), e);
            sendMessage(ctx, "Get ready users failed: " + e.getMessage());
        }
    }

    private void handleCasualInfo(MessageContext ctx) {
        log.debug("User {} requesting info for tournament {}", ctx.user().getUserName(), ctx.firstArg());
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(restBaseUrl + "/tournamentsCasual/" + ctx.firstArg(), String.class);
            JsonNode tournament = objectMapper.readTree(response.getBody());
            log.debug("Retrieved info for tournament {}", ctx.firstArg());
            StringBuilder sb = new StringBuilder();
            sb.append("Tournament: ").append(tournament.get("name").asText()).append("\n");
            sb.append("Owner: ").append(tournament.get("owner").get("displayName").asText()).append("\n");
            sb.append("Users: ").append(tournament.get("users").size()).append("\n");
            sb.append("Ready: ").append(tournament.get("readyUsers").size()).append("\n");
            sb.append("Groups: ").append(tournament.get("groups").size()).append("\n");
            sendMessage(ctx, sb.toString());
        } catch (Exception e) {
            log.error("Get info failed for tournament {}: {}", ctx.firstArg(), e.getMessage(), e);
            sendMessage(ctx, "Get info failed: " + e.getMessage());
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
