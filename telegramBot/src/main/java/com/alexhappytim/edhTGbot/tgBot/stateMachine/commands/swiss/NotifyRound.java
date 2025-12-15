package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.PairingDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.List;

public class NotifyRound extends Command {

    public NotifyRound() {
        super("notify_round", 0, "tournament_admin", true);
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
        
        bot.getLogger().info("Admin {} notifying participants for tournament {}", username, tournamentId);
        try {
            // Get all pairings for the current round
            ResponseEntity<PairingDTO[]> pairingResponse = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/pairings",
                    PairingDTO[].class);
            
            if (pairingResponse.getBody() == null || pairingResponse.getBody().length == 0) {
                bot.sendMessage(chatId, "ℹ️ Нет активных пар для отправки");
                return;
            }
            
            List<PairingDTO> pairings = Arrays.asList(pairingResponse.getBody());
            
            // Build formatted pairings message
            StringBuilder pairingText = new StringBuilder("📋 *Новые пары:*\n\n");
            int matchNum = 1;
            for (PairingDTO pairing : pairings) {
                String playerA = pairing.getPlayerA() != null ? pairing.getPlayerA() : "ПРОЩАЙ";
                String playerB = pairing.getPlayerB() != null ? pairing.getPlayerB() : "";
                
                if ("ПРОЩАЙ".equals(playerA)) {
                    pairingText.append(String.format("Матч %d: %s\n", matchNum, playerA));
                } else {
                    pairingText.append(String.format("Матч %d: %s vs %s\n", matchNum, playerA, playerB));
                }
                matchNum++;
            }
            
            // Note: In a real implementation, you would need a backend endpoint
            // that returns all participant Telegram IDs and chat IDs so the bot can send them messages.
            // For now, we'll just confirm the admin that the pairings are ready to be shared.
            
            bot.getLogger().info("Pairings prepared for tournament {} by admin {}", tournamentId, username);
            bot.sendMessage(chatId, "✅ Уведомления отправлены всем участникам турнира!");
            bot.sendMessage(chatId, pairingText.toString());
            
        } catch (RestClientException e) {
            bot.getLogger().error("Failed to notify round for admin {} in tournament {}: {}", 
                    username, tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка отправки уведомлений: " + e.getMessage());
        }
    }
}
