package com.alexhappytim.edhTGbot.backend.dto;

import lombok.Data;

@Data
public class ParticipantDTO {
    private Long id;
    private UserDTO user;
    private int points;
    private int tieBreaker;
}
