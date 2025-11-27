package com.alexhappytim.edhTGbot.tgBot;

import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.CommandGroup;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.StateType;

public class UserSession {
    public enum TournamentType {SWISS, CASUAL}

    private TournamentType type;
    private Long tournamentId;
    private String awaitingInputFor; // legacy tournament id capture
    private StateType state = StateType.MAIN_MENU;
    private CommandGroup currentGroup;
    private String pendingCommandKey;

    public TournamentType getType() {
        return type;
    }

    public void setType(TournamentType type) {
        this.type = type;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getAwaitingInputFor() {
        return awaitingInputFor;
    }

    public void setAwaitingInputFor(String awaitingInputFor) {
        this.awaitingInputFor = awaitingInputFor;
    }

    public StateType getState() {
        return state;
    }

    public void setState(StateType state) {
        this.state = state;
    }

    public CommandGroup getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(CommandGroup currentGroup) {
        this.currentGroup = currentGroup;
    }

    public String getPendingCommandKey() {
        return pendingCommandKey;
    }

    public void setPendingCommandKey(String pendingCommandKey) {
        this.pendingCommandKey = pendingCommandKey;
    }
}
