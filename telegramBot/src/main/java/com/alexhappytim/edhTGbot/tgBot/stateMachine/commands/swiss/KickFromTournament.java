package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.Keyboards;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.Command;
import com.alexhappytim.mtg.dto.ParticipantDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KickFromTournament extends Command {

    public KickFromTournament() {
        super("kick_from_tournament", 0, "tournament_admin", false);
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
        
        bot.getLogger().info("Admin {} fetching participants for tournament {}", username, tournamentId);
        try {

            ResponseEntity<ParticipantDTO[]> response = bot.getRestTemplate().getForEntity(
                    bot.getRestBaseUrl() + "/tournaments/" + tournamentId + "/swiss/participants",
                    ParticipantDTO[].class);
            
            if (response.getBody() == null || response.getBody().length == 0) {
                bot.sendMessage(chatId, "ℹ️ В турнире нет участников");
                return;
            }
            
            List<ParticipantDTO> participants = Arrays.asList(response.getBody());
            showParticipantsPage(bot, chatId, participants, 0, tournamentId, null);
            
        } catch (Exception e) {
            bot.getLogger().error("Failed to fetch participants for admin {} in tournament {}: {}", 
                    username, tournamentId, e.getMessage(), e);
            bot.sendMessage(chatId, "❌ Ошибка загрузки участников: " + e.getMessage());
        }
    }

    public void showParticipantsPage(BotFacade bot, long chatId, List<ParticipantDTO> participants, 
                                      int pageNum, String tournamentId, Integer messageId) {
        int pageSize = 10;
        int startIdx = pageNum * pageSize;
        int endIdx = Math.min(startIdx + pageSize, participants.size());
        int totalPages = (int) Math.ceil((double) participants.size() / pageSize);
        
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        for (int i = startIdx; i < endIdx; i++) {
            ParticipantDTO p = participants.get(i);
            String displayName = p.getUser().getDisplayName();
            String userTag = p.getUser().getUserTag();
            String buttonText = displayName;
            if (userTag != null && !userTag.isEmpty()) {
                buttonText += " (@" + userTag + ")";
            }
            

            Long userId = p.getUser().getTelegramId() == -1L ? p.getUser().getId() : p.getUser().getTelegramId();
            
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(buttonText)
                    .callbackData("kick_user:" + tournamentId + ":" + userId)
                    .build();
            buttons.add(btn);
        }

        List<InlineKeyboardButton> paginationRow = new ArrayList<>();
        if (pageNum > 0) {
            InlineKeyboardButton prevBtn = InlineKeyboardButton.builder()
                    .text("⬅️ Предыдущая")
                    .callbackData("kick_page:" + tournamentId + ":" + (pageNum - 1))
                    .build();
            paginationRow.add(prevBtn);
        }
        
        if (pageNum < totalPages - 1) {
            InlineKeyboardButton nextBtn = InlineKeyboardButton.builder()
                    .text("Следующая ➡️")
                    .callbackData("kick_page:" + tournamentId + ":" + (pageNum + 1))
                    .build();
            paginationRow.add(nextBtn);
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (InlineKeyboardButton btn : buttons) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        if (!paginationRow.isEmpty()) {
            InlineKeyboardRow paginationRowObj = new InlineKeyboardRow();
            paginationRowObj.addAll(paginationRow);
            rows.add(paginationRowObj);
        }
        
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
        
        String message = String.format("📋 Выберите игрока для удаления (Страница %d из %d):", pageNum + 1, totalPages);

        if (messageId != null) {
            bot.editMessage(chatId, messageId, message, markup);
        } else {
            bot.sendMessage(chatId, message, markup);
        }
    }

    @Override
    public boolean handleCallback(BotFacade bot, Update update, String callbackData) {
        long userId = getUserId(update);
        long chatId = getChatId(update);
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String tournamentId = bot.getSession(userId).getTournamentId();

        if (callbackData.startsWith("kick_user:")) {
            String[] parts = callbackData.split(":");
            if (parts.length == 3) {
                String tournamentIdFromCallback = parts[1];
                String userIdStr = parts[2];
                try {
                    long playerId = Long.parseLong(userIdStr);
                    bot.getLogger().info("Admin {} kicking user {} from tournament {}", userId, playerId, tournamentIdFromCallback);
                    bot.getRestTemplate().postForEntity(
                            bot.getRestBaseUrl() + "/tournaments/" + tournamentIdFromCallback + "/swiss/remove-participant?userId=" + playerId + "&requesterTelegramId=" + userId,
                            null, Void.class);

                    bot.editMessage(chatId, messageId, 
                            Keyboards.TOURNAMENT_ADMIN.getKeyboard().getText(),
                            Keyboards.TOURNAMENT_ADMIN.getKeyboard().getKeyboard());
                    bot.sendMessage(chatId, "✅ Игрок успешно удален из турнира.");
                } catch (Exception e) {
                    bot.getLogger().error("Failed to kick user from tournament: {}", e.getMessage(), e);
                    bot.sendMessage(chatId, "❌ Ошибка удаления: " + e.getMessage());
                }
            }
            return true;
        }

        if (callbackData.startsWith("kick_page:")) {
            String[] parts = callbackData.split(":");
            if (parts.length == 3) {
                String tournamentIdFromCallback = parts[1];
                int pageNum = Integer.parseInt(parts[2]);
                try {
                    ResponseEntity<ParticipantDTO[]> response = bot.getRestTemplate().getForEntity(
                            bot.getRestBaseUrl() + "/tournaments/" + tournamentIdFromCallback + "/swiss/participants",
                            ParticipantDTO[].class);
                    
                    if (response.getBody() != null) {
                        List<ParticipantDTO> participants = Arrays.asList(response.getBody());
                        showParticipantsPage(bot, chatId, participants, pageNum, tournamentIdFromCallback, messageId);
                    }
                } catch (Exception e) {
                    bot.getLogger().error("Failed to load kick page: {}", e.getMessage(), e);
                    bot.sendMessage(chatId, "❌ Ошибка загрузки страницы: " + e.getMessage());
                }
            }
            return true;
        }

        return false;
    }
}
