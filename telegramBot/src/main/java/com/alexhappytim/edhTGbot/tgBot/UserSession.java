package com.alexhappytim.edhTGbot.tgBot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
public class UserSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String tournamentId;
    private String tournamentType;
    private String pendingCommandKey;
    private java.util.List<String> inputs;
    private Integer inputStep;

    private Map<String, String> joinedTournaments;

    public UserSession(String tournamentId, String tournamentType, String pendingCommandKey, 
                      java.util.List<String> inputs, Integer inputStep) {
        this.tournamentId = tournamentId;
        this.tournamentType = tournamentType;
        this.pendingCommandKey = pendingCommandKey;
        this.inputs = inputs;
        this.inputStep = inputStep;
        this.joinedTournaments = new HashMap<>();
    }

    public void addTournament(String tournamentId, String tournamentType) {
        if (joinedTournaments == null) {
            joinedTournaments = new HashMap<>();
        }
        joinedTournaments.put(tournamentId, tournamentType);
    }

    public void setCurrentTournament(String tournamentId) {
        if (joinedTournaments != null && joinedTournaments.containsKey(tournamentId)) {
            this.tournamentId = tournamentId;
            this.tournamentType = joinedTournaments.get(tournamentId);
        }
    }
}
