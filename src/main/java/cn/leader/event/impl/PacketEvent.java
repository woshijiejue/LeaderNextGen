package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.protocol.Packet;

@Setter
@Getter
@AllArgsConstructor
public class PacketEvent extends Event {
    private Packet<?> packet;
    private final Type type;

    public enum Type {
        Send,
        Received
    }
}
