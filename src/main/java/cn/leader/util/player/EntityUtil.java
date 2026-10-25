package cn.leader.util.player;

import cn.injection.accessor.ClientPlayerInteractionManagerAccessor;
import cn.injection.accessor.MinecraftClientAccessor;
import cn.leader.manager.RotationManager;
import cn.leader.module.impl.client.ClientSetting;
import cn.leader.module.impl.player.AntiBot;
import cn.leader.util.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Objects;

@UtilityClass
public class EntityUtil implements IMinecraft {

    public boolean isSelected(Entity entity) {
        return isSelected(entity, true);
    }

    public boolean isSelected(Entity entity,  boolean checkSelf) {
        if (!(entity instanceof LivingEntity livingEntity)) return false;

        ClientSetting clientSetting = instance.getModuleManager().getModule(ClientSetting.class);

        if (!clientSetting.getTarget().isEnabled("Invisible") && livingEntity.isInvisible()) {
            return false;
        }

        if (!livingEntity.isAlive() || livingEntity.isSpectator()) {
            return false;
        }

        AntiBot antiBot = instance.getModuleManager().getModule(AntiBot.class);
        if (antiBot != null && antiBot.isEnabled() && antiBot.isBot(entity)) {
            return false;
        }

        if (livingEntity instanceof PlayerEntity player) {
            if (checkSelf && player.equals(mc.player)) {
                return false;
            }

            return clientSetting.getTarget().isEnabled("Player");
        }

        boolean isMob = clientSetting.getTarget().isEnabled("Mob") && isMob(livingEntity);
        boolean isAnimal = clientSetting.getTarget().isEnabled("Animal") && isAnimal(livingEntity);
        boolean isVillager = clientSetting.getTarget().isEnabled("Villager") && livingEntity instanceof VillagerEntity;

        return isMob || isAnimal || isVillager;
    }
    public boolean isAnimal(final Entity entity) {
        return entity instanceof AnimalEntity
                || entity instanceof SquidEntity
                || entity instanceof IronGolemEntity
                || entity instanceof BatEntity;
    }

    public boolean isMob(final Entity entity) {
        return entity instanceof Monster
                || entity instanceof SlimeEntity
                || entity instanceof GhastEntity
                || entity instanceof ShulkerEntity
                || entity instanceof EnderDragonEntity;
    }
    public static void doItemUseWithoutBlock() {
        if (!mc.interactionManager.isBreakingBlock()) {
            ((MinecraftClientAccessor)mc).setItemUseCooldown(4);
            if (!mc.player.isRiding()) {
                for(Hand hand : Hand.values()) {
                    ItemStack itemStack = mc.player.getStackInHand(hand);
                    if (!itemStack.isItemEnabled(mc.world.getEnabledFeatures())) {
                        return;
                    }

                    if (mc.crosshairTarget != null) {
                        if (Objects.requireNonNull(mc.crosshairTarget.getType()) == HitResult.Type.ENTITY) {
                            EntityHitResult entityHitResult = (EntityHitResult) mc.crosshairTarget;
                            Entity entity = entityHitResult.getEntity();
                            if (!mc.world.getWorldBorder().contains(entity.getBlockPos())) {
                                return;
                            }

                            ActionResult actionResult = mc.interactionManager.interactEntityAtLocation(mc.player, entity, entityHitResult, hand);
                            if (!actionResult.isAccepted()) {
                                actionResult = mc.interactionManager.interactEntity(mc.player, entity, hand);
                            }

                            if (actionResult instanceof ActionResult.Success) {
                                ActionResult.Success success = (ActionResult.Success) actionResult;
                                if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
                                    mc.player.swingHand(hand);
                                }

                                return;
                            }
                        }
                    }

                    if (!itemStack.isEmpty()) {
                        ActionResult actionResult3 = interactItem(mc.player, hand);
                        if (actionResult3 instanceof ActionResult.Success) {
                            ActionResult.Success success3 = (ActionResult.Success)actionResult3;
                            if (success3.swingSource() == ActionResult.SwingSource.CLIENT) {
                                mc.player.swingHand(hand);
                            }

                            return;
                        }
                    }
                }

            }
        }
    }
    public static ActionResult interactItem(PlayerEntity player, Hand hand) {
        ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).callSyncSelectedSlot();
        MutableObject<ActionResult> mutableObject = new MutableObject();
        ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).callSendSequencedPacket(mc.world, (sequence) -> {
            PlayerInteractItemC2SPacket playerInteractItemC2SPacket = new PlayerInteractItemC2SPacket(hand, sequence, RotationManager.currentRotations[0], RotationManager.currentRotations[1]);
            ItemStack itemStack = player.getStackInHand(hand);
            if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
                mutableObject.setValue(ActionResult.PASS);
                return playerInteractItemC2SPacket;
            } else {
                ActionResult actionResult = itemStack.use(mc.world, player, hand);
                ItemStack itemStack2;
                if (actionResult instanceof ActionResult.Success) {
                    ActionResult.Success success = (ActionResult.Success)actionResult;
                    itemStack2 = (ItemStack)Objects.requireNonNullElseGet(success.getNewHandStack(), () -> player.getStackInHand(hand));
                } else {
                    itemStack2 = player.getStackInHand(hand);
                }
                if (itemStack2 != itemStack) {
                    player.setStackInHand(hand, itemStack2);
                }
                mutableObject.setValue(actionResult);
                return playerInteractItemC2SPacket;
            }
        });
        return (ActionResult)mutableObject.getValue();
    }
}