package com.alexhappytim.edhTGbot.backend.dto;

import lombok.Data;

@Data
public class CreateTournamentRequest {
    private String name;
    private int maxPlayers;
}
