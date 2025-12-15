package com.alexhappytim.mtg.dto;

import lombok.Data;

@Data
public class PairingDTO {
    private Long matchId;
    private String playerA;
    private String playerADisplayName;
    private String playerB;
    private String playerBDisplayName;
    private Integer scoreA;
    private Integer scoreB;
    private boolean completed;
}
