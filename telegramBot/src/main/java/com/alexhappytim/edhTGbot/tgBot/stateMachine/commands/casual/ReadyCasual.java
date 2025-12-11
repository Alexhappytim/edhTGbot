package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ReadyCasual extends Command {

    public ReadyCasual() {
        super("ready_casual", 1, "tournament_admin_casual", "Введите ID казуал турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().info("User {} marking as ready in tournament {}", 
                update.getMessage().getFrom().getUserName(), tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/self-ready?userId=" + userId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("User {} marked as ready in tournament {}", 
                    update.getMessage().getFrom().getUserName(), tournamentId);
            bot.sendMessage(chatId, "Вы отмечены как готовый!");
        } catch (Exception e) {
            bot.getLogger().error("Ready failed for user {} in tournament {}: {}", 
                    update.getMessage().getFrom().getUserName(), tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
}
