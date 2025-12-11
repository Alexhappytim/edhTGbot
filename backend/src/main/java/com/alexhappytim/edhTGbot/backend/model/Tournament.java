package com.alexhappytim.edhTGbot.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@GenericGenerator(
        name = "code8-id",
        strategy = "com.alexhappytim.edhTGbot.backend.model.TournamentCodeIDGenerator"
)
public class Tournament {
    @Id
    @GeneratedValue(generator = "code8-id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentType type;
}

