package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class Unjoin extends Command {

    public Unjoin() {
        super("unjoin", 0, "main", false);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        if (!isMessage(update)) {
            bot.getLogger().warn("Unjoin command requires message update, got callback query");
            return;
        }

        long userId = getUserId(update);
        long chatId = getChatId(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        bot.getLogger().info("User {} unjoining tournament: {}", getUsername(update), tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/unjoin?userId=" + userId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            
            // Clear session
            bot.getSession(userId).setTournamentId(null);
            bot.getSession(userId).setTournamentType(null);
            
            bot.getLogger().info("User {} unjoined tournament {} successfully", getUsername(update), tournamentId);
            bot.sendMessage(chatId, "✅ Вы вышли из турнира");
        } catch (Exception e) {
            bot.getLogger().error("Unjoin tournament failed for user {}: {}", getUsername(update), e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка выхода из турнира: " + e.getMessage());
        }
    }
}
