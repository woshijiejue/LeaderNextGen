package cn.leader.command;

import cn.leader.command.impl.*;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.PacketEvent;
import cn.leader.util.IMinecraft;
import cn.leader.util.Util;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CommandManager implements IMinecraft {
    private final List<Command> commands = new ArrayList<>();

    public CommandManager() {
        instance.getEventManager().register(this);

        addCommands(
                new ToggleCommand(),
                new BindCommand(),
                new ConfigCommand(),
                new InfoCommand()
        );
    }

    public void addCommands(Command... commandsArray) {
        this.commands.addAll(Arrays.asList(commandsArray));
    }

    public List<String> getCompletions(String input) {
        List<String> suggestions = new ArrayList<>();
        String[] split = input.substring(".".length()).split(" ", -1);
        String label = split[0];

        if (split.length == 1) {
            for (Command command : commands) {
                for (String alias : command.getAliases()) {
                    if (alias.toLowerCase().startsWith(label.toLowerCase())) {
                        suggestions.add(alias);
                    }
                }
            }
        } else {
            for (Command command : commands) {
                for (String alias : command.getAliases()) {
                    if (alias.equalsIgnoreCase(label)) {
                        List<String> commandCompletions = command.getCompletions(split);
                        String currentArg = split[split.length - 1].toLowerCase();
                        if (commandCompletions != null) {
                            for (String s : commandCompletions) {
                                if (s.toLowerCase().startsWith(currentArg)) {
                                    suggestions.add(s);
                                }
                            }
                        }
                        return suggestions;
                    }
                }
            }
        }
        return suggestions;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == PacketEvent.Type.Send && event.getPacket() instanceof ServerboundChatPacket packet) {
            String message = packet.message();

            if (message.startsWith(".")) {
                event.setCancelled(true);
                String input = message.substring(".".length());

                if (input.isEmpty()) {
                    Util.log("No command entered.");
                    return;
                }

                String[] arguments = input.split(" ");
                String label = arguments[0];

                for (Command command : commands) {
                    for (String alias : command.getAliases()) {
                        if (alias.equalsIgnoreCase(label)) {
                            try {
                                command.execute(arguments);
                            } catch (Exception exception) {
                                Util.log("Execution error: " + exception.getMessage());
                            }
                            return;
                        }
                    }
                }
                Util.log(String.format("'%s' is not a command.", label));
            }
        }
    }

    public List<Command> getCommands() {
        return this.commands;
    }
}