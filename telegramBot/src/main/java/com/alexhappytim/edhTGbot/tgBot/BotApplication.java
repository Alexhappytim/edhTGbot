package com.alexhappytim.edhTGbot.tgBot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotApplication {
    public static void main(String[] args) {
        String botToken = System.getenv("TELEGRAM_TOKEN");
        String botUsername = System.getenv("TELEGRAM_BOT_USERNAME");
        String restBaseUrl = System.getenv().getOrDefault("REST_BASE_URL", "http://localhost:8080");
        Long adminID = Long.parseLong(System.getenv("ADMIN_ID"));
        MagicBot bot = new MagicBot(botToken, botUsername, restBaseUrl, adminID);
        try {
            TelegramBotsLongPollingApplication app = new TelegramBotsLongPollingApplication();
            app.registerBot(botToken, bot);
        } catch (TelegramApiException e){
        }
    }
}