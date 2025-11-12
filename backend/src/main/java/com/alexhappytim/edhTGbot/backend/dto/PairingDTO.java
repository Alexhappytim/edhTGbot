package com.alexhappytim.edhTGbot.backend.dto;

import lombok.Data;

@Data
public class PairingDTO {
    private Long matchId;
    private String playerA;
    private String playerB;
    private Integer scoreA;
    private Integer scoreB;
    private boolean completed;
}
