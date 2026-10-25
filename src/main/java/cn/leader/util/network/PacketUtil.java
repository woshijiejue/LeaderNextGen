package cn.leader.util.network;

import cn.injection.accessor.ClientWorldAccessor;
import cn.leader.util.IMinecraft;
import net.minecraft.client.multiplayer.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

import java.util.ArrayList;

public final class PacketUtil implements IMinecraft {
    @Getter
    private static final ArrayList<Packet<?>> packets = new ArrayList<>();

    public static void sendPacket(Packet<?> packet) {
        if (mc.getConnection() == null) return;

        mc.getConnection().sendPacket(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        packets.add(packet);
        sendPacket(packet);
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;

        try (PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor) mc.level).getPendingUpdateManagerField().incrementSequence()) {
            int sequence = pendingUpdateManager.getSequence();
            Packet<?> packet = packetCreator.predict(sequence);
            mc.getConnection().sendPacket(packet);
        }
    }

    public static void sendSequencedPacketNoEvent(SequencedPacketCreator packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;

        try (PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor) mc.level).getPendingUpdateManagerField().incrementSequence()) {
            int sequence = pendingUpdateManager.getSequence();
            Packet<?> packet = packetCreator.predict(sequence);
            packets.add(packet);
            mc.getConnection().sendPacket(packet);
        }
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
}