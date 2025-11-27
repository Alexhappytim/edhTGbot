package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.UserSession;

/**
 * Adapter giving commands controlled access to session data.
 */
public class UserSessionAdapter {
    private final UserSession delegate;
    public UserSessionAdapter(UserSession delegate) { this.delegate = delegate; }

    public UserSession getDelegate() { return delegate; }
    public Long tournamentId() { return delegate.getTournamentId(); }
    public void setTournamentId(Long id) { delegate.setTournamentId(id); }
}
