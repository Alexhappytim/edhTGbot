package com.alexhappytim.mtg.dto;

import lombok.Data;
import java.util.List;

@Data
public class TournamentDTO {

    private String id;
    private String name;
    private int maxPlayers;
    private UserDTO owner;
    private String status;
    private List<ParticipantDTO> participants;
}
