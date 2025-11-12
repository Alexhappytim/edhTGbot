package com.alexhappytim.edhTGbot.tgBot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;

@Component
public class MTGBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TournamentRepository tournamentRepository;
    private final TelegramClient telegramClient;
    private final Map<Long, BotState> userStates = new HashMap<>();
    private final Map<Long, User> tempUserData = new HashMap<>();
    private final Map<Long, Tournament> tempTournamentData = new HashMap<>();

    public MTGBot() {
        telegramClient = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        return System.getenv("TELEGRAM_TOKEN");
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();
            BotState state = userStates.getOrDefault(chat_id, BotState.IDLE);
            switch (state) {
                case IDLE -> handleIdleState(chat_id, message_text);
                case REGISTER_NAME -> handleRegisterName(chat_id, message_text);
                case REGISTER_SURNAME -> handleRegisterSurname(chat_id, message_text);
                case CREATING_TOURNAMENT_NAME -> handleTournamentName(chat_id, message_text);
                case CREATING_TOURNAMENT_DATE -> handleTournamentDate(chat_id, message_text);
                case CREATING_TOURNAMENT_CONFIRM -> handleTournamentConfirm(chat_id, message_text);
                default -> sendMessage(chat_id, "Unknown state. Please use /register or /create_tournament.");
            }
        }
    }

    private void handleIdleState(Long chatId, String text) {
        if (text.equalsIgnoreCase("/register")) {
            userStates.put(chatId, BotState.REGISTER_NAME);
            sendMessage(chatId, "Please enter your name:");
        } else if (text.equalsIgnoreCase("/create_tournament")) {
            tempTournamentData.put(chatId, new Tournament());
            userStates.put(chatId, BotState.CREATING_TOURNAMENT_NAME);
            sendMessage(chatId, "Enter tournament name:");
        } else {
            sendMessage(chatId, "Available commands: /register, /create_tournament, /join_tournament, /manage_tournament");
        }
    }

    private void handleRegisterName(Long chatId, String name) {
        User user = tempUserData.getOrDefault(chatId, new User());
        user.setTelegramId(chatId);
        user.setName(name);
        tempUserData.put(chatId, user);
        userStates.put(chatId, BotState.REGISTER_SURNAME);
        sendMessage(chatId, "Please enter your surname:");
    }

    private void handleRegisterSurname(Long chatId, String surname) {
        User user = tempUserData.get(chatId);
        if (user == null) {
            sendMessage(chatId, "Please start registration with /register.");
            userStates.put(chatId, BotState.IDLE);
            return;
        }
        user.setSurname(surname);
        user.setTag("user" + chatId); // Simple tag, can be improved
        userRepository.save(user);
        tempUserData.remove(chatId);
        userStates.put(chatId, BotState.IDLE);
        sendMessage(chatId, "Registration complete! Your tag: " + user.getTag());
    }

    private void handleTournamentName(Long chatId, String name) {
        Tournament t = tempTournamentData.get(chatId);
        t.setName(name);
        userStates.put(chatId, BotState.CREATING_TOURNAMENT_DATE);
        sendMessage(chatId, "Enter tournament date (YYYY-MM-DD):");
    }

    private void handleTournamentDate(Long chatId, String dateStr) {
        Tournament t = tempTournamentData.get(chatId);
        try {
            t.setDate(LocalDate.parse(dateStr));
            userStates.put(chatId, BotState.CREATING_TOURNAMENT_CONFIRM);
            sendMessage(chatId, "Confirm creation? (yes/no)");
        } catch (Exception e) {
            sendMessage(chatId, "Invalid date. Please enter date as YYYY-MM-DD:");
        }
    }

    private void handleTournamentConfirm(Long chatId, String confirm) {
        if (confirm.equalsIgnoreCase("yes")) {
            Tournament t = tempTournamentData.get(chatId);
            User owner = userRepository.findByTelegramId(chatId).orElse(null);
            if (owner == null) {
                sendMessage(chatId, "You must register first with /register.");
                userStates.put(chatId, BotState.IDLE);
                return;
            }
            t.setOwner(owner);
            tournamentRepository.save(t);
            tempTournamentData.remove(chatId);
            userStates.put(chatId, BotState.IDLE);
            sendMessage(chatId, "Tournament created! ID: " + t.getId());
        } else {
            tempTournamentData.remove(chatId);
            userStates.put(chatId, BotState.IDLE);
            sendMessage(chatId, "Tournament creation cancelled.");
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}