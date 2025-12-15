package com.alexhappytim.edhTGbot.tgBot.stateMachine;

import lombok.Getter;

@Getter
public enum Keyboards {
    MAIN(KeyboardBuilder.build(
            "Выбери действие:",
            "Зарегистрироваться:cmd:register",
            "Создать турнир:kbd:tournament_creating",
            "Присоединиться к турниру:cmd:join_any_tournament",
            "Переключиться на турнир:cmd:switch_tournament",
            "Управление моим турниром:cmd:manage_my_tournament",
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
            "⭐Запустить турнир (первые пары):cmd:start_tournament",
            "⭐Удалить игрока:cmd:kick_from_tournament",
            "Посмотреть пары:cmd:pairings",
            "Отправить результат матча:cmd:submit_result",
            "⭐Отредактировать результат:cmd:edit_result",
            "⭐Начать следующий раунд:cmd:start_next_round",
            "⭐Отправить уведомления о парах:cmd:notify_round",
            "Назад:kbd:main"

    ))
    ;
    private final KeyboardWrapper keyboard;

    Keyboards(KeyboardWrapper keyboard) {
        this.keyboard = keyboard;
    }

    public static Keyboards fromKey(String key) {
        if (key == null) return null;
        for (Keyboards type : values()) {
            if (type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
