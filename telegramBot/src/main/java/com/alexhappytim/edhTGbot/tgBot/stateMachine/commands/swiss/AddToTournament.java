package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.SimpleInputStrategy;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AddToTournament extends Command {

    public AddToTournament() {
        super("add_to_tournament", "main", 
              new SimpleInputStrategy("Введите имя игрока для добавления"));
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        String displayName = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().info("User {} adding temporary user {} to tournament {}", 
                username, displayName, tournamentId);
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
