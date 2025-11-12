package com.alexhappytim.edhTGbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userTag;
    @Column(unique = true, nullable = false)
    private Long telegramId;
    @Column(nullable = false)
    private String displayName;
}
