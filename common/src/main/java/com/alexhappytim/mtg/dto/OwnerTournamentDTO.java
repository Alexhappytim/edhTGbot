package com.alexhappytim.mtg.dto;

import lombok.Data;

@Data
public class OwnerTournamentDTO {
    private String id;
    private String name;
    private String type; // SWISS or CASUAL
}
