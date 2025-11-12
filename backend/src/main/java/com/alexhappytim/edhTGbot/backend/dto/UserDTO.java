package com.alexhappytim.edhTGbot.backend.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String userTag;
    private Long telegramId;
    private String displayName;
}
