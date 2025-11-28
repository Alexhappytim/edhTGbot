package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ReadyCasual extends Command {

    public ReadyCasual() {
        super("readycasual", 1, "main", "Введите ID казуал турнира");
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
                         "/ready?userId=" + userId + "&requesterId=" + userId;
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
