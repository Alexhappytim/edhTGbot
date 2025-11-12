package com.alexhappytim.mtg.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String userTag;
    private Long telegramId;
    private String displayName;
    private Long chatId;
}
