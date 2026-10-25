package cn.leader.module;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
    Combat("Combat"),
    Movement("Movement"),
    Player("Player"),
    Visual("Visual"),
    Misc("Misc"),
    Client("Client");

    public final String name;
}
