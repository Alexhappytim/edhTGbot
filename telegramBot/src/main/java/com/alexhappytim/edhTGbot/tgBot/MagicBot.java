package com.alexhappytim.edhTGbot.tgBot;

import com.alexhappytim.edhTGbot.tgBot.stateMachine.KeyboardRegistry;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.KeyboardWrapper;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.CommandGroup;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.StateType;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.CommandRegistry;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;


import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import org.springframework.web.client.RestTemplate;

import static java.lang.Math.toIntExact;
import static org.telegram.telegrambots.abilitybots.api.objects.Locality.USER;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

public class MagicBot extends AbilityBot implements BotFacade {
    private static final Logger log = LoggerFactory.getLogger(MagicBot.class);
    private final java.util.Map<Long, UserSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    private final String restBaseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Long adminID;
    private final KeyboardRegistry keyboards;
    private final CommandRegistry commandRegistry;

    public MagicBot(String botToken, String botUsername, String restBaseUrl, Long adminID) {
        super(new OkHttpTelegramClient(botToken), botUsername);
        this.restBaseUrl = restBaseUrl;
        this.adminID = adminID;
        this.keyboards = new KeyboardRegistry();
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.commandRegistry = new CommandRegistry();

        log.info("Initializing MagicBot with username: {}, REST URL: {}", botUsername, restBaseUrl);

//        this.addExtension(new SwissTournamentExtension(restBaseUrl, objectMapper, restTemplate, silent));
//        this.addExtension(new CasualTournamentExtension(restBaseUrl, objectMapper, restTemplate, silent, this));

        this.onRegister();
        log.info("MagicBot initialized successfully");
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
            System.out.println(e.getMessage());
        }
    }
    private void handleCallbackQuery(Update update) {
        long userId = update.getCallbackQuery().getFrom().getId();
        String data = update.getCallbackQuery().getData();

        if (data != null && data.startsWith("kbd:")) {
            String kbdName = data.substring("kbd:".length());
            KeyboardWrapper keyboardWrapper =  keyboards.getKeyboard(kbdName);
//            UserSession s = sessions.computeIfAbsent(userId, k -> new UserSession());
            log.info("User {} selected keyboard: {}", userId, kbdName);
            editMessage(update.getCallbackQuery().getMessage().getChatId(),update.getCallbackQuery().getMessage().getMessageId(), keyboardWrapper.getText(),keyboardWrapper.getKeyboard());
        }
    }
    @Override
    public long creatorId() {
        return adminID;
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
                    KeyboardWrapper kb = keyboards.getKeyboard("main");
                    SendMessage msg = SendMessage.builder()
                            .chatId(ctx.chatId())
                            .text(kb.getText())
                            .replyMarkup(kb.getKeyboard())
                            .build();
                    silent.execute(msg);
                })
                .build();
    }

    private void handleRegister(MessageContext ctx) {

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


    private void handleAwaitingInput(Update update, long userId, UserSession session) {
//        long chatId = update.getMessage().getChatId();
//        String text = update.getMessage().getText().trim();
//
//        if ("tournamentId".equals(session.getAwaitingInputFor())) {
//            try {
//                Long tid = Long.parseLong(text);
//                session.setTournamentId(tid);
//                session.setAwaitingInputFor(null);
//                log.info("User {} selected tournament: {} #{}", userId, session.getType(), tid);
//                silent.execute(SendMessage.builder().chatId(chatId)
//                        .text("✅ Tournament selected: " + session.getType() + " #" + tid)
//                        .build());
//            } catch (NumberFormatException nfe) {
//                log.warn("User {} sent invalid tournament ID: {}", userId, text);
//                silent.execute(SendMessage.builder().chatId(chatId)
//                        .text("❌ Invalid ID. Please send a numeric tournament ID.")
//                        .build());
//            }
//        }
//        if (session.getState() == StateType.INPUT && session.getPendingCommandKey() != null) {
//            BotCommand cmd = commandRegistry.get(session.getPendingCommandKey());
//            if (cmd != null) {
//                        InlineKeyboardMarkup kb = keyboardRegistry.get("main");
//                    cmd.getExecutor().accept(text);
//                    silent.execute(SendMessage.builder().chatId(chatId).text("✅ Executed: " + cmd.getLabel()).build());
//                } catch (Exception ex) {
//                    silent.execute(SendMessage.builder().chatId(chatId).text("❌ Failed: " + ex.getMessage()).build());
//                }
//            }
//            session.setPendingCommandKey(null);
//            session.setState(StateType.GROUP);
//            if (session.getCurrentGroup() != null) sendGroupMenu(chatId, session.getCurrentGroup());
    }

    public void sendMessage(long chatID, String text) {
        silent.execute(SendMessage.builder()
                .chatId(chatID)
                .text(text)
                .replyMarkup(new ReplyKeyboardRemove(true))
                .build());

    }
    public void sendMessage(long chatID, String text, InlineKeyboardMarkup kb) {
        silent.execute(SendMessage.builder()
                .chatId(chatID)
                .text(text)
                .replyMarkup(kb)
                .build());

    }

    public void editMessage(long chatID,long messageID, String text, InlineKeyboardMarkup kb) {
        silent.execute(
                EditMessageText.builder()
                        .chatId(chatID)
                        .messageId(toIntExact(messageID))
                        .text(text)
                        .replyMarkup(kb)
                        .build()
        );
    }

    private void sendGroupMenu(long chatId, CommandGroup group) {
//        java.util.List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
//        for (BotCommand cmd : commandRegistry.values()) {
//            if (cmd.getGroup() == group) {
//                InlineKeyboardRow r = new InlineKeyboardRow();
//                r.add(InlineKeyboardButton.builder().text(cmd.getLabel()).callbackData("cmd:" + cmd.getKey()).build());
//                rows.add(r);
//            }
//        }
//        InlineKeyboardRow back = new InlineKeyboardRow();
//        back.add(InlineKeyboardButton.builder().text("Back").callbackData("back:main").build());
//        rows.add(back);
//        silent.execute(SendMessage.builder().chatId(chatId)
//                .text("Group: " + group.name())
//                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
//                .build());
    }
}
