package com.alexhappytim.edhTGbot.tgBot;

import com.alexhappytim.edhTGbot.tgBot.stateMachine.BotCommand;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.CommandGroup;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.Keyboards;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.StateType;
import com.alexhappytim.mtg.dto.CreateUserRequest;
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
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.USER;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

public class MagicBot extends AbilityBot implements SessionProvider {
    private static final Logger log = LoggerFactory.getLogger(MagicBot.class);
    private final java.util.Map<Long, UserSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final String restBaseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Long adminID;
    private final Keyboards keyboards;

    public MagicBot(String botToken, String botUsername, String restBaseUrl, Long adminID) {
        super(new OkHttpTelegramClient(botToken), botUsername);
        this.restBaseUrl = restBaseUrl;
        this.adminID = adminID;
        this.keyboards = new Keyboards();
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        log.info("Initializing MagicBot with username: {}, REST URL: {}", botUsername, restBaseUrl);

        this.addExtension(new SwissTournamentExtension(restBaseUrl, objectMapper, restTemplate, silent));
        this.addExtension(new CasualTournamentExtension(restBaseUrl, objectMapper, restTemplate, silent, this));

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

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Show main menu")
                .privacy(PUBLIC)
                .locality(USER)
                .action(ctx -> {
                    InlineKeyboardMarkup kb = keyboards.getMainKeyboard();
                    SendMessage msg = SendMessage.builder()
                            .chatId(ctx.chatId())
                            .text("Привет! Выбери действие:")
                            .replyMarkup(kb)
                            .build();
                    silent.execute(msg);
                })
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

    private final java.util.Map<String, BotCommand> commandRegistry = new java.util.HashMap<>();

    private void initCommands() {
        commandRegistry.put("register", new BotCommand("register", "Register", CommandGroup.REGISTRATION, true, "Send display name", input -> {
        }));
        commandRegistry.put("createtournament", new BotCommand("createtournament", "Create Swiss", CommandGroup.SWISS, true, "Send: <name> <maxPlayers>", in -> {
        }));
        commandRegistry.put("jointournament", new BotCommand("jointournament", "Join Swiss", CommandGroup.SWISS, true, "Send tournament ID", in -> {
        }));
        commandRegistry.put("addtotournament", new BotCommand("addtotournament", "Add To Swiss", CommandGroup.SWISS, true, "Send: <tournamentId> <displayName>", in -> {
        }));
        commandRegistry.put("standings", new BotCommand("standings", "Standings", CommandGroup.SWISS, true, "Send tournament ID", in -> {
        }));
        commandRegistry.put("createcasual", new BotCommand("createcasual", "Create Casual", CommandGroup.CASUAL, true, "Send casual tournament name", in -> {
        }));
        commandRegistry.put("joincasual", new BotCommand("joincasual", "Join Casual", CommandGroup.CASUAL, true, "Send casual tournament ID", in -> {
        }));
        commandRegistry.put("addtocasual", new BotCommand("addtocasual", "Add To Casual", CommandGroup.CASUAL, true, "Send: <tournamentId> <displayName>", in -> {
        }));
        commandRegistry.put("startcasual", new BotCommand("startcasual", "Start Casual Round", CommandGroup.CASUAL, true, "Send casual tournament ID", in -> {
        }));
        commandRegistry.put("readycasual", new BotCommand("readycasual", "Ready", CommandGroup.CASUAL, true, "Send casual tournament ID", in -> {
        }));
        commandRegistry.put("adminready", new BotCommand("adminready", "Admin Ready", CommandGroup.CASUAL, true, "Send: <tournamentId> <userId>", in -> {
        }));
        commandRegistry.put("adminnotready", new BotCommand("adminnotready", "Admin Not Ready", CommandGroup.CASUAL, true, "Send: <tournamentId> <userId>", in -> {
        }));
        commandRegistry.put("reshufflecasual", new BotCommand("reshufflecasual", "Reshuffle", CommandGroup.CASUAL, true, "Send casual tournament ID", in -> {
        }));
        commandRegistry.put("casualgroups", new BotCommand("casualgroups", "Casual Groups", CommandGroup.INFO, true, "Send casual tournament ID", in -> {
        }));
        commandRegistry.put("casualreadylist", new BotCommand("casualreadylist", "Casual Ready List", CommandGroup.INFO, true, "Send casual tournament ID", in -> {
        }));
        commandRegistry.put("casualinfo", new BotCommand("casualinfo", "Casual Info", CommandGroup.INFO, true, "Send casual tournament ID", in -> {
        }));
    }

    public void promptSelectTournamentType(long chatId) {
        InlineKeyboardButton swissBtn = InlineKeyboardButton.builder().text("Swiss").callbackData("type:SWISS").build();
        InlineKeyboardButton casualBtn = InlineKeyboardButton.builder().text("Casual").callbackData("type:CASUAL").build();
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(swissBtn);
        row.add(casualBtn);
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(java.util.List.of(row)).build();
        SendMessage msg = SendMessage.builder().chatId(chatId).text("Choose tournament type:").replyMarkup(kb).build();
        silent.execute(msg);
    }

    public void promptEnterTournamentId(long chatId) {
        SendMessage msg = SendMessage.builder().chatId(chatId)
                .text("Send tournament ID in next message")
                .replyMarkup(new ReplyKeyboardRemove(true))
                .build();
        silent.execute(msg);
    }


    // SessionProvider implementation
    @Override
    public UserSession getSession(long userId) {
        return sessions.get(userId);
    }

    @Override
    public void setSession(long userId, UserSession session) {
        sessions.put(userId, session);
    }

    @Override
    public void consume(Update update) {
        try {
            // Handle callback queries for inline keyboards
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
                return;
            }

            // Check if user is awaiting input before processing commands
            if (update.hasMessage() && update.getMessage().hasText() && !update.getMessage().getText().subSequence(0, 1).equals("/")) {
                long userId = update.getMessage().getFrom().getId();
                UserSession session = sessions.get(userId);

                if (session != null && session.getAwaitingInputFor() != null) {
                    handleAwaitingInput(update, userId, session);
                    return;
                }
            }

            // Normal command processing
            super.consume(update);
        } catch (Exception e) {
            log.error("Update handling error: {}", e.getMessage(), e);
        }
    }

