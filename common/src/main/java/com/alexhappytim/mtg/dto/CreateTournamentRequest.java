package com.alexhappytim.mtg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTournamentRequest {
    private String name;
    private int maxPlayers;
    private long ownerId;
}
