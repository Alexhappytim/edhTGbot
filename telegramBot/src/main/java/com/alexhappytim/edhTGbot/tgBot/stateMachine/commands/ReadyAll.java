package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ReadyAll extends Command {

    public ReadyAll() {
        super("ready_all", 1, "tournament_admin_casual", "Введите ID казуал турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(adminId).getInputs().get(0);
        
        bot.getLogger().info("Admin {} marking all players as ready in tournament {}", 
                update.getMessage().getFrom().getUserName(), tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/ready-all?adminId=" + adminId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("All players marked as ready by admin {} in tournament {}", 
                    update.getMessage().getFrom().getUserName(), tournamentId);
            bot.sendMessage(chatId, "✅ Все игроки отмечены как готовые!");
        } catch (Exception e) {
            bot.getLogger().error("Mark all ready failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
}
