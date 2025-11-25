package com.alexhappytim.mtg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JoinTournamentRequest {
    private Long userId;
    private String participantName;
    private Boolean isTemporary;
}
