package com.alexhappytim.edhTGbot.tgBot;

public interface SessionProvider {
    UserSession getSession(long userId);
    void setSession(long userId, UserSession session);
    void promptSelectTournamentType(long chatId);
    void promptEnterTournamentId(long chatId);
}
