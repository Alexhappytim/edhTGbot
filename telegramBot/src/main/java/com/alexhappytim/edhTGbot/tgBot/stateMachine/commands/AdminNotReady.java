package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AdminNotReady extends Command {

    public AdminNotReady() {
        super("adminnotready", 2, "main",
              "Введите ID казуал турнира", 
              "Введите ID пользователя");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(adminId).getInputs().get(0);
        String targetUserId = bot.getSession(adminId).getInputs().get(1);
        
        bot.getLogger().info("Admin {} marking user {} as not ready in tournament {}", 
                update.getMessage().getFrom().getUserName(), targetUserId, tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/not-ready?userId=" + targetUserId + "&adminId=" + adminId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("User {} marked as not ready by admin {} in tournament {}", 
                    targetUserId, update.getMessage().getFrom().getUserName(), tournamentId);
            bot.sendMessage(chatId, "Пользователь " + targetUserId + " отмечен как не готовый!");
        } catch (Exception e) {
            bot.getLogger().error("Admin not ready failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
}
