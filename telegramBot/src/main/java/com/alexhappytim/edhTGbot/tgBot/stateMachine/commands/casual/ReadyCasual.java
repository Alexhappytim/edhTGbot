package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ReadyCasual extends Command {

    public ReadyCasual() {
        super("ready_casual", 0, "tournament_casual", false);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        bot.getLogger().info("User {} marking as ready in tournament {}", 
                username, tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/self-ready?userId=" + userId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("User {} marked as ready in tournament {}", 
                    username, tournamentId);
            bot.sendMessage(chatId, "✅ Вы отмечены как готовый!");
        } catch (Exception e) {
            bot.getLogger().error("Ready failed for user {} in tournament {}: {}", 
                    username, tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
}
