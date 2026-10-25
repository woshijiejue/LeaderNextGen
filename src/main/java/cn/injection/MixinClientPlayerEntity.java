package cn.injection;

import cn.leader.Client;
import cn.leader.event.impl.*;
import cn.leader.util.IMinecraft;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity implements IMinecraft {

    @Shadow
    public Input input;
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
    public ClientPlayNetworkHandler networkHandler;
    @Shadow
    @Final
    protected MinecraftClient client;

    @Shadow
    protected abstract void sendSprintingPacket();
    @Shadow
    protected abstract boolean isCamera();

    public MixinClientPlayerEntity(final ClientWorld world, final GameProfile profile) {
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
    private void move(final MovementType movementType, final Vec3d movement, final CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement.x, movement.y, movement.z);
        instance.getEventManager().call(event);

        if (event.isCancelled()) {
            ci.cancel();
        } else if (event.getX() != movement.x || event.getY() != movement.y || event.getZ() != movement.z) {
            super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean tickMovement(final ClientPlayerEntity player) {
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
            MotionEvent event = new MotionEvent(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), this.isOnGround(), this.horizontalCollision);
            instance.getEventManager().call(event);

            if (!event.isCancelled()) {
                double d = event.getX() - this.lastX;
                double e = event.getY() - this.lastBaseY;
                double f = event.getZ() - this.lastZ;
                double g = event.getYaw() - this.lastYaw;
                double h = event.getPitch() - this.lastPitch;

                ++this.ticksSinceLastPositionPacketSent;

                boolean isPosChanged = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
                boolean isLookChanged = g != 0.0 || h != 0.0;

                if (isPosChanged && isLookChanged) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(event.getX(), event.getY(), event.getZ(), event.getYaw(), event.getPitch(), event.isOnGround(), event.isHorizontalCollision()));
                } else if (isPosChanged) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(event.getX(), event.getY(), event.getZ(), event.isOnGround(), event.isHorizontalCollision()));
                } else if (isLookChanged) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(event.getYaw(), event.getPitch(), event.isOnGround(), event.isHorizontalCollision()));
                } else if (this.lastOnGround != event.isOnGround() || this.lastHorizontalCollision != event.isHorizontalCollision()) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(event.isOnGround(), event.isHorizontalCollision()));
                }

                if (isPosChanged) {
                    this.lastX = event.getX();
                    this.lastBaseY = event.getY();
                    this.lastZ = event.getZ();
                    this.ticksSinceLastPositionPacketSent = 0;
                }

                if (isLookChanged) {
                    this.lastYaw = event.getYaw();
                    this.lastPitch = event.getPitch();
                }

                this.lastOnGround = event.isOnGround();
                this.lastHorizontalCollision = event.isHorizontalCollision();
                this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
            }

            event.setPost();
            Client.instance.getEventManager().call(event);
        }

        ci.cancel();
    }
}