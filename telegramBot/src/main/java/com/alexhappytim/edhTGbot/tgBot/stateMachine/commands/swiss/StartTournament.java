package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class StartTournament extends Command {

    public StartTournament() {
        super("start_tournament", 0, "tournament_admin", false);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        String username = getUsername(update);
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        bot.getLogger().info("User {} starting tournament {}", username, tournamentId);
        try {
                ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/start-round?requesterTelegramId=" + userId,
                    null, String.class);
            
            bot.getLogger().info("Tournament {} started successfully", tournamentId);
            bot.sendMessage(chatId, "✅ Турнир начат! Созданы первые пары.");
        } catch (Exception e) {
            bot.getLogger().error("Start tournament failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка начала турнира: " + e.getMessage());
        }
    }
}
