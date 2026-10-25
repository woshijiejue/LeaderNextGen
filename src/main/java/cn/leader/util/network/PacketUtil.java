package cn.leader.util.network;

import cn.leader.util.IMinecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

import java.util.ArrayList;

public final class PacketUtil implements IMinecraft {
    private static final ArrayList<Packet<?>> packets = new ArrayList<>();

    public static void sendPacket(Packet<?> packet) {
        if (mc.getConnection() == null) return;

        mc.getConnection().sendPacket(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        packets.add(packet);
        sendPacket(packet);
    }



    @SuppressWarnings("unchecked")
    public static void receivePacket(Packet<?> packet) {
        if (mc.getConnection() == null) return;
        ((Packet<ClientGamePacketListener>) packet).apply(mc.getConnection());
    }

    public static void receivePacketNoEvent(Packet<?> packet) {
        packets.add(packet);
        receivePacket(packet);
    }

    private PacketUtil() {
    }

    public static final ArrayList<Packet<?>> getPackets() {
        return this.packets;
    }
}