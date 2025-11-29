package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AdminNotReady extends Command {

    public AdminNotReady() {
        super("admin_not_ready", 2, "tournament_admin_casual",
              "Введите ID казуал турнира", 
              "Введите номер игрока в списке (используйте /casualinfo для просмотра)");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(adminId).getInputs().get(0);
        String playerPosition = bot.getSession(adminId).getInputs().get(1);
        
        bot.getLogger().info("Admin {} marking player #{} as not ready in tournament {}", 
                update.getMessage().getFrom().getUserName(), playerPosition, tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/not-ready?playerPosition=" + playerPosition + "&adminId=" + adminId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("Player #{} marked as not ready by admin {} in tournament {}", 
                    playerPosition, update.getMessage().getFrom().getUserName(), tournamentId);
            bot.sendMessage(chatId, "Игрок #" + playerPosition + " отмечен как не готовый!");
        } catch (Exception e) {
            bot.getLogger().error("Admin not ready failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
}
