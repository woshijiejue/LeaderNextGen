package cn.leader.command.impl;

import cn.leader.Client;
import cn.leader.command.Command;
import cn.leader.config.Config;
import cn.leader.config.impl.ModuleConfig;
import cn.leader.util.Util;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public final class ConfigCommand extends Command {

    public ConfigCommand() {
        super(".config <load/save/list/delete> [name]", "config", "cfg");
    }

    @Override
    public void execute(final String[] arguments) {
        if (arguments.length < 2) {
            Util.log(this.getUsage());
            return;
        }

        final String action = arguments[1].toLowerCase();

        switch (action) {
            case "list" -> {
                Util.log(ChatFormatting.BLUE + "Available configs:");
                for (final Config config : Client.instance.getConfigManager().getConfigs()) {
                    Util.log("- " + config.getName());
                }
            }

            case "save" -> {
                if (arguments.length < 3) {
                    final Config defaultConfig = Client.instance.getConfigManager().getConfig("Modules");
                    if (defaultConfig != null) {
                        defaultConfig.save();
                        Util.log("Saved default configuration.");
                    }
                    return;
                }

                final String configName = arguments[2];
                Config targetConfig = Client.instance.getConfigManager().getConfig(configName);

                if (targetConfig == null) {
                    targetConfig = new ModuleConfig(configName);
                    Client.instance.getConfigManager().addConfigs(targetConfig);
                }

                targetConfig.save();
                Util.log(String.format("Saved configuration to " + ChatFormatting.GREEN + "%s.json", configName));
            }

            case "load" -> {
                if (arguments.length < 3) {
                    Util.log("Usage: .cfg load <name>");
                    return;
                }

                final String configName = arguments[2];
                final Config targetConfig = Client.instance.getConfigManager().getConfig(configName);

                if (targetConfig != null) {
                    targetConfig.load();
                    Util.log("Loaded config: " + ChatFormatting.AQUA + targetConfig.getName());
                } else {
                    Util.log(ChatFormatting.RED + "Config not found in memory: " + configName);
                }
            }
        }
    }

    @Override
    public List<String> getCompletions(final String[] arguments) {
        final List<String> completions = new ArrayList<>();
        if (arguments.length == 2) {
            completions.add("load");
            completions.add("save");
            completions.add("list");
        } else if (arguments.length == 3) {
            for (final Config config : Client.instance.getConfigManager().getConfigs()) {
                completions.add(config.getName());
            }
        }
        return completions;
    }
}
