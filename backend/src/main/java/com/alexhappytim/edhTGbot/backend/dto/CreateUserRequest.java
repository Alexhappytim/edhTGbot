package com.alexhappytim.edhTGbot.backend.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String userTag;
    private String displayName;
    private Long telegramId;
}
