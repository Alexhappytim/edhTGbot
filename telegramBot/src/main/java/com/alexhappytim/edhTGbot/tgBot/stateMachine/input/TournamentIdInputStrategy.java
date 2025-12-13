package com.alexhappytim.edhTGbot.tgBot.stateMachine.input;

import com.alexhappytim.edhTGbot.tgBot.BotFacade;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Input strategy for tournament ID that:
 * 1. Saves the tournament ID to session when collected (so it can be reused for other commands)
 * 2. Allows skipping input if tournament ID is already in session
 */
public class TournamentIdInputStrategy implements InputStrategy {
    private final String prompt;

    public TournamentIdInputStrategy(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    @Override
    public void collectInput(BotFacade bot, Update update) throws Exception {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            throw new IllegalArgumentException("Update does not contain a message with text");
        }
        
        long userId = update.getMessage().getFrom().getId();
        String tournamentId = update.getMessage().getText().trim();
        
        if (tournamentId.isBlank()) {
            throw new IllegalArgumentException("Tournament ID cannot be empty");
        }
        
        // Save tournament ID to session for future use
        bot.getSession(userId).setTournamentId(tournamentId);
    }

    @Override
    public boolean isPreconditionMet(BotFacade bot, Update update) {
        long userId;
        
        // Handle both Message and CallbackQuery updates
        if (update.hasMessage()) {
            userId = update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            userId = update.getCallbackQuery().getFrom().getId();
        } else {
            return false;
        }
        
        String tournamentId = bot.getSession(userId).getTournamentId();
        
        // Precondition is met if tournament ID is already in session
        return tournamentId != null && !tournamentId.isEmpty();
    }
}
