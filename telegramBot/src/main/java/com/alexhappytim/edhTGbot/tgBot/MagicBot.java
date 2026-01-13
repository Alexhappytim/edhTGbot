package com.alexhappytim.edhTGbot.tgBot;

import com.alexhappytim.edhTGbot.tgBot.stateMachine.Keyboards;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.KeyboardWrapper;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import okhttp3.OkHttpClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.toIntExact;
import static org.telegram.telegrambots.abilitybots.api.objects.Locality.USER;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

public class MagicBot extends AbilityBot implements BotFacade {
    private static final Logger log = LoggerFactory.getLogger(MagicBot.class);
    private final Map<Long, UserSession> sessions = new java.util.HashMap<>();

    private final String restBaseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Long adminID;

    public MagicBot(String botToken, String botUsername, String restBaseUrl, Long adminID) {
        super(createTelegramClientWithTimeout(botToken), botUsername);
        this.restBaseUrl = restBaseUrl;
        this.adminID = adminID;
        this.restTemplate = createRestTemplateWithTimeout();
        this.objectMapper = new ObjectMapper();

        log.info("Initializing MagicBot with username: {}, REST URL: {}", botUsername, restBaseUrl);
        this.onRegister();
        log.info("MagicBot initialized successfully");

    }

    private static OkHttpTelegramClient createTelegramClientWithTimeout(String botToken) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        return new OkHttpTelegramClient(okHttpClient, botToken);
    }

    private RestTemplate createRestTemplateWithTimeout() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        return new RestTemplate(new BufferingClientHttpRequestFactory(factory));
    }

    @Override
    public void consume(Update update) {
        try {
            // Handle inline queries
            if (update.hasInlineQuery()) {
                handleInlineQuery(update.getInlineQuery());
                return;
            }
            
            long userId = 0;
            if (update.hasCallbackQuery()) {
                userId = update.getCallbackQuery().getFrom().getId();
            } else if (update.hasMessage()) {
                userId = update.getMessage().getFrom().getId();
            }

            UserSession session = getSession(userId);
            if (session == null) {
                session = new UserSession(null, null, null, null, null);
                setSession(userId, session);
            }

            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
                return;
            }


            if (update.hasMessage() && update.getMessage().hasText() && !update.getMessage().getText().startsWith("/")) {
                if (session.getPendingCommandKey() != null) {
                    Command cmd = Commands.fromKey(session.getPendingCommandKey());
                    if (session.getInputs() == null) session.setInputs(new java.util.ArrayList<>());
                    if (session.getInputStep() == null) session.setInputStep(0);
                    session.getInputs().add(update.getMessage().getText().trim());
                    int nextStep = session.getInputStep() + 1;
                    if (nextStep < cmd.getRequiredInputs()) {
                        session.setInputStep(nextStep);
                        InlineKeyboardRow cancelRow = new InlineKeyboardRow();
                        cancelRow.add(InlineKeyboardButton.builder().text("Отмена").callbackData("cancel").build());
                        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(java.util.List.of(cancelRow)).build();
                        sendMessage(update.getMessage().getChatId(), cmd.getInputPrompt(nextStep), kb);
                    } else {
                        cmd.execute(this, update);
                        session.setPendingCommandKey(null);
                        session.setInputs(null);
                        session.setInputStep(null);
                        String kbdName = cmd.getNextKeyboard();
                        Keyboards kbdType = Keyboards.fromKey(kbdName);
                        KeyboardWrapper keyboardWrapper = kbdType != null ? kbdType.getKeyboard() : Keyboards.MAIN.getKeyboard();
                        log.info("User {} selected keyboard: {}", userId, kbdName);
                        sendMessage(update.getMessage().getChatId(), keyboardWrapper.getText(), keyboardWrapper.getKeyboard());
                    }
                    return;
                }
            }

            super.consume(update);
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

    private void handleCallbackQuery(Update update) {
        long userId = update.getCallbackQuery().getFrom().getId();
        String data = update.getCallbackQuery().getData();

        if (data != null && data.equals("cancel")) {
            UserSession session = sessions.get(userId);
            if (session != null && session.getPendingCommandKey() != null) {
                Command cmd = Commands.fromKey(session.getPendingCommandKey());
                String kbdName = cmd != null ? cmd.getNextKeyboard() : "main";
                session.setPendingCommandKey(null);
                session.setInputs(null);
                session.setInputStep(null);
                Keyboards kbdType = Keyboards.fromKey(kbdName);
                KeyboardWrapper keyboardWrapper = kbdType != null ? kbdType.getKeyboard() : Keyboards.MAIN.getKeyboard();
                log.info("User {} cancelled input, returning to keyboard: {}", userId, kbdName);
                editMessage(update.getCallbackQuery().getMessage().getChatId(),
                        update.getCallbackQuery().getMessage().getMessageId(),
                        keyboardWrapper.getText(),
                        keyboardWrapper.getKeyboard());
            }
            return;
        }

        // Route callback explicitly by prefix to avoid random handling
        if (data != null) {
            if (data.startsWith("kick_user:") || data.startsWith("kick_page:")) {
                Command command = Commands.fromKey("kick_from_tournament");
                if (command != null && command.handleCallback(this, update, data)) {
                    return;
                }
            } else if (data.startsWith("submit_score:")) {
                Command command = Commands.fromKey("submit_result");
                if (command != null && command.handleCallback(this, update, data)) {
                    return;
                }
            } else if (data.startsWith("edit_match:") || data.startsWith("edit_score:") || data.equals("edit_back_to_matches")) {
                Command command = Commands.fromKey("edit_result");
                if (command != null && command.handleCallback(this, update, data)) {
                    return;
                }
            } else if (data.startsWith("switch_tournament:")) {
                Command command = Commands.fromKey("switch_tournament");
                if (command != null && command.handleCallback(this, update, data)) {
                    return;
                }
            }
        }

        if (data != null && data.startsWith("kbd:")) {
            String kbdName = data.substring("kbd:".length());
            Keyboards kbdType = Keyboards.fromKey(kbdName);
            KeyboardWrapper keyboardWrapper = kbdType != null ? kbdType.getKeyboard() : Keyboards.MAIN.getKeyboard();
            log.info("User {} selected keyboard: {}", userId, kbdName);
            editMessage(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId(), keyboardWrapper.getText(), keyboardWrapper.getKeyboard());
        } else if (data != null && data.startsWith("cmd:")) {
            String cmdName = data.substring("cmd:".length());
            Command command = Commands.fromKey(cmdName);
            UserSession userSession = sessions.get(userId);

            if (command.needsInput()) {
                userSession.setPendingCommandKey(cmdName);
                userSession.setInputs(new java.util.ArrayList<>());
                userSession.setInputStep(0);
                InlineKeyboardRow cancelRow = new InlineKeyboardRow();
                cancelRow.add(InlineKeyboardButton.builder().text("Отмена").callbackData("cancel").build());
                InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(java.util.List.of(cancelRow)).build();
                editMessage(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId(), command.getInputPrompt(0), kb);
            } else {
                command.execute(this, update);
            }
        }
    }

    @Override
    public long creatorId() {
        return adminID;
    }

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Show main menu")
                .privacy(PUBLIC)
                .locality(USER)
                .action(ctx -> {
                    long userId = ctx.chatId();
                    String[] args = ctx.arguments();
                    
                    // Handle deep link for tournament join
                    if (args.length > 0 && args[0].startsWith("join_")) {
                        String tournamentId = args[0].substring(5); // Remove "join_" prefix
                        handleTournamentJoinDeepLink(userId, tournamentId);
                        return;
                    }
                    
                    KeyboardWrapper kb = Keyboards.MAIN.getKeyboard();
                    SendMessage msg = SendMessage.builder()
                            .chatId(ctx.chatId())
                            .text(kb.getText())
                            .replyMarkup(kb.getKeyboard())
                            .build();
                    silent.execute(msg);
                })
                .build();
    }
    
    private void handleInlineQuery(InlineQuery inlineQuery) {
        try {
            Long userId = inlineQuery.getFrom().getId();
            
            // Fetch tournaments owned by user
            String url = restBaseUrl + "/tournaments/by-owner/" + userId;
            com.alexhappytim.mtg.dto.OwnerTournamentDTO[] tournaments = restTemplate.getForObject(url, com.alexhappytim.mtg.dto.OwnerTournamentDTO[].class);
            
            List<InlineQueryResultArticle> results = new ArrayList<>();
            System.out.println(Arrays.toString(tournaments));
            if (tournaments != null && tournaments.length > 0) {
                for (com.alexhappytim.mtg.dto.OwnerTournamentDTO tournament : tournaments) {
                    String typeDisplay = tournament.getType().equals("SWISS") ? "Швейцарка" : "Казуал";
                    String description = typeDisplay + " - " + tournament.getId();
                    
                    // Create inline keyboard with join button
                    InlineKeyboardButton joinButton = InlineKeyboardButton.builder()
                            .text("Присоединиться к турниру")
                            .url("https://t.me/" + getBotUsername() + "?start=join_" + tournament.getId())
                            .build();
                    InlineKeyboardRow row = new InlineKeyboardRow();
                    row.add(joinButton);
                    InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                            .keyboardRow(row)
                            .build();
                    
                    InputTextMessageContent messageContent = InputTextMessageContent.builder()
                            .messageText("Вас пригласили в турнир\n" +
                                    "*" + tournament.getName() + "*\n\n" +
                                    "Тип: " + typeDisplay + "\n" +
                                    "Код турнира: `" + tournament.getId() + "`\n\n" +
                                    "Нажмите кнопку ниже, чтобы присоединиться!")
                            .parseMode("Markdown")
                            .build();
                    
                    InlineQueryResultArticle article = InlineQueryResultArticle.builder()
                            .id(tournament.getId())
                            .title(tournament.getName())
                            .description(description)
                            .inputMessageContent(messageContent)
                            .replyMarkup(keyboard)
                            .build();
                    results.add(article);
                }
            }
            
            AnswerInlineQuery answer = AnswerInlineQuery.builder()
                    .inlineQueryId(inlineQuery.getId())
                    .results(results)
                    .cacheTime(0)
                    .build();
            
            silent.execute(answer);
            
        } catch (Exception e) {
            log.error("Failed to handle inline query", e);
        }
    }
    
    private void handleTournamentJoinDeepLink(long userId, String tournamentId) {
        try {
            // Check if user is registered
            String checkUrl = restBaseUrl + "/users/telegram/" + userId;
            boolean userExists = false;
            try {
                restTemplate.getForEntity(checkUrl, String.class);
                userExists = true;
            } catch (Exception e) {
                log.info("User {} not found, will register", userId);
            }
            
            // If user doesn't exist, trigger registration
            if (!userExists) {
                sendMessage(userId, "Добро пожаловать! Сначала зарегистрируйтесь, введя свой ник:");
                UserSession session = getSession(userId);
                if (session == null) {
                    session = new UserSession(null, null, null, null, null);
                }
                session.setPendingCommandKey("register");
                session.setInputs(new ArrayList<>());
                session.setInputStep(0);
                // Store tournament ID to join after registration
                session.setTournamentId(tournamentId);
                setSession(userId, session);
                return;
            }
            
            // User exists, proceed with join
            joinUserToTournament(userId, tournamentId);
            
        } catch (Exception e) {
            log.error("Failed to handle deep link for user {} and tournament {}", userId, tournamentId, e);
            sendMessage(userId, "❌ Ошибка при присоединении к турниру");
        }
    }
    
    @Override
    public void joinUserToTournament(long userId, String tournamentId) {
        try {
            com.alexhappytim.mtg.dto.JoinTournamentRequest request = new com.alexhappytim.mtg.dto.JoinTournamentRequest();
            request.setUserId(userId);
            request.setIsTemporary(false);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<com.alexhappytim.mtg.dto.JoinTournamentRequest> entity = 
                new org.springframework.http.HttpEntity<>(request, headers);
            
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    restBaseUrl + "/tournaments/" + tournamentId + "/join",
                    entity, String.class);
            
            com.fasterxml.jackson.databind.JsonNode responseJson = objectMapper.readTree(response.getBody());
            boolean joined = responseJson.get("joined").asBoolean();
            String tournamentType = responseJson.get("tournamentType").asText();
            
            UserSession session = getSession(userId);
            if (session == null) {
                session = new UserSession(null, null, null, null, null);
            }
            session.setTournamentId(tournamentId);
            session.setTournamentType(tournamentType);
            session.addTournament(tournamentId, tournamentType);
            setSession(userId, session);
            
            if (joined) {
                sendMessage(userId, "✅ Вы успешно присоединились к турниру " + tournamentId);
            } else {
                sendMessage(userId, "ℹ️ Вы уже зарегистрированы в турнире " + tournamentId);
            }
            
            // Show main keyboard
            KeyboardWrapper kb = Keyboards.MAIN.getKeyboard();
            sendMessage(userId, kb.getText(), kb.getKeyboard());
            
        } catch (Exception e) {
            log.error("Failed to join user {} to tournament {}", userId, tournamentId, e);
            sendMessage(userId, "❌ Ошибка при присоединении: " + e.getMessage());
        }
    }

    @Override
    public UserSession getSession(long userId) {
        UserSession session = sessions.get(userId);
        if (session == null) {
            try {
                if (db == null) {
                    log.warn("AbilityBot DBContext is null; skipping session load for user {} (persistence disabled)", userId);
                } else {
                    Map<Long, UserSession> persistedSessions = db.getMap("user_sessions");
                    session = persistedSessions.get(userId);
                    if (session != null) {
                        sessions.put(userId, session);
                        log.debug("Loaded session for user {} from database", userId);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load session for user {} from database: {}", userId, String.valueOf(e));
                log.debug("Session load exception", e);
            }
        }
        return session;
    }

    @Override
    public void setSession(long userId, UserSession session) {
        sessions.put(userId, session);
        // Persist to AbilityBot database using getMap
        try {
            if (db == null) {
                log.warn("AbilityBot DBContext is null; failed to persist session for user {} (persistence disabled)", userId);
                return;
            }
            Map<Long, UserSession> persistedSessions = db.getMap("user_sessions");
            persistedSessions.put(userId, session);
            db.commit();
            log.debug("Persisted session for user {} to database", userId);
        } catch (Exception e) {
            log.warn("Failed to persist session for user {} to database: {}", userId, String.valueOf(e));
            log.debug("Session persist exception", e);
        }
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getRestBaseUrl() {
        return restBaseUrl;
    }

    @Override
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void sendMessage(long chatID, String text) {

        sendMessage(chatID,text,new ReplyKeyboardRemove(true));

    }

    public void sendMessage(long chatID, String text, ReplyKeyboard kb) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatID)
                .text(text)
                .replyMarkup(kb)
                .build();
        sendMessage.enableMarkdown(true);
        sendMessage.setParseMode("Markdown");
        log.info("send " + text);
        silent.execute(sendMessage);

    }

    public void editMessage(long chatID, long messageID, String text, InlineKeyboardMarkup kb) {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatID)
                .messageId(toIntExact(messageID))
                .text(text)
                .replyMarkup(kb)
                .build();
        editMessage.enableMarkdown(true);
        editMessage.setParseMode("Markdown");
        silent.execute(
                editMessage
        );
    }


}
