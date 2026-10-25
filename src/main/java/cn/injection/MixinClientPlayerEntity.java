package cn.injection;

import cn.leader.Client;
import cn.leader.event.impl.*;
import cn.leader.util.IMinecraft;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer implements IMinecraft {

    @Shadow
    public ClientInput input;
    @Shadow
    protected int ticksLeftToDoubleTapSprint;
    @Shadow
    private double lastX;
    @Shadow
    private double lastBaseY;
    @Shadow
    private double lastZ;
    @Shadow
    private float lastYaw;
    @Shadow
    private float lastPitch;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    private boolean lastHorizontalCollision;
    @Shadow
    private boolean autoJumpEnabled;
    @Shadow
    private int ticksSinceLastPositionPacketSent;

    @Shadow
    @Final
    public ClientPacketListener networkHandler;
    @Shadow
    @Final
    protected Minecraft client;

    @Shadow
    protected abstract void sendSprintingPacket();
    @Shadow
    protected abstract boolean isCamera();

    public MixinClientPlayerEntity(final ClientLevel world, final GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(final CallbackInfo ci) {
        Client.instance.announce();
        instance.getEventManager().call(new UpdateEvent());
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void tickMovement(final CallbackInfo ci) {
        instance.getEventManager().call(new LivingUpdateEvent());
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void move(final MoverType movementType, final Vec3 movement, final CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement.x, movement.y, movement.z);
        instance.getEventManager().call(event);

        if (event.isCancelled()) {
            ci.cancel();
        } else if (event.getX() != movement.x || event.getY() != movement.y || event.getZ() != movement.z) {
            super.move(movementType, new Vec3(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/LocalPlayer;isUsingItem()Z"))
    private boolean tickMovement(final LocalPlayer player) {
        if (this.isUsingItem() && !this.hasVehicle()) {
            final SlowEvent event = new SlowEvent(0.2F, 0.2F);
            instance.getEventManager().call(event);

            if (!event.isCancelled()) {
                this.input.movementSideways *= event.getSideways();
                this.input.movementForward *= event.getForward();
                this.ticksLeftToDoubleTapSprint = 0;
            }
            return false;
        }

        return this.isUsingItem();
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void sendMovementPackets(final CallbackInfo ci) {
        this.sendSprintingPacket();

        if (this.isCamera()) {
            MotionEvent event = new MotionEvent(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision);
            instance.getEventManager().call(event);

            if (!event.isCancelled()) {
                double d = event.getX() - this.lastX;
                double e = event.getY() - this.lastBaseY;
                double f = event.getZ() - this.lastZ;
                double g = event.getYRot() - this.lastYaw;
                double h = event.getXRot() - this.lastPitch;

                ++this.ticksSinceLastPositionPacketSent;

                boolean isPosChanged = Mth.squaredMagnitude(d, e, f) > Mth.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
                boolean isLookChanged = g != 0.0 || h != 0.0;

                if (isPosChanged && isLookChanged) {
                    this.networkHandler.sendPacket(new ServerboundMovePlayerPacket.Full(event.getX(), event.getY(), event.getZ(), event.getYRot(), event.getXRot(), event.onGround(), event.isHorizontalCollision()));
                } else if (isPosChanged) {
                    this.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(event.getX(), event.getY(), event.getZ(), event.onGround(), event.isHorizontalCollision()));
                } else if (isLookChanged) {
                    this.networkHandler.sendPacket(new ServerboundMovePlayerPacket.LookAndOnGround(event.getYRot(), event.getXRot(), event.onGround(), event.isHorizontalCollision()));
                } else if (this.lastOnGround != event.onGround() || this.lastHorizontalCollision != event.isHorizontalCollision()) {
                    this.networkHandler.sendPacket(new ServerboundMovePlayerPacket.OnGroundOnly(event.onGround(), event.isHorizontalCollision()));
                }

                if (isPosChanged) {
                    this.lastX = event.getX();
                    this.lastBaseY = event.getY();
                    this.lastZ = event.getZ();
                    this.ticksSinceLastPositionPacketSent = 0;
                }

                if (isLookChanged) {
                    this.lastYaw = event.getYRot();
                    this.lastPitch = event.getXRot();
                }

                this.lastOnGround = event.onGround();
                this.lastHorizontalCollision = event.isHorizontalCollision();
                this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
            }

            event.setPost();
            Client.instance.getEventManager().call(event);
        }

        ci.cancel();
    }
}