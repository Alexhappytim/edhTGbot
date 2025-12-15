package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.Keyboards;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ManageMyTournament extends Command {

    public ManageMyTournament() {
        super("manage_my_tournament", 0, "main", false);
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String tournamentId = bot.getSession(userId).getTournamentId();
        String tournamentType = bot.getSession(userId).getTournamentType();
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Вы не присоединены ни к какому турниру. Сначала создайте или присоединитесь к турниру.");
            return;
        }
        if (tournamentType == null){
            return;
        }
        // Edit message with appropriate admin keyboard based on tournament type
        if (tournamentType.equalsIgnoreCase("CASUAL")) {
            bot.editMessage(chatId, messageId,
                    Keyboards.TOURNAMENT_ADMIN_CASUAL.getKeyboard().getText(),
                    Keyboards.TOURNAMENT_ADMIN_CASUAL.getKeyboard().getKeyboard());
        } else {
            // Default to Swiss tournament admin
            bot.editMessage(chatId, messageId,
                    Keyboards.TOURNAMENT_ADMIN.getKeyboard().getText(),
                    Keyboards.TOURNAMENT_ADMIN.getKeyboard().getKeyboard());
        }
    }
}
