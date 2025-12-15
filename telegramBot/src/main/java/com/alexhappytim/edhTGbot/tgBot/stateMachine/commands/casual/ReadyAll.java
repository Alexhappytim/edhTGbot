package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ReadyAll extends Command {

    public ReadyAll() {
        super("ready_all", 0, "tournament_admin_casual", false);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(adminId).getTournamentId();
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        bot.getLogger().info("Admin {} marking all players as ready in tournament {}", 
                username, tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/ready-all?adminId=" + adminId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("All players marked as ready by admin {} in tournament {}", 
                    username, tournamentId);
            bot.sendMessage(chatId, "✅ Все игроки отмечены как готовые!");
        } catch (Exception e) {
            bot.getLogger().error("Mark all ready failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
}
