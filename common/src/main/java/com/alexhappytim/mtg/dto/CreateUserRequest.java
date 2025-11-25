package com.alexhappytim.mtg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {
    private String userTag;
    private String displayName;
    private Long telegramId;
    private Long chatId;
}
