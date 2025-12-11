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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.toIntExact;
import static org.telegram.telegrambots.abilitybots.api.objects.Locality.USER;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

public class MagicBot extends AbilityBot implements BotFacade {
    private static final Logger log = LoggerFactory.getLogger(MagicBot.class);
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

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
//        this.addExtension(new SwissTournamentExtension(restBaseUrl, objectMapper, restTemplate, silent));
//        this.addExtension(new CasualTournamentExtension(restBaseUrl, objectMapper, restTemplate, silent, this));

        this.onRegister();
        log.info("MagicBot initialized successfully");
    }
    @Override
    public void consume(Update update) {
        try {
            long userId = 0;
            if (update.hasCallbackQuery()) {
                userId = update.getCallbackQuery().getFrom().getId();
            }
            else if(update.hasMessage()){
                userId = update.getMessage().getFrom().getId();
            }

            UserSession session = sessions.get(userId);
            if(session == null){
                sessions.put(userId, new UserSession(null, null, null, null));
                session = sessions.get(userId);
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
                        sendMessage(update.getMessage().getChatId(), keyboardWrapper.getText(),keyboardWrapper.getKeyboard());
                    }
                    return;
                }
            }

            // Normal command processing
            super.consume(update);
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
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

        if (data != null && data.startsWith("kbd:")) {
            String kbdName = data.substring("kbd:".length());
            Keyboards kbdType = Keyboards.fromKey(kbdName);
            KeyboardWrapper keyboardWrapper = kbdType != null ? kbdType.getKeyboard() : Keyboards.MAIN.getKeyboard();
//            UserSession s = sessions.computeIfAbsent(userId, k -> new UserSession());
            log.info("User {} selected keyboard: {}", userId, kbdName);
            editMessage(update.getCallbackQuery().getMessage().getChatId(),update.getCallbackQuery().getMessage().getMessageId(), keyboardWrapper.getText(),keyboardWrapper.getKeyboard());
        }else if(data != null && data.startsWith("cmd:")){
            String cmdName = data.substring("cmd:".length());
            Command command = Commands.fromKey(cmdName);
            if(command.needsInput()){
                UserSession userSession = sessions.get(userId);
                userSession.setPendingCommandKey(cmdName);
                userSession.setInputs(new java.util.ArrayList<>());
                userSession.setInputStep(0);
                InlineKeyboardRow cancelRow = new InlineKeyboardRow();
                cancelRow.add(InlineKeyboardButton.builder().text("Отмена").callbackData("cancel").build());
                InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboard(java.util.List.of(cancelRow)).build();
                editMessage(update.getCallbackQuery().getMessage().getChatId(),update.getCallbackQuery().getMessage().getMessageId(), command.getInputPrompt(0), kb);
            }else{
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


}
