package cn.leader.module.impl.combat;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.*;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;

public class Velocity extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Modify", "Modify", "Jump Reset", "Hypixel", "Delay");

    private final NumberValue horizontal = new NumberValue("Horizontal", 0, 0, 100, 1,
            () -> this.mode.is("Modify"));
    private final NumberValue vertical = new NumberValue("Vertical", 0, 0, 100, 1,
            () -> this.mode.is("Modify"));
    private final NumberValue delayApply = new NumberValue("Delay", 0, 0, 20, 1,
            () -> this.mode.is("Modify"));

    private final NumberValue chance = new NumberValue("Chance", 100, 0, 100, 1,
            () -> this.mode.is("Modify"));
    private final NumberValue pauseOnFlag = new NumberValue("Pause On Flag", 0, 0, 20, 1);
    private final BoolValue onlySprinting = new BoolValue("Only Sprinting", true,
            () -> this.mode.is("Jump Reset"));
    private final BoolValue delay = new BoolValue("Delay", true,() -> this.mode.is("Hypixel"));
    private final NumberValue delayTicks = new NumberValue("DelayTicks", 3, 0, 20, 1,
            () -> this.mode.is("Hypixel") && delay.getValue());
    private final NumberValue delayModeTicks = new NumberValue("Delay Ticks", 3, 1, 5, 1,
            () -> this.mode.is("Delay"));
    private final BoolValue delayModeJump = new BoolValue("Jump Reset", false,
            () -> this.mode.is("Delay"));

    private static final LinkedBlockingDeque<Packet<ClientGamePacketListener>> serverPackets = new LinkedBlockingDeque<>();
    private final Queue<Packet<ClientGamePacketListener>> delayPackets = new ArrayDeque<>();
    private final Object delayLock = new Object();
    private int delayTicksRemaining;
    private long delayLag;
    private boolean forward;
    private boolean buffer = false;
    private int bufferTick = -1;
    private Vec3 pending;
    private int pendingTicks;
    private int pause;
    private boolean jumpPending;
    private boolean replaying;
    public static boolean packetOnGround;
    public Velocity() {
        super("Velocity", Category.Combat);
        setDescription("Reduces knockback");
    }

    @Override
    public void onEnable() {
        this.pending = null;
        this.pendingTicks = 0;
        this.pause = 0;
        this.jumpPending = false;
        this.buffer = false;
        this.bufferTick = -1;
        this.replaying = false;
        serverPackets.clear();
        resetDelayState();
    }

    @Override
    public void onDisable() {
        flushDelay();
        this.pending = null;
        this.pendingTicks = 0;
        this.jumpPending = false;
        this.buffer = false;
        this.bufferTick = -1;
        this.replaying = false;
        serverPackets.clear();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;
        if (this.replaying) return;
        if (event.getType() == PacketEvent.Type.Send){
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet)
            {
                packetOnGround = packet.isOnGround();
            }
        }
        if (event.getType() != PacketEvent.Type.Received) return;
        if (this.mode.is("Delay") && handleDelayPacket(event)) return;
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.pause = this.pauseOnFlag.getValue().intValue();

            if (buffer) {
                serverPackets.clear();
                buffer = false;
                bufferTick = -1;
            }
            return;
        }
        if (buffer) {
            if (!inInBlackList(event)) {
                event.setCancelled(true);
                @SuppressWarnings("unchecked")
                Packet<ClientGamePacketListener> typedPacket = (Packet<ClientGamePacketListener>) event.getPacket();
                serverPackets.add(typedPacket);
                return;
            }
        }

        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket motion)) return;
        if (motion.id() != mc.player.getId()) return;
        if (this.pause > 0) return;
        if (!rollChance()) return;

        switch (this.mode.getValue()) {
            case "Hypixel" -> {
                if(!buffer){
                    buffer = true;
                    bufferTick = 0;
                    Packet<ClientGamePacketListener> typedPacket = (Packet<ClientGamePacketListener>) event.getPacket();
                    serverPackets.add(typedPacket);
                    event.setCancelled(true);
                }
            }

            case "Modify" -> {
                event.setCancelled(true);

                Vec3 movement = motion.movement();
                Vec3 scaled = new Vec3(
                        movement.x * percent(this.horizontal),
                        movement.y * percent(this.vertical),
                        movement.z * percent(this.horizontal));

                int wait = this.delayApply.getValue().intValue();
                if (wait <= 0) {
                    apply(scaled);
                } else {
                    this.pending = scaled;
                    this.pendingTicks = wait;
                }
            }
            case "Jump Reset" -> {
                if (!mc.player.onGround()) return;
                if (this.onlySprinting.getValue() && !mc.player.isSprinting()) return;

                this.jumpPending = true;
            }
            default -> {
            }
        }
    }
    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        if (bufferTick >= 0) {
            bufferTick++;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        if (this.pause > 0) this.pause--;

        if (this.mode.is("Delay")) {
            tickDelay();
        } else if (!this.delayPackets.isEmpty()) {
            flushDelay();
        }

        releaseBuffer();

        if (this.pending != null) {
            if (--this.pendingTicks <= 0) {
                apply(this.pending);
                this.pending = null;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.mode.is("Delay") && this.delayModeJump.getValue()
                && mc.player.onGround() && mc.player.hurtTime == 9) {
            event.setJumping(true);
        }

        if (!this.jumpPending) return;

        this.jumpPending = false;
        event.setJumping(true);
    }

    @SuppressWarnings("unchecked")
    private boolean handleDelayPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();

        synchronized (this.delayLock) {
            if (packet instanceof ClientboundDisconnectPacket) {
                resetDelayState();
                return false;
            }

            if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundExplodePacket) {
                this.delayLag = System.currentTimeMillis();
            }

            if (this.delayTicksRemaining > 0) {
                event.setCancelled(true);
                this.delayPackets.add((Packet<ClientGamePacketListener>) packet);
                return true;
            }

            if (packet instanceof ClientboundSetEntityMotionPacket motion
                    && motion.id() == mc.player.getId()
                    && System.currentTimeMillis() - this.delayLag >= 100L) {

                Vec3 movement = motion.movement();
                boolean knockback = movement.y > 0.0D && (movement.x != 0.0D || movement.z != 0.0D);

                if (knockback) {
                    this.delayTicksRemaining = this.delayModeTicks.getValue().intValue();
                    event.setCancelled(true);
                    this.delayPackets.add((Packet<ClientGamePacketListener>) motion);
                    return true;
                }
            }

            return false;
        }
    }

    private void tickDelay() {
        List<Packet<ClientGamePacketListener>> pending = null;

        synchronized (this.delayLock) {
            if (this.delayTicksRemaining <= 0) return;

            if (--this.delayTicksRemaining == 0) {
                pending = new ArrayList<>(this.delayPackets);
                this.delayPackets.clear();
            }
        }

        if (pending != null) replay(pending);
    }

    private void flushDelay() {
        List<Packet<ClientGamePacketListener>> pending = null;

        synchronized (this.delayLock) {
            if (!this.delayPackets.isEmpty()) {
                pending = new ArrayList<>(this.delayPackets);
                this.delayPackets.clear();
            }
            this.delayTicksRemaining = 0;
        }

        if (pending != null) replay(pending);
    }

    private void resetDelayState() {
        synchronized (this.delayLock) {
            this.delayTicksRemaining = 0;
            this.delayPackets.clear();
            this.delayLag = 0L;
        }
    }

    private void replay(List<Packet<ClientGamePacketListener>> packets) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return;

        replaying = true;

        try {
            for (Packet<ClientGamePacketListener> packet : packets) {
                if (packet != null) packet.handle(connection);
            }
        } finally {
            replaying = false;
        }
    }

    private void releaseBuffer() {
        if (!buffer) return;

        boolean released = bufferTick >= delayTicks.getValue().intValue()
                || clientOnGround();

        if (!released) return;

        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            serverPackets.clear();
            buffer = false;
            bufferTick = -1;
            return;
        }

        buffer = false;
        bufferTick = -1;
        if (mc.player.onGround()){
            jumpPending = true;
        }
        replaying = true;

        try {
            while (!serverPackets.isEmpty()) {
                Packet<ClientGamePacketListener> packet = serverPackets.poll();
                if (packet == null) break;
                packet.handle(connection);
            }
        } finally {
            replaying = false;
        }
    }

    private void apply(Vec3 velocity) {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(velocity);
    }
    public static boolean inInBlackList(PacketEvent event) {
        if (mc.player == null) return false;

        Packet<?> packet = event.getPacket();
        return packet instanceof ClientboundKeepAlivePacket
                || packet instanceof ClientboundSetHealthPacket
                || packet instanceof ClientboundPlayerPositionPacket
                || packet instanceof ClientboundSoundPacket
                || packet instanceof ClientboundPlayerChatPacket
                || packet instanceof ClientboundPlayerCombatKillPacket
                || packet instanceof ClientboundContainerClosePacket
                || packet instanceof ClientboundHurtAnimationPacket
                || packet instanceof ClientboundSetTitleTextPacket
                || packet instanceof ClientboundSetPlayerTeamPacket
                || (packet instanceof ClientboundAnimatePacket animate && animate.getId() != mc.player.getId());
    }
    private static double percent(NumberValue value) {
        return value.getValue().doubleValue() / 100.0D;
    }

    private boolean rollChance() {
        if (!mode.is("Modify")) return true;
        float value = this.chance.getValue();
        return value >= 100.0F || ThreadLocalRandom.current().nextFloat() * 100.0F < value;
    }
    public static boolean clientOnGround() {
        if (mc.player == null) return false;
        return packetOnGround && mc.player.onGround();
    }
}
