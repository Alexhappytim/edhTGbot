package com.alexhappytim.edhTGbot.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class TournamentDTO {
    private Long id;
    private String name;
    private int maxPlayers;
    private String status;
    private List<ParticipantDTO> participants;
}
