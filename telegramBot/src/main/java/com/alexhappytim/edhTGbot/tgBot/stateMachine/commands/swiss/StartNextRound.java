package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.PairingDTO;
import com.alexhappytim.mtg.dto.ParticipantDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartNextRound extends Command {

    public StartNextRound() {
        super("start_next_round", 0, "tournament_admin", true);
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
        
        bot.getLogger().info("Admin {} starting next round for tournament {}", username, tournamentId);
        try {
            ResponseEntity<Void> response = bot.getRestTemplate().postForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/swiss/next-round?requesterTelegramId=" + userId,
                    null, Void.class);
            
            bot.getLogger().info("Next round started for tournament {} by admin {}", tournamentId, username);

            try {
                ResponseEntity<String> pairingsResponse = bot.getRestTemplate().getForEntity(
                        bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/swiss/pairings",
                        String.class);
                ResponseEntity<ParticipantDTO[]> participantsResponse = bot.getRestTemplate().getForEntity(
                        bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/swiss/participants",
                        ParticipantDTO[].class);
                
                JsonNode pairingsJson = bot.getObjectMapper().readTree(pairingsResponse.getBody());
                List<ParticipantDTO> participants = Arrays.asList(participantsResponse.getBody());

                Map<Long, ParticipantDTO> participantMap = new HashMap<>();
                for (ParticipantDTO p : participants) {
                    if (p.getUser() != null && p.getUser().getTelegramId() != null && p.getUser().getTelegramId() > 0) {
                        participantMap.put(p.getUser().getTelegramId(), p);
                    }
                }
                pairingsJson.forEach(pairing -> {
                    String playerATag = pairing.get("playerA").asText();
                    String playerADisplayName = pairing.has("playerADisplayName") ? pairing.get("playerADisplayName").asText() : playerATag;
                    String playerBTag = pairing.has("playerB") && !pairing.get("playerB").isNull() ? pairing.get("playerB").asText() : null;
                    String playerBDisplayName = pairing.has("playerBDisplayName") && !pairing.get("playerBDisplayName").isNull() ? pairing.get("playerBDisplayName").asText() : playerBTag;

                    for (ParticipantDTO p : participants) {
                        if (p.getUser() != null && p.getUser().getTelegramId() != null && p.getUser().getTelegramId() > 0) {
                            String userTag = p.getUser().getUserTag();
                            String displayName = p.getUser().getDisplayName();
                            Long chatId_participant = p.getUser().getChatId();

                            if ((playerATag.equals(userTag) || playerADisplayName.equals(displayName)) && playerBTag != null) {
                                String opponentDisplay = playerBDisplayName;
                                String notification = String.format("🎮 *Новый раунд!*\n\n" +
                                        "Ваш противник: *%s*\n" +
                                        "Удачи в игре!", opponentDisplay);
                                bot.sendMessage(chatId_participant, notification);
                            } else if (playerBTag != null && (playerBTag.equals(userTag) || playerBDisplayName.equals(displayName))) {
                                String opponentDisplay = playerADisplayName;
                                String notification = String.format("🎮 *Новый раунд!*\n\n" +
                                        "Ваш противник: *%s*\n" +
                                        "Удачи в игре!", opponentDisplay);
                                bot.sendMessage(chatId_participant, notification);
                            } else if (playerBTag == null && (playerATag.equals(userTag) || playerADisplayName.equals(displayName))) {
                                bot.sendMessage(chatId_participant, "👻 *Новый раунд!*\n\nВы получили фрау (bye). Вам начисляется 1-0!");
                            }
                        }
                    }
                });
                
                bot.sendMessage(chatId, "✅ Следующий раунд начат! Уведомления отправлены всем участникам.");
            } catch (Exception notifyEx) {
                bot.getLogger().warn("Could not send notifications for tournament {}: {}", tournamentId, notifyEx.getMessage());
                bot.sendMessage(chatId, "✅ Раунд начат, но не удалось отправить уведомления участникам.");
            }
        } catch (Exception e) {
            bot.getLogger().error("Start next round failed for admin {} in tournament {}: {}", 
                    username, tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка запуска следующего раунда: " + e.getMessage());
        }
    }
}
