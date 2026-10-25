package cn.leader.command.impl;

import cn.leader.Client;
import cn.leader.command.Command;
import cn.leader.module.Module;
import cn.leader.util.Util;
import net.minecraft.ChatFormatting;

/**
 * Prints what build is actually running. Useful when a stale jar or stale build resources make
 * the version in the title bar disagree with what was expected.
 */
public final class InfoCommand extends Command {

    public InfoCommand() {
        super(".info", "info", "version");
    }

    @Override
    public void execute(final String[] arguments) {
        int total = Client.instance.getModuleManager().getModuleMap().size();
        long enabled = Client.instance.getModuleManager().getModuleMap().values().stream()
                .filter(Module::isEnabled)
                .count();

        Util.log(ChatFormatting.AQUA + Client.name + " " + Client.version);
        Util.log(ChatFormatting.GRAY + "Minecraft " + Client.minecraftVersion
                + ChatFormatting.DARK_GRAY + " | " + ChatFormatting.GRAY + "Fabric Loader " + Client.loaderVersion);
        Util.log(ChatFormatting.GRAY + "Modules " + ChatFormatting.WHITE + enabled + "/" + total);
    }
}
