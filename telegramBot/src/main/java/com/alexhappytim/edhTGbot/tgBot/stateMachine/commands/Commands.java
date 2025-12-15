package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.casual.*;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.swiss.*;

import lombok.Getter;

@Getter
public enum Commands {
    // Registration
    REGISTER(new Register()),
    
    // Navigation
    JOIN_ANY_TOURNAMENT(new JoinAnyTournament()),
    SWITCH_TOURNAMENT(new SwitchTournament()),
    SWITCH_TOURNAMENT_MANUAL(new SwitchTournamentManual()),
    MANAGE_MY_TOURNAMENT(new ManageMyTournament()),
    
    // Swiss Tournament
    CREATE_TOURNAMENT(new CreateTournament()),
    JOIN_TOURNAMENT(new JoinTournament()),
    ADD_TO_TOURNAMENT(new AddToTournament()),
    STANDINGS(new Standings()),
    START_TOURNAMENT(new StartTournament()),
    KICK_FROM_TOURNAMENT(new KickFromTournament()),
    PAIRINGS(new Pairings()),
    SUBMIT_RESULT(new SubmitResult()),
    EDIT_RESULT(new EditResult()),
    START_NEXT_ROUND(new StartNextRound()),
    
    // Casual Tournament
    CREATE_CASUAL(new CreateCasual()),
    JOIN_CASUAL(new JoinCasual()),
    ADD_TO_CASUAL(new AddToCasual()),
    START_CASUAL(new StartCasual()),
    READY_CASUAL(new ReadyCasual()),
    ADMIN_READY(new AdminReady()),
    ADMIN_NOT_READY(new AdminNotReady()),
    READY_ALL(new ReadyAll()),
    READY_GROUP(new ReadyGroup()),
    RESHUFFLE_CASUAL(new ReshuffleCasual()),
    CASUAL_GROUPS(new CasualGroups()),
    CASUAL_READY_LIST(new CasualReadyList()),
    CASUAL_INFO(new CasualInfo());

    private final Command command;

    Commands(Command command) {
        this.command = command;
    }

    public static Command fromKey(String key) {
        if (key == null) return null;
        
        for (Commands type : values()) {
            if (type.command.getKey().equalsIgnoreCase(key)) {
                return type.command;
            }
        }
        return null;
    }
}
