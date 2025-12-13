package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.TournamentIdInputStrategy;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ReadyAll extends Command {

    public ReadyAll() {
        super("ready_all", "tournament_admin_casual", 
              new TournamentIdInputStrategy("Введите ID казуал турнира"));
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(adminId).getInputs().get(0);
        
        bot.getLogger().info("Admin {} marking all players as ready in tournament {}", 
                username, tournamentId);
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
