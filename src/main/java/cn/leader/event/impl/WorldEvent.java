package cn.leader.event.impl;

import cn.leader.event.base.Event;
import net.minecraft.client.multiplayer.ClientLevel;

public class WorldEvent extends Event {
    public final ClientLevel world;

    public ClientLevel getWorld() {
        return this.world;
    }

    public WorldEvent(ClientLevel world) {
        this.world = world;
    }

}
