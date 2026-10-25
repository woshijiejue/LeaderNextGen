package cn.leader.util;

import cn.leader.Client;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

@UtilityClass
public class Util implements IMinecraft{
    public int offGroundTicks, onGroundTicks;
    public int skipTicks = 0;
    public float timer = 1;


    public void addChatMessage(String message) {
        if (mc.player == null) return;

        mc.player.sendMessage(Component.literal(message), false);
    }

    public void log(String message) {
        addChatMessage(ChatFormatting.DARK_GRAY + "[" + ChatFormatting.AQUA + Client.name + ChatFormatting.DARK_GRAY + "] " + ChatFormatting.RESET + message);
    }

    public void debug(String message) {
        addChatMessage(ChatFormatting.DARK_GRAY + "[" + ChatFormatting.RED + "Debug" + ChatFormatting.DARK_GRAY + "] " + ChatFormatting.RESET + message);
    }

    public String getIGN() {
        if (mc.player != null) {
            return mc.player.getGameProfile().getName();
        }

        return mc.getSession().getUsername();
    }
}
