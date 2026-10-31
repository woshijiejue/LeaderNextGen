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
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;

public class Velocity extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Modify", "Modify", "Jump Reset","Hypixel");

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

    private static final LinkedBlockingDeque<Packet<ClientGamePacketListener>> serverPackets = new LinkedBlockingDeque<>();
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
    }

    @Override
    public void onDisable() {
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
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.pause = this.pauseOnFlag.getValue().intValue();
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
        if (motion.movement().y <= 0)return;
        if (this.pause > 0) return;
        if (!rollChance()) return;

        switch (this.mode.getValue()) {
            case "Hypixel" -> {
                if(!buffer){
                    buffer = true;
                    bufferTick = 0;
                    event.setCancelled(true);
                    Packet<ClientGamePacketListener> typedPacket = (Packet<ClientGamePacketListener>) event.getPacket();
                    serverPackets.add(typedPacket);
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
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;
        if (!event.isPost() || !buffer) return;

        boolean released = bufferTick >= delayTicks.getValue().intValue()
                || mc.player.isSprinting()
                || (mc.player.getLastSentInput() != null && mc.player.getLastSentInput().sprint())
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
        jumpPending = false;
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

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.pause > 0) this.pause--;

        if (this.pending != null) {
            if (--this.pendingTicks <= 0) {
                apply(this.pending);
                this.pending = null;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.jumpPending) return;

        this.jumpPending = false;
        event.setJumping(true);
    }

    private void apply(Vec3 velocity) {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(velocity);
    }
    public static boolean inInBlackList(PacketEvent event) {
        if (mc.player == null) return false;

        Packet<?> packet = event.getPacket();
        return packet instanceof ClientboundSetHealthPacket
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
