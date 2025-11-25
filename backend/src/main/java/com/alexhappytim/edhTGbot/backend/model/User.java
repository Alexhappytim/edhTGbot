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
    private String userTag;
    private Long telegramId;
    @Column(nullable = false)
    private String displayName;
    private Long chatId;
}
