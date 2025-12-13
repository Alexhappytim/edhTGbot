package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.input.TournamentIdInputStrategy;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class JoinTournament extends Command {

    public JoinTournament() {
        super("join_tournament", "main", 
              new TournamentIdInputStrategy("Введите ID турнира"));
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        
        bot.getLogger().info("User {} joining tournament: {}", username, tournamentId);
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(userId, username, false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/join", 
                    entity, String.class);
            bot.getLogger().info("User {} joined tournament {} successfully", username, tournamentId);
            bot.sendMessage(chatId, "Вы присоединились к турниру! Ответ: " + response.getBody());
        } catch (Exception e) {
            bot.getLogger().error("Join tournament failed for user {}: {}", username, e.getMessage(), e);
            bot.sendMessage(chatId, "Ошибка присоединения: " + e.getMessage());
        }
    }
}
