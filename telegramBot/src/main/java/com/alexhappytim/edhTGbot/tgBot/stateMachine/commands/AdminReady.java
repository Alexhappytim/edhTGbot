package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AdminReady extends Command {

    public AdminReady() {
        super("adminready", 2, "main",
              "Введите ID казуал турнира", 
              "Введите ID пользователя");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(adminId).getInputs().get(0);
        String targetUserId = bot.getSession(adminId).getInputs().get(1);
        
        bot.getLogger().info("Admin {} marking user {} as ready in tournament {}", 
                update.getMessage().getFrom().getUserName(), targetUserId, tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/ready?userId=" + targetUserId + "&requesterId=" + adminId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("User {} marked as ready by admin {} in tournament {}", 
                    targetUserId, update.getMessage().getFrom().getUserName(), tournamentId);
            bot.sendMessage(chatId, "Пользователь " + targetUserId + " отмечен как готовый!");
        } catch (Exception e) {
            bot.getLogger().error("Admin ready failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
}
