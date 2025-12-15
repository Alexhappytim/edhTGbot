package com.alexhappytim.mtg.dto;

import lombok.Data;

@Data
public class SubmitMatchResultRequest {
    private Long matchId;               // Used for edit-result
    private Integer scoreA;
    private Integer scoreB;
    private Long submitterTelegramId;   // Used for submit
    private Long adminId;               // Used for edit-result
}