    private void handleCallbackQuery(Update update) {
        long userId = update.getCallbackQuery().getFrom().getId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        if (data != null && data.startsWith("type:")) {
            String typeStr = data.substring("type:".length());
            UserSession s = sessions.computeIfAbsent(userId, k -> new UserSession());
            s.setType("SWISS".equals(typeStr) ? UserSession.TournamentType.SWISS : UserSession.TournamentType.CASUAL);
            s.setAwaitingInputFor("tournamentId");
            log.info("User {} selected tournament type: {}", userId, typeStr);
            promptEnterTournamentId(chatId);
        }
    }

    private void handleAwaitingInput(Update update, long userId, UserSession session) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        if ("tournamentId".equals(session.getAwaitingInputFor())) {
            try {
                Long tid = Long.parseLong(text);
                session.setTournamentId(tid);
                session.setAwaitingInputFor(null);
                log.info("User {} selected tournament: {} #{}", userId, session.getType(), tid);
                silent.execute(SendMessage.builder().chatId(chatId)
                        .text("✅ Tournament selected: " + session.getType() + " #" + tid)
                        .build());
            } catch (NumberFormatException nfe) {
                log.warn("User {} sent invalid tournament ID: {}", userId, text);
                silent.execute(SendMessage.builder().chatId(chatId)
                        .text("❌ Invalid ID. Please send a numeric tournament ID.")
                        .build());
            }
        }
        if (session.getState() == StateType.INPUT && session.getPendingCommandKey() != null) {
            BotCommand cmd = commandRegistry.get(session.getPendingCommandKey());
            if (cmd != null) {
                try {
                    cmd.getExecutor().accept(text);
                    silent.execute(SendMessage.builder().chatId(chatId).text("✅ Executed: " + cmd.getLabel()).build());
                } catch (Exception ex) {
                    silent.execute(SendMessage.builder().chatId(chatId).text("❌ Failed: " + ex.getMessage()).build());
                }
            }
            session.setPendingCommandKey(null);
            session.setState(StateType.GROUP);
            if (session.getCurrentGroup() != null) sendGroupMenu(chatId, session.getCurrentGroup());
        }
    }

    private void sendMessage(MessageContext ctx, String text) {
        silent.execute(SendMessage.builder()
                .chatId(ctx.chatId().toString())
                .text(text)
                .replyMarkup(new ReplyKeyboardRemove(true))
                .build());

    }

    private void sendGroupMenu(long chatId, CommandGroup group) {
        java.util.List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
        for (BotCommand cmd : commandRegistry.values()) {
            if (cmd.getGroup() == group) {
                InlineKeyboardRow r = new InlineKeyboardRow();
                r.add(InlineKeyboardButton.builder().text(cmd.getLabel()).callbackData("cmd:" + cmd.getKey()).build());
                rows.add(r);
            }
        }
        InlineKeyboardRow back = new InlineKeyboardRow();
        back.add(InlineKeyboardButton.builder().text("Back").callbackData("back:main").build());
        rows.add(back);
        silent.execute(SendMessage.builder().chatId(chatId)
                .text("Group: " + group.name())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build());
    }
}
