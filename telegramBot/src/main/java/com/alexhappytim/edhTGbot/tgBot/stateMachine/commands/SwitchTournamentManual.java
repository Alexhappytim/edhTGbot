package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.UserSession;
import org.telegram.telegrambots.meta.api.objects.Update;

public class SwitchTournamentManual extends Command {

    public SwitchTournamentManual() {
        super(
            "switch_tournament_manual",
            2,
            "main",
            false,
            "Введите ID турнира",
            "Введите тип турнира (CASUAL или SWISS)"
        );
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        UserSession session = bot.getSession(userId);
        if (session == null) {
            session = new UserSession(null, null, null, null, null);
        }
        String tournamentId = session.getInputs().get(0).trim();
        String typeInput = session.getInputs().get(1).trim();
        String normalizedType = typeInput.equalsIgnoreCase("CASUAL") ? "CASUAL" :
                                 typeInput.equalsIgnoreCase("SWISS") ? "SWISS" : null;

        if (normalizedType == null || tournamentId.isEmpty()) {
            bot.getLogger().warn("Manual switch validation failed for user {}: id='{}', type='{}'", userId, tournamentId, typeInput);
            bot.sendMessage(getChatId(update), "❌ Неверные данные. Тип должен быть CASUAL или SWISS, ID не должен быть пустым.");
            return;
        }

        session.addTournament(tournamentId, normalizedType);
        session.setCurrentTournament(tournamentId);
        bot.setSession(userId, session);

        bot.getLogger().info("User {} manually switched to tournament {} ({})", userId, tournamentId, normalizedType);
        // MagicBot will show the next keyboard (main) automatically after execution
    }
}
