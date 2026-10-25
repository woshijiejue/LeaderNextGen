package cn.leader.util;

import cn.leader.Client;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class Util implements IMinecraft{
    public static int offGroundTicks, onGroundTicks;
    public static int skipTicks = 0;
    public static float timer = 1;


    public static void addChatMessage(String message) {
        if (mc.player == null) return;

        mc.player.displayClientMessage(Component.literal(message), false);
    }

    public static void log(String message) {
        addChatMessage(ChatFormatting.DARK_GRAY + "[" + ChatFormatting.AQUA + Client.name + ChatFormatting.DARK_GRAY + "] " + ChatFormatting.RESET + message);
    }

    public static void debug(String message) {
        addChatMessage(ChatFormatting.DARK_GRAY + "[" + ChatFormatting.RED + "Debug" + ChatFormatting.DARK_GRAY + "] " + ChatFormatting.RESET + message);
    }

    public static String getIGN() {
        if (mc.player != null) {
            return mc.player.getGameProfile().getName();
        }

        return mc.getSession().getUsername();
    }

    private static Util() {
    }

}
