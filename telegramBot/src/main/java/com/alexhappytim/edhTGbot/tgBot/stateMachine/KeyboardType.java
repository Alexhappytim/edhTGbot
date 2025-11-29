package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import com.alexhappytim.edhTGbot.tgBot.stateMachine.commands.CommandType;
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
            "Присоединиться к турниру:kbd:tournament_choosing",
            "Управление моими турнирами:kbd:tournament_admin_choosing",
            "Посмотреть инфо о турнире:kbd:tournament_info"
    )),
    
    TOURNAMENT_CREATING(KeyboardBuilder.build(
            "Выбери вариант турнира:",
            new String[][] {
                {"Классический:cmd:create_tournament", "Казуальный EDH:cmd:create_casual"},
                {"Назад:kbd:main"}
            }
    )),
    TOURNAMENT_CHOOSING(KeyboardBuilder.build(
            "Выбери вариант турнира:",
            new String[][] {
                    {"Классический:cmd:join_tournament", "Казуальный EDH:cmd:join_casual"},
                    {"Назад:kbd:main"}
            }
    )),
    TOURNAMENT_ADMIN_CHOOSING(KeyboardBuilder.build(
            "Выбери вариант турнира:",
            new String[][] {
        {"Классический:kbd:tournament_admin", "Казуальный EDH:kbd:tournament_admin_casual"},
        {"Назад:kbd:main"}
    }
    )),
    //TODO Сделать управление готовностями клавиатурой
    TOURNAMENT_ADMIN_CASUAL(KeyboardBuilder.build(
            "Что вы хотите сделать?",
            "Добавить игрока:cmd:add_to_casual",
            "Запустить турнир:cmd:start_casual",
            "Отметиться для шаффла:cmd:ready_casual",
            "⭐Отметить другого для шаффла:cmd:admin_ready",
            "⭐Отметить ВСЕХ для шаффла:cmd:ready_all",
            "⭐Отметить всех в группе для шаффла:cmd:ready_group",
            "⭐Убрать отметку о готовности другого игрока:cmd:admin_not_ready",
            "⭐Пошаффлить готовых:cmd:reshuffle_casual",
            "Посмотреть группы:cmd:casual_groups",
            "Посмотреть кто готов:cmd:casual_ready_list",
            "Инфо о турнире:cmd:casual_info",
            "Назад:kbd:main"
            )),
    TOURNAMENT_ADMIN(KeyboardBuilder.build(
            "Что вы хотите сделать?",
            "Добавить игрока:cmd:add_to_tournament",
            "Стендинги:cmd:standings",
            "Назад:kbd:main"

    ))
    ;
    private final KeyboardWrapper keyboard;

    KeyboardType(KeyboardWrapper keyboard) {
        this.keyboard = keyboard;
    }

    /**
     * Get keyboard by string key (for backward compatibility).
     */
    public static KeyboardType fromKey(String key) {
        if (key == null) return null;
        for (KeyboardType type : values()) {
            if (type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
//        return switch (key.toLowerCase()) {
//            case "main" -> MAIN;
//            case "tournament_creating" -> TOURNAMENT_CREATING;
//            default -> null;
//        };
    }
}
