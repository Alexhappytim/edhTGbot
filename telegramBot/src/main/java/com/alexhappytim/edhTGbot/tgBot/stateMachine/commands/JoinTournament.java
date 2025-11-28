package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class JoinTournament extends Command {

    public JoinTournament() {
        super("jointournament", 1, "main", "Введите ID турнира");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String tournamentId = bot.getSession(userId).getInputs().get(0);
        String username = update.getMessage().getFrom().getUserName();
        
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
