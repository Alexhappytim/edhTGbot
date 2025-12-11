package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AddToTournament extends Command {

    public AddToTournament() {
        super("add_to_tournament", 2, "main",
              "Введите ID турнира", 
              "Введите имя игрока для добавления");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        String displayName = bot.getSession(userId).getInputs().get(1);
        
        bot.getLogger().info("User {} adding temporary user {} to tournament {}", 
                update.getMessage().getFrom().getUserName(), displayName, tournamentId);
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(userId, displayName, true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/join", 
                    entity, String.class);
            bot.getLogger().info("Temporary user {} added to tournament {} successfully", displayName, tournamentId);
            bot.sendMessage(chatId, "Игрок добавлен в турнир! Ответ: " + response.getBody());
        } catch (Exception e) {
            bot.getLogger().error("Add to tournament failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка добавления: " + e.getMessage());
        }
    }
}
