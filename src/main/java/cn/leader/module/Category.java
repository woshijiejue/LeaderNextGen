package cn.leader.module;


public enum Category {
    Combat("Combat"),
    Movement("Movement"),
    Player("Player"),
    Visual("Visual"),
    Misc("Misc"),
    Client("Client");

    public final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
