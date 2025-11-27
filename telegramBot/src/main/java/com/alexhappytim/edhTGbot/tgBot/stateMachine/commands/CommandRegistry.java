package com.alexhappytim.edhTGbot.tgBot.stateMachine.commands;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry holding all commands by key.
 */
public class CommandRegistry {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public CommandRegistry() {
//        commands.put(command.getKey(), command);
    }

    public Command get(String key) { return commands.get(key); }

    public Collection<Command> getByGroup(CommandGroup group) {
        return commands.values().stream().filter(c -> c.getGroup() == group).collect(Collectors.toList());
    }
}
