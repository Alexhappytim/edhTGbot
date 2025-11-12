package com.alexhappytim.mtg.dto;

import lombok.Data;

@Data
public class SubmitMatchResultRequest {
    private Long matchId;
    private Integer scoreA;
    private Integer scoreB;
}
