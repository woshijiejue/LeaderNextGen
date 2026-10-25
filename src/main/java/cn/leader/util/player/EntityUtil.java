package cn.leader.util.player;

import cn.injection.accessor.ClientPlayerInteractionManagerAccessor;
import cn.injection.accessor.MinecraftClientAccessor;
import cn.leader.manager.RotationManager;
import cn.leader.module.impl.client.ClientSetting;
import cn.leader.module.impl.player.AntiBot;
import cn.leader.util.IMinecraft;
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

public final class EntityUtil implements IMinecraft {

    public static boolean isSelected(Entity entity) {
        return isSelected(entity, true);
    }

    public static boolean isSelected(Entity entity,  boolean checkSelf) {
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
    public static boolean isAnimal(final Entity entity) {
        return entity instanceof AnimalEntity
                || entity instanceof SquidEntity
                || entity instanceof IronGolemEntity
                || entity instanceof BatEntity;
    }

    public static boolean isMob(final Entity entity) {
        return entity instanceof Monster
                || entity instanceof SlimeEntity
                || entity instanceof GhastEntity
                || entity instanceof ShulkerEntity
                || entity instanceof EnderDragon;
    }
    public static void doItemUseWithoutBlock() {
        if (!mc.gameMode.isBreakingBlock()) {
            ((MinecraftClientAccessor)mc).setItemUseCooldown(4);
            if (!mc.player.isRiding()) {
                for(InteractionHand hand : InteractionHand.values()) {
                    ItemStack itemStack = mc.player.getItemInHand(hand);
                    if (!itemStack.isItemEnabled(mc.level.getEnabledFeatures())) {
                        return;
                    }

                    if (mc.hitResult != null) {
                        if (Objects.requireNonNull(mc.hitResult.getType()) == HitResult.Type.ENTITY) {
                            EntityHitResult entityHitResult = (EntityHitResult) mc.hitResult;
                            Entity entity = entityHitResult.getEntity();
                            if (!mc.level.getWorldBorder().contains(entity.getBlockPos())) {
                                return;
                            }

                            InteractionResult actionResult = mc.gameMode.interactEntityAtLocation(mc.player, entity, entityHitResult, hand);
                            if (!actionResult.isAccepted()) {
                                actionResult = mc.gameMode.interactEntity(mc.player, entity, hand);
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
        ((ClientPlayerInteractionManagerAccessor)mc.gameMode).callSyncSelectedSlot();
        MutableObject<InteractionResult> mutableObject = new MutableObject();
        ((ClientPlayerInteractionManagerAccessor)mc.gameMode).callSendSequencedPacket(mc.level, (sequence) -> {
            ServerboundUseItemPacket playerInteractItemC2SPacket = new ServerboundUseItemPacket(hand, sequence, RotationManager.currentRotations[0], RotationManager.currentRotations[1]);
            ItemStack itemStack = player.getItemInHand(hand);
            if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
                mutableObject.setValue(InteractionResult.PASS);
                return playerInteractItemC2SPacket;
            } else {
                InteractionResult actionResult = itemStack.use(mc.level, player, hand);
                ItemStack itemStack2;
                if (actionResult instanceof InteractionResult.Success) {
                    InteractionResult.Success success = (InteractionResult.Success)actionResult;
                    itemStack2 = (ItemStack)Objects.requireNonNullElseGet(success.getNewHandStack(), () -> player.getItemInHand(hand));
                } else {
                    itemStack2 = player.getItemInHand(hand);
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

    private static EntityUtil() {
    }

}