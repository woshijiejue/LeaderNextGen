package cn.leader.module;


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
