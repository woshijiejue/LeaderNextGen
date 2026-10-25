package cn.leader.util.player;

import cn.injection.accessor.ClientPlayerInteractionManagerAccessor;
import cn.injection.accessor.MinecraftClientAccessor;
import cn.leader.manager.RotationManager;
import cn.leader.module.impl.client.ClientSetting;
import cn.leader.module.impl.player.AntiBot;
import cn.leader.util.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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

        if (livingEntity instanceof Player player) {
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
                || entity instanceof EnderDragon;
    }
    public static void doItemUseWithoutBlock() {
        if (!mc.interactionManager.isBreakingBlock()) {
            ((MinecraftClientAccessor)mc).setItemUseCooldown(4);
            if (!mc.player.isRiding()) {
                for(InteractionHand hand : InteractionHand.values()) {
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

                            InteractionResult actionResult = mc.interactionManager.interactEntityAtLocation(mc.player, entity, entityHitResult, hand);
                            if (!actionResult.isAccepted()) {
                                actionResult = mc.interactionManager.interactEntity(mc.player, entity, hand);
                            }

                            if (actionResult instanceof InteractionResult.Success) {
                                InteractionResult.Success success = (InteractionResult.Success) actionResult;
                                if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                                    mc.player.swingHand(hand);
                                }

                                return;
                            }
                        }
                    }

                    if (!itemStack.isEmpty()) {
                        InteractionResult actionResult3 = interactItem(mc.player, hand);
                        if (actionResult3 instanceof InteractionResult.Success) {
                            InteractionResult.Success success3 = (InteractionResult.Success)actionResult3;
                            if (success3.swingSource() == InteractionResult.SwingSource.CLIENT) {
                                mc.player.swingHand(hand);
                            }

                            return;
                        }
                    }
                }

            }
        }
    }
    public static InteractionResult interactItem(Player player, InteractionHand hand) {
        ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).callSyncSelectedSlot();
        MutableObject<InteractionResult> mutableObject = new MutableObject();
        ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).callSendSequencedPacket(mc.world, (sequence) -> {
            ServerboundUseItemPacket playerInteractItemC2SPacket = new ServerboundUseItemPacket(hand, sequence, RotationManager.currentRotations[0], RotationManager.currentRotations[1]);
            ItemStack itemStack = player.getStackInHand(hand);
            if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
                mutableObject.setValue(InteractionResult.PASS);
                return playerInteractItemC2SPacket;
            } else {
                InteractionResult actionResult = itemStack.use(mc.world, player, hand);
                ItemStack itemStack2;
                if (actionResult instanceof InteractionResult.Success) {
                    InteractionResult.Success success = (InteractionResult.Success)actionResult;
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
        return (InteractionResult)mutableObject.getValue();
    }
}