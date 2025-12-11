package com.alexhappytim.mtg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinTournamentResponse {
    private String tournamentId;
    private String tournamentType;  // SWISS or CASUAL
    private boolean joined;
    private String message;
}
