package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AdminNotReady extends Command {

    public AdminNotReady() {
        super("admin_not_ready", 1, "tournament_admin_casual",true,
              "Введите номер игрока в списке (используйте /casualinfo для просмотра)");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(adminId).getTournamentId();
        String playerPosition = bot.getSession(adminId).getInputs().get(0);
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        bot.getLogger().info("Admin {} marking player #{} as not ready in tournament {}", 
                username, playerPosition, tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/not-ready?playerPosition=" + playerPosition + "&adminId=" + adminId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("Player #{} marked as not ready by admin {} in tournament {}", 
                    playerPosition, username, tournamentId);
            bot.sendMessage(chatId, "✅ Игрок #" + playerPosition + " отмечен как не готовый!");
        } catch (Exception e) {
            bot.getLogger().error("Admin not ready failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
}
