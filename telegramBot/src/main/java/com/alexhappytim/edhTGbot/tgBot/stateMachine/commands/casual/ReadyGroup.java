package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ReadyGroup extends Command {

    public ReadyGroup() {
        super("ready_group", 1, "tournament_admin_casual",true,
              "Введите номер группы (используйте /casualgroups для просмотра)");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long adminId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(adminId).getTournamentId();
        String groupNumber = bot.getSession(adminId).getInputs().get(0);
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        bot.getLogger().info("Admin {} marking group #{} as ready in tournament {}", 
                username, groupNumber, tournamentId);
        try {
            String url = bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + 
                         "/ready-group?groupNumber=" + groupNumber + "&adminId=" + adminId;
            bot.getRestTemplate().postForEntity(url, null, String.class);
            bot.getLogger().info("Group #{} marked as ready by admin {} in tournament {}", 
                    groupNumber, username, tournamentId);
            bot.sendMessage(chatId, "✅ Группа #" + groupNumber + " отмечена как готовая!");
        } catch (Exception e) {
            bot.getLogger().error("Mark group ready failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
}
