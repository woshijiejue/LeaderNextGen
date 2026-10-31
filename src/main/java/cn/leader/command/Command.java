package cn.leader.command;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    private final String[] aliases;
    private final String usage;

    public Command(String usage, String... aliases) {
        this.usage = usage;
        this.aliases = aliases;
    }

    public abstract void execute(final String[] arguments);

    public List<String> getCompletions(final String[] arguments) {
        return new ArrayList<>();
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public String getUsage() {
        return this.usage;
    }

}
