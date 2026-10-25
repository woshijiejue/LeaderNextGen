package cn.leader.util.network;

import cn.leader.command.impl.BindCommand;
import cn.leader.command.impl.ConfigCommand;
import cn.leader.command.impl.ToggleCommand;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.PacketEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.util.concurrent.LinkedBlockingDeque;


public class BlinkUtils implements IMinecraft {
    private static final LinkedBlockingDeque<Packet<ClientGamePacketListener>> serverPackets = new LinkedBlockingDeque<>();
    private static final LinkedBlockingDeque<Packet<?>> clientPackets = new LinkedBlockingDeque<>();
    public static boolean blinking = false;
    public static boolean delaying = false;

    public static void startBlink() {
        if (blinking) return;
        clientPackets.clear();
        blinking = true;
    }

    public static void stopBlink() {
        if (!blinking) return;
        sendBlinkPackets();
        blinking = false;
    }

    public static void startDelay() {
        if (delaying) return;
        serverPackets.clear();
        delaying = true;
    }

    public static void stopDelay() {
        if (!delaying) return;
        handleDelayPackets();
        delaying = false;
    }

    public static void sendBlinkPackets() {
        synchronized (clientPackets) {
            while (!clientPackets.isEmpty()) {
                Packet<?> packet = clientPackets.poll();
                PacketUtil.sendPacketNoEvent(packet);
            }
        }
    }

    public static void handleMove(ServerboundMovePlayerPacket packet, RemotePlayer clonePlayer) {
        double x = packet.getX(clonePlayer.getX());
        double y = packet.getY(clonePlayer.getY());
        double z = packet.getZ(clonePlayer.getZ());

        float yaw = packet.getYRot(clonePlayer.getYRot());
        float pitch = packet.getXRot(clonePlayer.getXRot());

        clonePlayer.absSnapTo(x, y, z, yaw, pitch);

        if (packet.hasRotation()) {
            clonePlayer.setYRot(yaw);
            clonePlayer.setYHeadRot(yaw);
            clonePlayer.setXRot(pitch);
        }
    }

    public static void releaseTick(RemotePlayer clonePlayer) {
        synchronized (clientPackets) {
            while (!clientPackets.isEmpty()) {
                Packet<?> poll = clientPackets.poll();
                PacketUtil.sendPacketNoEvent(poll);
                if (poll instanceof ServerboundMovePlayerPacket) {
                    handleMove((ServerboundMovePlayerPacket) poll, clonePlayer);
                    break;
                }
            }
        }
    }
    public static LinkedBlockingDeque<Packet<ClientGamePacketListener>> getServerPackets() {
        return serverPackets;
    }

    public static LinkedBlockingDeque<Packet<?>> getClientPackets() {
        return clientPackets;
    }

    public BlinkUtils() {
        instance.getEventManager().register(this);
    }
    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.isCancelled()) return;
        if (mc.player == null || mc.level == null || mc.getConnection() == null || mc.player.isDeadOrDying()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientIntentionPacket
                || packet instanceof ServerboundStatusRequestPacket
                || packet instanceof ServerboundPingRequestPacket
                || packet instanceof ClientboundSystemChatPacket
                || packet instanceof ClientboundDisconnectPacket
                || packet instanceof ClientboundMoveEntityPacket) {
            return;
        }

        if (event.getType() == PacketEvent.Type.Send && blinking) {
            event.setCancelled();
            synchronized (BlinkUtils.getClientPackets()) {
                BlinkUtils.getClientPackets().add(packet);
            }
        }

        if (event.getType() == PacketEvent.Type.Received && delaying) {
            event.setCancelled();
            synchronized (BlinkUtils.getServerPackets()) {
                BlinkUtils.getServerPackets().add((Packet<ClientGamePacketListener>) packet);
            }
        }
    }
    public static void handleDelayPackets() {
        synchronized (serverPackets) {
            while (!serverPackets.isEmpty()) {
                Packet<ClientGamePacketListener> packet = serverPackets.poll();
                packet.handle(mc.getConnection());
            }
        }
    }


}
