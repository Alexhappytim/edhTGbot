package com.alexhappytim.edhTGbot.tgBot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserSession {
    private Long tournamentId;
    private String pendingCommandKey;
    private java.util.List<String> inputs;
    private Integer inputStep;

}
