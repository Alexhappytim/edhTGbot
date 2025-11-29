package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import lombok.Getter;

/**
 * Enum containing all available commands.
 * Each enum constant creates its command instance in the constructor.
 */
@Getter
public enum CommandType {
    // Registration
    REGISTER(new Register()),
    
    // Swiss Tournament
    CREATE_TOURNAMENT(new CreateTournament()),
    JOIN_TOURNAMENT(new JoinTournament()),
    ADD_TO_TOURNAMENT(new AddToTournament()),
    STANDINGS(new Standings()),
    
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

    CommandType(Command command) {
        this.command = command;
    }

    /**
     * Get command by string key.
     */
    public static Command fromKey(String key) {
        if (key == null) return null;
        
        for (CommandType type : values()) {
            if (type.command.getKey().equalsIgnoreCase(key)) {
                return type.command;
            }
        }
        return null;
    }
}
