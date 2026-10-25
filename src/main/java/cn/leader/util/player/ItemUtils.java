package cn.leader.util.player;

import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.*;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;

import java.util.Arrays;
import java.util.List;

import static cn.leader.util.IMinecraft.mc;


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

            var string = itemStack.getName().getString();

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

            var string = stack.getName().getString();
            var block = ((BlockItem) stack.getItem()).getBlock();

            if (isSca && block instanceof TntBlock) {
                return false;
            }

            if (string.contains("Click") || string.contains("点击")) return false;

            if (block instanceof FlowerBlock) {
                return false;
            }

            if (block instanceof PlantBlock) {
                return false;
            }

            if (block instanceof FungusBlock) {
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

        if (stack.getItem() instanceof AxeItem && stack.getItem() == Items.GOLDEN_AXE) {
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

        if (stack.getName().getString().contains("点击使用")) return true;

        if (stack.getItem() == Items.TNT) return true;

        if (stack.getItem() instanceof PotionItem) {
            PotionContentsComponent potionContents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            Iterable<StatusEffectInstance> potion = potionContents.getEffects();

            for (StatusEffectInstance effectInstance : potion) {
                StatusEffect effect = effectInstance.getEffectType().value();
                if (effect.getName().getString().contains("jump_boost")) {
                    return true;
                }
//                if (effect.getCategory() == StatusEffectCategory.HARMFUL)
            }
//            return Client.moduleManager.getModule(InventoryManager.class).keepPotion.get();
        }

        if (stack.getItem() == Items.COMPASS) return true;

        if (stack.getItem() instanceof FireChargeItem) return true;

        if (stack.getItem() instanceof ShovelItem) {
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
        BlockState blockState = block.getDefaultState();
        if (block instanceof SnowBlock
                && (!blockState.contains(SnowBlock.LAYERS) || blockState.get(SnowBlock.LAYERS) < 8)) {
            return false;
        }

        return !(block instanceof FluidBlock)
                && !(block instanceof AirBlock)
                && !(block instanceof LadderBlock)
                && !(block instanceof CobwebBlock)
                && !(block instanceof TntBlock)
                && !(block instanceof PlantBlock)
                && !(block instanceof FlowerPotBlock)
                && !(block instanceof SlabBlock)
                && !(block instanceof StairsBlock)
                && !(block instanceof FenceBlock)
                && !(block instanceof WallBlock)
                && !(block instanceof CarpetBlock)
                && !(block instanceof PaneBlock)
                && !(block instanceof SignBlock)
                && !(block instanceof AbstractPressurePlateBlock)
                && !(block instanceof ButtonBlock)
                && !(block instanceof LeverBlock)
                && !(block instanceof TorchBlock)
                && !(block instanceof LanternBlock)
                && !(block instanceof DoorBlock)
                && !(block instanceof TrapdoorBlock)
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
                && !(block instanceof TripwireBlock)
                && !(block instanceof TripwireHookBlock)
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
                && !(block instanceof PistonHeadBlock)
                && !(block instanceof PistonExtensionBlock)
                && !(block instanceof FallingBlock);
    }

    public static boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.getComponents().get(DataComponentTypes.FOOD) != null;
    }

    public static int getEnchantLevel(ItemStack stack, RegistryKey<Enchantment> key) {
        if (stack.isEmpty() || mc.world == null) {
            return 0;
        }

        return EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(key.getRegistryRef()).getOrThrow(key), stack);
    }

    public static EquipmentType getEquipmentType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
            return null;
        }

        var comp = stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);

        if (comp == null) {
            return null;
        }

        for (var entry : comp.modifiers()) {
            if (entry.attribute().matchesKey(EntityAttributes.ARMOR.getKey().orElseThrow()) || entry.attribute().matchesKey(EntityAttributes.ARMOR_TOUGHNESS.getKey().orElseThrow())) {
                return switch (entry.slot()) {
                    case HEAD -> EquipmentType.HELMET;
                    case CHEST -> EquipmentType.CHESTPLATE;
                    case LEGS -> EquipmentType.LEGGINGS;
                    case FEET -> EquipmentType.BOOTS;
                    default -> null;
                };
            }
        }

        return null;
    }

    public static boolean isConsumable(ItemStack stack) {
        return isFood(stack) || stack.isOf(Items.POTION) || stack.isOf(Items.MILK_BUCKET);
    }

    public static FoodComponent getFoodComponent(ItemStack stack) {
        return stack.get(DataComponentTypes.FOOD);
    }
}
