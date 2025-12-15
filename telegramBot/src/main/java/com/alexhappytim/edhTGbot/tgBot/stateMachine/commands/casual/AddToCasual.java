package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.JoinTournamentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AddToCasual extends Command {

    public AddToCasual() {
        super("add_to_casual", 1,"tournament_admin_casual", true,
              "Введите имя игрока");
    }

    @Override
    public void execute(BotFacade bot, Update update) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        String username = getUsername(update);
        String tournamentId = bot.getSession(userId).getTournamentId();
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            bot.sendMessage(chatId, "❌ Ошибка: вы не присоединены ни к какому турниру");
            return;
        }
        
        String displayName = bot.getSession(userId).getInputs().get(0);
        
        bot.getLogger().info("User {} adding temporary user {} to casual tournament {}", 
                username, displayName, tournamentId);
        try {
            JoinTournamentRequest request = new JoinTournamentRequest(userId, displayName, true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JoinTournamentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournamentsCasual/" + tournamentId + "/join", 
                    entity, String.class);
            JsonNode node = bot.getObjectMapper().readTree(response.getBody());
            bot.getLogger().info("Temporary user {} added to casual tournament {}", displayName, tournamentId);
            bot.sendMessage(chatId, "✅ Добавлен временный игрок: " + node.get("displayName").asText());
        } catch (Exception e) {
            bot.getLogger().error("Add to casual tournament failed: {}", e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка добавления: " + e.getMessage());
        }
    }
}
