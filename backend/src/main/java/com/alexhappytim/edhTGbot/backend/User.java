package com.alexhappytim.edhTGbot.backend;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long telegramId;

    @Column(unique = true, nullable = false)
    private String tag;

    private String name;
    private String surname;
}
