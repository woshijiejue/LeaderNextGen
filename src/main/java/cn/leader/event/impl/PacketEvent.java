package cn.leader.event.impl;

import cn.leader.event.base.Event;
import net.minecraft.network.protocol.Packet;

public class PacketEvent extends Event {
    private Packet<?> packet;
    private final Type type;

    public enum Type {
        Send,
        Received
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public void setPacket(Packet<?> value) {
        this.packet = value;
    }

    public Type getType() {
        return this.type;
    }

    public PacketEvent(Packet<?> packet, Type type) {
        this.packet = packet;
        this.type = type;
    }

}
