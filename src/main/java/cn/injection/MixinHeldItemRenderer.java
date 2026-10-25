package cn.injection;

import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.impl.visual.Animation;
import cn.leader.util.IMinecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer implements IMinecraft {
    @Shadow
    private ItemStack offHand;
    @Shadow
    @Final
    private ItemRenderer itemRenderer;
    @Shadow
    protected abstract void renderArmHoldingItem(PoseStack matrices, MultiBufferSource vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm);
    @Shadow
    protected abstract void renderMapInBothHands(PoseStack matrices, MultiBufferSource vertexConsumers, int light, float pitch, float equipProgress, float swingProgress);
    @Shadow
    protected abstract void renderMapInOneHand(PoseStack matrices, MultiBufferSource vertexConsumers, int light, float equipProgress, HumanoidArm arm, float swingProgress, ItemStack stack);
    @Shadow
    protected abstract void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress);
    @Shadow
    protected abstract void applySwingOffset(PoseStack matrices, HumanoidArm arm, float swingProgress);
    @Shadow
    protected abstract void applyEatOrDrinkTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, Player player);
    @Shadow
    protected abstract void applyBrushTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, Player player, float equipProgress);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void renderFirstPersonItem(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        Animation animation = instance.getModuleManager().getModule(Animation.class);
        KillAura aura = instance.getModuleManager().getModule(KillAura.class);

        if (!animation.isEnabled()) {
            return;
        }

        ci.cancel();

        if (!player.isUsingSpyglass()) {
            boolean bl = hand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();

            matrices.push();
            if (item.isEmpty()) {
                if (bl && !player.isInvisible()) {
                    this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                }
            } else if (item.contains(DataComponents.MAP_ID)) {
                if (bl && this.offHand.isEmpty()) {
                    this.renderMapInBothHands(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
                } else {
                    this.renderMapInOneHand(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
                }
            } else if (item.isOf(Items.CROSSBOW)) {
                boolean bl2 = CrossbowItem.isCharged(item);
                boolean bl3 = arm == HumanoidArm.RIGHT;
                int i = bl3 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float)i * -0.4785682F, -0.094387F, 0.05731531F);
                    matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-11.935F));
                    matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)i * 65.3F));
                    matrices.multiply(Axis.POSITIVE_Z.rotationDegrees((float)i * -9.785F));
                    float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float g = f / (float)CrossbowItem.getPullTime(item, player);
                    if (g > 1.0F) {
                        g = 1.0F;
                    }

                    if (g > 0.1F) {
                        float h = Mth.sin((f - 0.1F) * 1.3F);
                        float j = g - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }

                    matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                    matrices.multiply(Axis.NEGATIVE_Y.rotationDegrees((float)i * 45.0F));
                } else {
                    this.swingArm(swingProgress, equipProgress, matrices, i, arm);
                    if (bl2 && swingProgress < 0.001F && bl) {
                        matrices.translate((float)i * -0.641864F, 0.0F, 0.0F);
                        matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)i * 10.0F));
                    }
                }

                this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
            } else {
                boolean bl2 = arm == HumanoidArm.RIGHT;
                int l = bl2 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    switch (item.getUseAction()) {
                        case NONE:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case EAT:
                        case DRINK:
                            this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item, player);
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case BLOCK:
                            if (item.getItem() instanceof SwordItem && (this.offHand.isEmpty() || aura.isRenderBlock())) {
                                this.blockAnimation(swingProgress, equipProgress, matrices, arm, l, animation);
                            } else {
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                if (!(item.getItem() instanceof ShieldItem)) {
                                    matrices.translate((float)l * -0.14142136F, 0.08F, 0.14142136F);
                                    matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-102.25F));
                                    matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)l * 13.365F));
                                    matrices.multiply(Axis.POSITIVE_Z.rotationDegrees((float)l * 78.05F));
                                }
                            }
                            break;
                        case BOW:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float)l * -0.2785682F, 0.18344387F, 0.15731531F);
                            matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-13.935F));
                            matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)l * 35.3F));
                            matrices.multiply(Axis.POSITIVE_Z.rotationDegrees((float)l * -9.785F));
                            float m = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float f = m / 20.0F;
                            f = (f * f + f * 2.0F) / 3.0F;
                            if (f > 1.0F) {
                                f = 1.0F;
                            }

                            if (f > 0.1F) {
                                float g = Mth.sin((m - 0.1F) * 1.3F);
                                float h = f - 0.1F;
                                float j = g * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }

                            matrices.translate(f * 0.0F, f * 0.0F, f * 0.04F);
                            matrices.scale(1.0F, 1.0F, 1.0F + f * 0.2F);
                            matrices.multiply(Axis.NEGATIVE_Y.rotationDegrees((float)l * 45.0F));
                            break;
                        case SPEAR:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float)l * -0.5F, 0.7F, 0.1F);
                            matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-55.0F));
                            matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)l * 35.3F));
                            matrices.multiply(Axis.POSITIVE_Z.rotationDegrees((float)l * -9.785F));
                            float m1 = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float f1 = m1 / 10.0F;
                            if (f1 > 1.0F) {
                                f1 = 1.0F;
                            }

                            if (f1 > 0.1F) {
                                float g = Mth.sin((m1 - 0.1F) * 1.3F);
                                float h = f1 - 0.1F;
                                float j = g * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }

                            matrices.translate(0.0F, 0.0F, f1 * 0.2F);
                            matrices.scale(1.0F, 1.0F, 1.0F + f1 * 0.2F);
                            matrices.multiply(Axis.NEGATIVE_Y.rotationDegrees((float)l * 45.0F));
                            break;
                        case BRUSH:
                            this.applyBrushTransformation(matrices, tickDelta, arm, item, player, equipProgress);
                            break;
                        case BUNDLE:
                            this.swingArm(swingProgress, equipProgress, matrices, l, arm);
                    }
                } else if (player.isUsingRiptide()) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float)l * -0.4F, 0.8F, 0.3F);
                    matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)l * 65.0F));
                    matrices.multiply(Axis.POSITIVE_Z.rotationDegrees((float)l * -85.0F));
                } else {
                    boolean isSwordBlock = item.getItem() instanceof SwordItem && mc.options.useKey.isPressed() && this.offHand.isEmpty();
                    if (isSwordBlock || aura.isRenderBlock()) {
                        this.blockAnimation(swingProgress, equipProgress, matrices, arm, l, animation);
                    } else {
                        this.swingArm(swingProgress, equipProgress, matrices, l, arm);
                    }
                }

                this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
            }

            matrices.pop();
        }
    }

    @Unique
    private void blockAnimation(float swingProgress, float equipProgress, PoseStack matrices, HumanoidArm arm, int armX, Animation animation) {
        this.applyEquipOffset(matrices, arm, animation.equipProgress.getValue() ? equipProgress : 0);

        if (animation.blockMode.is("1.7")) {
            this.applySwingOffset(matrices, arm, swingProgress);

            matrices.translate((float)armX * -0.15F, 0.05F, 0.1F);
            matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-105.0F));
            matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)armX * 16.0F));
            matrices.multiply(Axis.POSITIVE_Z.rotationDegrees((float)armX * -278.0F));

        } else if (animation.blockMode.is("SideDown")) {
            matrices.translate((float)armX * -0.15F, 0.05F, 0.1F);
            matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-105.0F));
            matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)armX * 16.0F));
            matrices.multiply(Axis.POSITIVE_Z.rotationDegrees((float)armX * -278.0F));

            float sqr = Mth.sin((float) (swingProgress * swingProgress * Math.PI));
            float sqrt = Mth.sin((float) (Mth.sqrt(swingProgress) * Math.PI));
            matrices.multiply(Axis.POSITIVE_Y.rotationDegrees((float)armX * sqr * -20.0F));
            matrices.multiply(Axis.POSITIVE_Z.rotationDegrees((float)armX * sqrt * -20.0F));
            matrices.multiply(Axis.POSITIVE_X.rotationDegrees(sqrt * -80.0F));

        }
    }

    @Unique
    private void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if (!stack.isEmpty()) {
            this.itemRenderer.renderItem(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
        }
    }

    @Unique
    private void swingArm(float swingProgress, float equipProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        Animation animation = instance.getModuleManager().getModule(Animation.class);

        if (!animation.isEnabled() || animation.swingMode.is("Vanilla")) {
            float f = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
            float g = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * ((float) Math.PI * 2F));
            float h = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
            matrices.translate((float) armX * f, g, h);
        }

        this.applyEquipOffset(matrices, arm, equipProgress);
        this.applySwingOffset(matrices, arm, swingProgress);
    }
}