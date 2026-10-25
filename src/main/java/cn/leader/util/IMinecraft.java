package cn.leader.util;

import cn.leader.Client;
import net.minecraft.client.MinecraftClient;

public interface IMinecraft {
    MinecraftClient mc = MinecraftClient.getInstance();
    Client instance = Client.instance;
}
