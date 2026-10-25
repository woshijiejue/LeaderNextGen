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
import net.minecraft.world.phys.Vec2;
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
    protected int sprintTriggerTime;
    @Shadow
    private double xLast;
    @Shadow
    private double yLast;
    @Shadow
    private double zLast;
    @Shadow
    private float yRotLast;
    @Shadow
    private float xRotLast;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    private boolean lastHorizontalCollision;
    @Shadow
    private boolean autoJumpEnabled;
    @Shadow
    private int positionReminder;

    @Shadow
    @Final
    public ClientPacketListener connection;
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    protected abstract void sendIsSprintingIfNeeded();
    @Shadow
    protected abstract boolean isControlledCamera();

    public MixinClientPlayerEntity(final ClientLevel world, final GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(final CallbackInfo ci) {
        Client.instance.announce();
        instance.getEventManager().call(new UpdateEvent());
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
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

    @Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec2;scale(F)Lnet/minecraft/world/phys/Vec2;", ordinal = 1))
    private Vec2 itemUseSlowdown(final Vec2 move, final float multiplier) {
        final SlowEvent event = new SlowEvent(multiplier, multiplier);
        instance.getEventManager().call(event);

        if (event.isCancelled()) {
            return move;
        }

        this.sprintTriggerTime = 0;
        return new Vec2(move.x * event.getSideways(), move.y * event.getForward());
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void sendMovementPackets(final CallbackInfo ci) {
        this.sendIsSprintingIfNeeded();

        if (this.isControlledCamera()) {
            MotionEvent event = new MotionEvent(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision);
            instance.getEventManager().call(event);

            if (!event.isCancelled()) {
                double d = event.getX() - this.xLast;
                double e = event.getY() - this.yLast;
                double f = event.getZ() - this.zLast;
                double g = event.getYRot() - this.yRotLast;
                double h = event.getXRot() - this.xRotLast;

                ++this.positionReminder;

                boolean isPosChanged = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4) || this.positionReminder >= 20;
                boolean isLookChanged = g != 0.0 || h != 0.0;

                if (isPosChanged && isLookChanged) {
                    this.connection.send(new ServerboundMovePlayerPacket.PosRot(event.getX(), event.getY(), event.getZ(), event.getYRot(), event.getXRot(), event.isOnGround(), event.isHorizontalCollision()));
                } else if (isPosChanged) {
                    this.connection.send(new ServerboundMovePlayerPacket.Pos(event.getX(), event.getY(), event.getZ(), event.isOnGround(), event.isHorizontalCollision()));
                } else if (isLookChanged) {
                    this.connection.send(new ServerboundMovePlayerPacket.Rot(event.getYRot(), event.getXRot(), event.isOnGround(), event.isHorizontalCollision()));
                } else if (this.lastOnGround != event.isOnGround() || this.lastHorizontalCollision != event.isHorizontalCollision()) {
                    this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(event.isOnGround(), event.isHorizontalCollision()));
                }

                if (isPosChanged) {
                    this.xLast = event.getX();
                    this.yLast = event.getY();
                    this.zLast = event.getZ();
                    this.positionReminder = 0;
                }

                if (isLookChanged) {
                    this.yRotLast = event.getYRot();
                    this.xRotLast = event.getXRot();
                }

                this.lastOnGround = event.isOnGround();
                this.lastHorizontalCollision = event.isHorizontalCollision();
                this.autoJumpEnabled = this.minecraft.options.autoJump().get();
            }

            event.setPost();
            Client.instance.getEventManager().call(event);
        }

        ci.cancel();
    }
}