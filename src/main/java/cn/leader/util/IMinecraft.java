package cn.leader.util;

import cn.leader.Client;
import net.minecraft.client.Minecraft;

public interface IMinecraft {
    Minecraft mc = Minecraft.getInstance();
    Client instance = Client.instance;
}
