package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import lombok.Getter;

/**
 * Enum containing all available keyboards.
 * Each enum constant holds its keyboard definition.
 */
@Getter
public enum KeyboardType {
    MAIN(KeyboardBuilder.build(
            "Выбери действие:",
            "Зарегистрироваться:cmd:register",
            "Создать турнир:kbd:tournament_creating",
            "Присоединиться к турниру:kbd:tournament_join",
            "Управление моими турнирами:kbd:tournament_admin",
            "Посмотреть инфо о турнире:kbd:tournament_info"
    )),
    
    TOURNAMENT_CREATING(KeyboardBuilder.build(
            "Выбери вариант турнира:",
            new String[][] {
                {"Классический:cmd:createtournament", "Казуальный EDH:cmd:createcasual"},
                {"Назад:kbd:main"}
            }
    ));

    private final KeyboardWrapper keyboard;

    KeyboardType(KeyboardWrapper keyboard) {
        this.keyboard = keyboard;
    }

    /**
     * Get keyboard by string key (for backward compatibility).
     */
    public static KeyboardType fromKey(String key) {
        if (key == null) return null;
        
        return switch (key.toLowerCase()) {
            case "main" -> MAIN;
            case "tournament_creating" -> TOURNAMENT_CREATING;
            default -> null;
        };
    }
}
