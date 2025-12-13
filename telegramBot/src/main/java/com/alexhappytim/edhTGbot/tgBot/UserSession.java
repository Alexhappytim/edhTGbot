package com.alexhappytim.edhTGbot.tgBot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserSession {
    private String tournamentId;
    private String pendingCommandKey;
    private java.util.List<String> inputs;
    private Integer inputStep;

}
