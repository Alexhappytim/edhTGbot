package com.alexhappytim.edhTGbot.backend.dto;

import lombok.Data;

@Data
public class StandingDTO {
    private Long participantId;
    private String userTag;
    private int points;
    private int tieBreaker;
    private int rank;
}
