package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.TournamentIdInputStrategy;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class Standings extends Command {

    public Standings() {
        super("standings", "main", 
              new TournamentIdInputStrategy("Введите ID турнира"));
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        
        bot.getLogger().info("User {} requesting standings for tournament {}", 
                username, tournamentId);
        try {
            ResponseEntity<String> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/standings", String.class);
            bot.getLogger().debug("Standings retrieved for tournament {}", tournamentId);
            bot.sendMessage(chatId, "Таблица результатов: " + response.getBody());
        } catch (Exception e) {
            bot.getLogger().error("Failed to get standings for tournament {}: {}", tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка получения таблицы: " + e.getMessage());
        }
    }
}
