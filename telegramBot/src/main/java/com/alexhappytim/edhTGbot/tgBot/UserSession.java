package com.alexhappytim.edhTGbot.tgBot;

import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.CommandGroup;
import com.alexhappytim.edhTGbot.tgBot.stateMachine.StateType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSession {
    private Long tournamentId;
    private String pendingCommandKey;
    private String input;


}
