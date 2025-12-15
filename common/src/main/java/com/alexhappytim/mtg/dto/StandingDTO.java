package com.alexhappytim.mtg.dto;

import lombok.Data;

@Data
public class StandingDTO {
    private Long participantId;
    private String userTag;
    private String displayName;
    private int points;
    private int tieBreaker;
    private int rank;
    
    // Detailed tiebreaker percentages
    private double omwPercentage;      // Opponents' Match Win Percentage
    private double gwPercentage;       // Game Win Percentage
    private double ogwPercentage;      // Opponents' Game Win Percentage
}
