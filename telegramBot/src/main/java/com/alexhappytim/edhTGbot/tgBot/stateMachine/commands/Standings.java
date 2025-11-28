package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class Standings extends Command {

    public Standings() {
        super("standings", 1, "main", "Введите ID турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().info("User {} requesting standings for tournament {}", 
                update.getMessage().getFrom().getUserName(), tournamentId);
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
