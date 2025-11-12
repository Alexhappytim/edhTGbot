package com.alexhappytim.edhTGbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "round_id")
    private Round round;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "player_a_id")
    private Participant playerA;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "player_b_id")
    private Participant playerB;

    private Integer scoreA;
    private Integer scoreB;

    private boolean completed;
}
