package cn.leader.util.player;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceKey;

import java.util.Arrays;
import java.util.List;

import static cn.leader.util.IMinecraft.mc;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.BlockItem;

public class ItemUtils {
    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE, Blocks.IRON_BARS, Blocks.SNOW,
            Blocks.COAL_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.TORCH, Blocks.ANVIL,
            Blocks.NOTE_BLOCK, Blocks.JUKEBOX, Blocks.GOLD_ORE,
            Blocks.IRON_ORE, Blocks.LAPIS_ORE, Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON, Blocks.LEVER, Blocks.TALL_GRASS, Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK, Blocks.RAIL, Blocks.CORNFLOWER, Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM, Blocks.VINE, Blocks.SUNFLOWER, Blocks.LADDER,
            Blocks.FURNACE, Blocks.SAND, Blocks.CACTUS, Blocks.DISPENSER,
            Blocks.DROPPER, Blocks.CRAFTING_TABLE, Blocks.COBWEB, Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL, Blocks.OAK_FENCE, Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT, Blocks.DRAGON_HEAD
    );

    public static boolean isValidItem(ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            if (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SkullBlock) {
                return false;
            }

            var string = itemStack.getHoverName().getString();

            if (string.contains("Click") || string.contains("Right") || string.contains("点击") || string.contains("Teleport") || string.contains("使用") || string.contains("传送")) {
                return false;
            }

            return !string.contains("再来");
        }

        return true;
    }

    public static boolean isValidBlock(ItemStack stack) {
        return isValidBlock(stack, false);
    }

    public static boolean isValidBlock(ItemStack stack, boolean isSca) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem && stack.getCount() > 0) {
            if (!isValidItem(stack)) return false;

            var string = stack.getHoverName().getString();
            var block = ((BlockItem) stack.getItem()).getBlock();

            if (isSca && block instanceof TntBlock) {
                return false;
            }

            if (string.contains("Click") || string.contains("点击")) return false;

            if (block instanceof FlowerBlock) {
                return false;
            }

            if (block instanceof BushBlock) {
                return false;
            }

            if (block instanceof NetherFungusBlock) {
                return false;
            }

            if (block instanceof CropBlock) {
                return false;
            }

            if (block instanceof SlabBlock) {
                return false;
            }

            return !blacklistedBlocks.contains(block);
        }

        return false;
    }

    public static boolean isGodItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.is(net.minecraft.tags.ItemTags.AXES) && stack.getItem() == Items.GOLDEN_AXE) {
            if (getEnchantLevel(stack, Enchantments.SHARPNESS) > 100) {
                return true;
            }
        }

        if (stack.getItem() == Items.SLIME_BALL) {
            if (getEnchantLevel(stack, Enchantments.KNOCKBACK) > 1) {
                return true;
            }
        }

        if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            return true;
        }

        return stack.getItem() == Items.END_CRYSTAL;
    }

    public static boolean isOtherItemUseful(ItemStack stack, boolean isStealing) {
        if (stack.isEmpty()) return false;

        if (isGodItem(stack)) return true;

        if (stack.getHoverName().getString().contains("点击使用")) return true;

        if (stack.getItem() == Items.TNT) return true;

        if (stack.getItem() instanceof net.minecraft.world.item.PotionItem) {
            PotionContents potionContents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

            Iterable<MobEffectInstance> potion = potionContents.getAllEffects();

            for (MobEffectInstance effectInstance : potion) {
                MobEffect effect = effectInstance.getEffect().value();
                if (effect.getDescriptionId().contains("jump_boost")) {
                    return true;
                }

            }

        }

        if (stack.getItem() == Items.COMPASS) return true;

        if (stack.getItem() instanceof net.minecraft.world.item.FireChargeItem) return true;

        if (stack.is(net.minecraft.tags.ItemTags.SHOVELS)) {
            return InventoryUtils.getBestShovel() == stack;
        }

        if (stack.getItem() == Items.WATER_BUCKET) {
            return (isStealing && InventoryUtils.getItemCount(Items.WATER_BUCKET) < 1) || (!isStealing && InventoryUtils.getItemCount(Items.WATER_BUCKET) == 1);
        }

        if (stack.getItem() == Items.LAVA_BUCKET) {
            return (isStealing && InventoryUtils.getItemCount(Items.LAVA_BUCKET) < 1) || (!isStealing && InventoryUtils.getItemCount(Items.LAVA_BUCKET) == 1);
        }

        if (stack.getItem() == Items.BUCKET) {
            return (isStealing && InventoryUtils.getItemCount(Items.BUCKET) < 1) || (!isStealing && InventoryUtils.getItemCount(Items.BUCKET) == 1);
        }

        return false;
    }

    public static boolean isNotInBlockBlacklist(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return itemStack.getItem() instanceof BlockItem blockItem
                && ItemUtils.isNotInBlockBlacklist(blockItem.getBlock());
    }

    public static boolean isNotInBlockBlacklist(Block block) {
        BlockState blockState = block.defaultBlockState();
        if (block == Blocks.SNOW
                && (!blockState.hasProperty(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) || blockState.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) < 8)) {
            return false;
        }

        return !(block instanceof LiquidBlock)
                && !(block instanceof AirBlock)
                && !(block instanceof LadderBlock)
                && !(block instanceof WebBlock)
                && !(block instanceof TntBlock)
                && !(block instanceof BushBlock)
                && !(block instanceof FlowerPotBlock)
                && !(block instanceof SlabBlock)
                && !(block instanceof StairBlock)
                && !(block instanceof FenceBlock)
                && !(block instanceof WallBlock)
                && !(block instanceof CarpetBlock)
                && !(block instanceof IronBarsBlock)
                && !(block instanceof SignBlock)
                && !(block instanceof BasePressurePlateBlock)
                && !(block instanceof ButtonBlock)
                && !(block instanceof LeverBlock)
                && !(block instanceof TorchBlock)
                && !(block instanceof LanternBlock)
                && !(block instanceof DoorBlock)
                && !(block instanceof TrapDoorBlock)
                && !(block instanceof AbstractBannerBlock)
                && !(block instanceof SkullBlock)
                && !(block instanceof BedBlock)
                && !(block instanceof CakeBlock)
                && !(block instanceof BrewingStandBlock)
                && !(block instanceof HopperBlock)
                && !(block instanceof DispenserBlock)
                && !(block instanceof DaylightDetectorBlock)
                && !(block instanceof BeaconBlock)
                && !(block instanceof ShulkerBoxBlock)
                && !(block instanceof BarrelBlock)
                && !(block instanceof SmokerBlock)
                && !(block instanceof BlastFurnaceBlock)
                && !(block instanceof RepeaterBlock)
                && !(block instanceof ComparatorBlock)
                && !(block instanceof TripWireBlock)
                && !(block instanceof TripWireHookBlock)
                && !(block instanceof EndPortalFrameBlock)
                && !(block instanceof EndPortalBlock)
                && !(block instanceof AbstractCauldronBlock)
                && !(block instanceof BellBlock)
                && !(block instanceof ComposterBlock)
                && !(block instanceof LecternBlock)
                && !(block instanceof GrindstoneBlock)
                && !(block instanceof StonecutterBlock)
                && !(block instanceof CampfireBlock)
                && !(block instanceof StructureVoidBlock)
                && !(block instanceof BarrierBlock)
                && !(block instanceof LightBlock)
                && !(block instanceof net.minecraft.world.level.block.piston.PistonHeadBlock)
                && !(block instanceof net.minecraft.world.level.block.piston.MovingPistonBlock)
                && !(block instanceof FallingBlock);
    }

    public static boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.getComponents().get(DataComponents.FOOD) != null;
    }

    public static int getEnchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty() || mc.level == null) {
            return 0;
        }

        return EnchantmentHelper.getItemEnchantmentLevel(mc.level.registryAccess().lookupOrThrow(key.registryKey()).getOrThrow(key), stack);
    }

    public static boolean isArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR;
    }

    public static EquipmentType getEquipmentType(ItemStack stack) {
        if (!isArmor(stack)) {
            return null;
        }

        return switch (stack.get(DataComponents.EQUIPPABLE).slot()) {
            case HEAD -> EquipmentType.HELMET;
            case CHEST -> EquipmentType.CHESTPLATE;
            case LEGS -> EquipmentType.LEGGINGS;
            case FEET -> EquipmentType.BOOTS;
            default -> null;
        };
    }

    public static boolean isConsumable(ItemStack stack) {
        return isFood(stack) || stack.is(Items.POTION) || stack.is(Items.MILK_BUCKET);
    }

    public static FoodProperties getFoodComponent(ItemStack stack) {
        return stack.get(DataComponents.FOOD);
    }
}
