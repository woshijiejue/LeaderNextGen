package cn.leader.util.player;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cn.leader.util.IMinecraft.mc;
import static cn.leader.util.player.ItemUtils.getEnchantLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.BlockItem;

public final class InventoryUtils {

    public static final int HELMET_SLOT_INVENTORY = 39, HELMET_SLOT_INTERACTION_MANAGER = 5;
    public static final int CHEST_PLATE_SLOT_INVENTORY = 38, CHEST_PLATE_SLOT_INTERACTION_MANAGER = 6;
    public static final int LEGGINGS_SLOT_INVENTORY = 37, LEGGINGS_SLOT_INTERACTION_MANAGER = 7;
    public static final int BOOTS_SLOT_INVENTORY = 36, BOOTS_SLOT_INTERACTION_MANAGER = 8;

    @SuppressWarnings("unused")
    public static final int OFF_HAND_SLOT_INVENTORY = 40;

    public static boolean serverOpenContainer, serverOpenInventory;

    @SuppressWarnings("DataFlowIssue")
    public static boolean playerInventoryHasEmptySlot() {
        for (int slotID = 0; slotID < mc.player.getInventory().getContainerSize(); ++slotID) {
            if (slotID == HELMET_SLOT_INVENTORY || slotID == CHEST_PLATE_SLOT_INVENTORY || slotID == LEGGINGS_SLOT_INVENTORY || slotID == BOOTS_SLOT_INVENTORY || slotID == OFF_HAND_SLOT_INVENTORY)
                continue;

            ItemStack itemStack = mc.player.getInventory().getItem(slotID);

            if (itemStack == null) return true;
            if (itemStack.isEmpty()) return true;
        }

        return false;
    }

    public static boolean playerHotbarHasEmptySlot() {
        if (mc.player == null) return false;
        for (int slotID = 0; slotID < 9; slotID++) {
            ItemStack itemStack = mc.player.getInventory().getItem(slotID);

            if (itemStack == null) return true;
            if (itemStack.isEmpty()) return true;
        }

        return false;
    }
    public static boolean isUsefulItem(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (itemStack.isEmpty()) return false;

        Item item = itemStack.getItem();

        if (ItemUtils.isArmor(itemStack)) return true;
        if (new ItemStack(item).is(net.minecraft.tags.ItemTags.CROSSBOW_ENCHANTABLE)) return true;
        if (new ItemStack(item).is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE)) return true;
        if (new ItemStack(item).is(net.minecraft.tags.ItemTags.AXES)) return true;
        if (new ItemStack(item).is(net.minecraft.tags.ItemTags.PICKAXES)) return true;
        if (new ItemStack(item).is(net.minecraft.tags.ItemTags.SHOVELS)) return true;
        if (item instanceof net.minecraft.world.item.HoeItem) return true;
        if (itemStack.is(net.minecraft.tags.ItemTags.SWORDS)) return true;

        if (item instanceof net.minecraft.world.item.PotionItem) return true;
        if (item instanceof net.minecraft.world.item.PlayerHeadItem) return true;

        if (item instanceof BlockItem) {
            Block block = Block.byItem(item);
            if (block instanceof AirBlock) return false;
            if (block instanceof StairBlock) return false;
            if (block instanceof SlabBlock) return false;
            if (block instanceof FenceBlock) return false;
            if (block instanceof FenceGateBlock) return false;
            if (block instanceof DoorBlock) return false;
            if (block instanceof TrapDoorBlock) return false;
            if (block instanceof PressurePlateBlock) return false;
            if (block instanceof ButtonBlock) return false;
            if (block instanceof WallBlock) return false;
            if (block instanceof CarpetBlock) return false;
            if (block instanceof ConcretePowderBlock) return false;
            if (block instanceof TintedGlassBlock) return false;
            if (block instanceof StainedGlassBlock) return false;
            if (block instanceof StainedGlassPaneBlock) return false;
            if (block instanceof BannerBlock) return false;
            if (block instanceof LeavesBlock) return false;
            if (block instanceof SaplingBlock) return false;
            if (block instanceof MushroomBlock) return false;
            if (block instanceof HugeMushroomBlock) return false;
            if (block instanceof FlowerBlock) return false;
            if (block instanceof ComposterBlock) return false;
            if (block instanceof NoteBlock) return false;
            if (block instanceof JukeboxBlock) return false;
            if (block instanceof SignBlock) return false;
            if (block == Blocks.IRON_BARS) return false;
            if (block == Blocks.IRON_CHAIN) return false;
            if (block == Blocks.GLASS_PANE) return false;
            if (block == Blocks.DIRT_PATH) return false;
            if (block == Blocks.FARMLAND) return false;
            if (block == Blocks.SNOW) return false;
            if (block == Blocks.AMETHYST_CLUSTER) return false;
            if (block == Blocks.MANGROVE_ROOTS) return false;
            if (block == Blocks.SUGAR_CANE) return false;
            if (block == Blocks.CACTUS) return false;
            return block != Blocks.LILY_PAD;
        }

        if (itemStack.getItem() == Items.SLIME_BALL) return true;
        if (itemStack.getItem() == Items.FIRE_CHARGE)
            return true;
        if (itemStack.getItem() == Items.TOTEM_OF_UNDYING)
            return true;
        if (itemStack.getItem() == Items.GOLDEN_APPLE)
            return true;
        if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE)
            return true;
        if (itemStack.getItem() == Items.FISHING_ROD)
            return true;
        if (itemStack.getItem() == Items.SNOWBALL) return true;
        if (itemStack.getItem() == Items.EGG) return true;
        if (itemStack.getItem() == Items.BUCKET) return true;
        if (itemStack.getItem() == Items.WATER_BUCKET)
            return true;
        if (itemStack.getItem() == Items.LAVA_BUCKET)
            return true;
        if (itemStack.getItem() == Items.MILK_BUCKET)
            return true;
        if (itemStack.getItem() == Items.ENDER_PEARL)
            return true;
        if (itemStack.getItem() == Items.ARROW) return true;
        if (itemStack.getItem() == Items.SHIELD) return true;
        if (itemStack.getItem() == Items.END_CRYSTAL)
            return true;
        if (itemStack.has(DataComponents.FOOD)) {
            return true;
        }

        return false;
    }
    @SuppressWarnings("RedundantIfStatement")

    public static float getBestSwordDamage() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.SWORDS) && ItemUtils.isValidItem(item)).map(InventoryUtils::getSwordDamage).max(Float::compareTo).orElse(0f);
    }

    public static float getBestAxeScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.AXES) && ItemUtils.isValidItem(item)).map(InventoryUtils::getAxeScore).max(Float::compareTo).orElse(0f);
    }

    public static float getBestPickaxeScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.PICKAXES) && ItemUtils.isValidItem(item)).map(InventoryUtils::getToolScore).max(Float::compareTo).orElse(0f);
    }

    public static float getBestCrossbowScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.CROSSBOW_ENCHANTABLE) && ItemUtils.isValidItem(item)).map(InventoryUtils::getCrossbowScore).max(Float::compareTo).orElse(0f);
    }

    public static float getBestPowerBowScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE) && ItemUtils.isValidItem(item)).map(InventoryUtils::getPowerBowScore).max(Float::compareTo).orElse(0f);
    }

    public static float getBestPunchBowScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE) && ItemUtils.isValidItem(item)).map(InventoryUtils::getPunchBowScore).max(Float::compareTo).orElse(0f);
    }

    public static float getBestArmorScore(EquipmentType type) {
        return getAllItems().stream().filter(item -> !item.isEmpty() && ItemUtils.isArmor(item) && ItemUtils.getEquipmentType(item) == type).map(InventoryUtils::getProtection).max(Float::compareTo).orElse(0f);
    }

    public static int getBlockCountInInventory() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && ItemUtils.isValidBlock(item)).mapToInt(ItemStack::getCount).sum();
    }

    public static int getFoodCountInInventoryExcludingGapple() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && ItemUtils.isFood(item) && item.getItem() != Items.GOLDEN_APPLE && item.getItem() != Items.ENCHANTED_GOLDEN_APPLE && ItemUtils.isValidItem(item)).mapToInt(ItemStack::getCount).sum();
    }

    public static ItemStack getBestSword() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.SWORDS) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(stack -> (int) (getSwordDamage(stack) * 100))).orElse(null);
    }

    public static ItemStack getBestAxe() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.AXES) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getAxeScore(s) * 100))).orElse(null);
    }

    public static ItemStack getBestPickaxe() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.PICKAXES) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getToolScore(s) * 100))).orElse(null);
    }

    public static ItemStack getBestShovel() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.SHOVELS) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getToolScore(s) * 100))).orElse(null);
    }

    public static ItemStack getBestCrossbow() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.CROSSBOW_ENCHANTABLE) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getCrossbowScore(s) * 100))).orElse(null);
    }

    public static ItemStack getBestPunchBow() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getPunchBowScore(s) * 100))).orElse(null);
    }

    public static ItemStack getBestPowerBow() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getPowerBowScore(s) * 100))).orElse(null);
    }

    public static ItemStack getBestGapple() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && (item.getItem() == Items.GOLDEN_APPLE || item.getItem() == Items.ENCHANTED_GOLDEN_APPLE) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public static ItemStack getBestEnderPearl() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() == Items.ENDER_PEARL && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public static ItemStack getBestBlock() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && ItemUtils.isValidBlock(item)).max(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public static ItemStack getWorstBlock() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && ItemUtils.isValidBlock(item)).min(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public static ItemStack getBestThrowable() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public static ItemStack getWorstThrowable() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && ItemUtils.isValidItem(item)).min(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public static ItemStack getWorstFoodExcludingGapple() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && ItemUtils.isFood(item) && item.getItem() != Items.GOLDEN_APPLE && item.getItem() != Items.ENCHANTED_GOLDEN_APPLE && ItemUtils.isValidItem(item)).min(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public static int getItemCount(Item checkItem) {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() == checkItem).mapToInt(ItemStack::getCount).sum();
    }

    public static boolean has(Item checkItem) {
        return getAllItems().stream().anyMatch(item -> item.getItem() == checkItem);
    }

    public static float getSwordDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.is(net.minecraft.tags.ItemTags.SWORDS))) {
            return 0;
        }

        float valence = 0;
        var comp = stack.getComponents().get(DataComponents.ATTRIBUTE_MODIFIERS);

        if (comp != null) {
            double add = 0;
            double addBase = 0;
            double addTotal = 0;

            for (var entry : comp.modifiers()) {
                if (entry.slot() == EquipmentSlotGroup.MAINHAND && entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                    switch (entry.modifier().operation()) {
                        case ADD_VALUE -> add += entry.modifier().amount();
                        case ADD_MULTIPLIED_BASE -> addBase += entry.modifier().amount();
                        case ADD_MULTIPLIED_TOTAL -> addTotal += entry.modifier().amount();
                    }
                }
            }

            valence += (float) ((add * (1 + addBase)) * (1 + addTotal));
        }

        int itemEnchantmentLevel = getEnchantLevel(stack, Enchantments.SHARPNESS);

        if (itemEnchantmentLevel > 0) {
            valence += 0.5f * itemEnchantmentLevel + 0.5f;
        }

        return valence;
    }

    public static float getAttackDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0f;
        }

        float damage = 0f;

        var attrComp = stack.getComponents().get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attrComp != null) {
            for (var entry : attrComp.modifiers()) {
                if (entry.slot() == EquipmentSlotGroup.MAINHAND && entry.attribute().is(Attributes.ATTACK_DAMAGE)) {

                    var mod = entry.modifier();
                    switch (mod.operation()) {
                        case ADD_VALUE -> damage += mod.amount();
                        case ADD_MULTIPLIED_BASE -> damage += mod.amount();
                        case ADD_MULTIPLIED_TOTAL -> damage += mod.amount();
                    }
                }
            }
        }

        int sharpnessLevel = getEnchantLevel(stack, Enchantments.SHARPNESS);
        if (sharpnessLevel > 0) {
            damage += 0.5f * sharpnessLevel + 0.5f;
        }

        return damage;
    }

    public static float getWeaponDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0F;
        }

        float damage = Math.max(getSwordDamage(stack), getAttackDamage(stack));

        if (damage <= 0F) {
            damage = fallbackDamage(stack);
        }

        return damage;
    }

    private static float fallbackDamage(ItemStack stack) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return 0F;

        String id = key.getPath();

        if (id.contains("netherite")) return 8F;
        if (id.contains("diamond")) return 7F;
        if (id.contains("iron")) return 6F;
        if (id.contains("stone")) return 5F;
        if (id.contains("golden") || id.contains("wooden")) return 4F;

        return 0F;
    }

    public static float getWeaponScore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0F;
        if (!stack.is(ItemTags.SWORDS) && !stack.is(ItemTags.AXES)) return 0F;

        float score = getWeaponDamage(stack);
        score += getEnchantLevel(stack, Enchantments.FIRE_ASPECT) * 3.0F;
        score += stack.getMaxDamage() * 0.0005F;

        // 属性/材质都读不出来时给个保底，避免整把武器被判成 0 分而永远不捡
        return Math.max(score, 1.0F);
    }

    public static float getBestSwordWeaponScore() {
        return getAllItems().stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.SWORDS) && ItemUtils.isValidItem(item))
                .map(InventoryUtils::getWeaponScore)
                .max(Float::compareTo)
                .orElse(0F);
    }

    public static float getBestAxeWeaponScore() {
        return getAllItems().stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.AXES) && ItemUtils.isValidItem(item))
                .map(InventoryUtils::getWeaponScore)
                .max(Float::compareTo)
                .orElse(0F);
    }

    public static float getBestWeaponScore() {
        return getAllItems().stream()
                .filter(item -> !item.isEmpty() && ItemUtils.isValidItem(item))
                .map(InventoryUtils::getWeaponScore)
                .max(Float::compareTo)
                .orElse(0F);
    }

    public static float getAxeScore(ItemStack stack) {
        float valence = getToolScore(stack);

        if (ItemUtils.isGodItem(stack)) {
            return valence * 1000;
        }

        int itemEnchantmentLevel = getEnchantLevel(stack, Enchantments.SHARPNESS);

        if (itemEnchantmentLevel > 0) {
            valence += 0.5f * itemEnchantmentLevel + 0.5f;
        }

        return valence;
    }

    public static float getToolScore(ItemStack stack) {
        float valence = 0;

        if (stack == null) {
            return 0;
        }

        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.is(ItemTags.PICKAXES)) {
            valence += stack.getDestroySpeed(Blocks.STONE.defaultBlockState());
        } else if (stack.is(ItemTags.AXES)) {
            valence += stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState());
        } else if (stack.is(ItemTags.SHOVELS)) {
            valence += stack.getDestroySpeed(Blocks.DIRT.defaultBlockState());
        } else {
            return 0;
        }

        int efficiency = getEnchantLevel(stack, Enchantments.EFFICIENCY);

        if (efficiency > 0) {
            valence += efficiency * 0.0075F;
        }

        return valence;
    }

    public static float getCrossbowScore(ItemStack stack) {
        int valence = 0;

        if (stack == null) {
            return 0;
        }

        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.is(net.minecraft.tags.ItemTags.CROSSBOW_ENCHANTABLE)) {
            valence += getEnchantLevel(stack, Enchantments.QUICK_CHARGE);
            valence += getEnchantLevel(stack, Enchantments.MULTISHOT);
            valence += getEnchantLevel(stack, Enchantments.PIERCING);
        }

        return valence;
    }

    public static float getPunchBowScore(ItemStack stack) {
        if (stack == null) {
            return 0;
        }

        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE)) {
            float valence = 10;
            valence += getEnchantLevel(stack, Enchantments.PUNCH);
            valence += getEnchantLevel(stack, Enchantments.INFINITY);
            valence += getEnchantLevel(stack, Enchantments.FLAME);
            valence += getEnchantLevel(stack, Enchantments.POWER) / 10F;
            return valence + ((float) stack.getDamageValue() / stack.getMaxDamage());
        }

        return 0;
    }

    public static float getPowerBowScore(ItemStack stack) {
        if (stack == null) {
            return 0;
        }

        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE)) {
            float valence = 10;
            valence += getEnchantLevel(stack, Enchantments.PUNCH) / 10F;
            valence += getEnchantLevel(stack, Enchantments.INFINITY);
            valence += getEnchantLevel(stack, Enchantments.FLAME);
            valence += getEnchantLevel(stack, Enchantments.POWER);
            return valence + ((float) stack.getDamageValue() / stack.getMaxDamage());
        }

        return 0;
    }

    public static float getProtection(ItemStack stack) {
        float valence = 0;

        if (stack == null || stack.isEmpty()) {
            return valence;
        }

        var modifiers = stack.getComponents().get(DataComponents.ATTRIBUTE_MODIFIERS);

        if (modifiers != null) {
            for (var entry : modifiers.modifiers()) {
                if (entry.attribute().is(Attributes.ARMOR)) {
                    var mod = entry.modifier();

                    if (mod.operation() == AttributeModifier.Operation.ADD_VALUE) {
                        valence += (float) mod.amount();
                    }
                }

                if (entry.attribute().is(Attributes.ARMOR_TOUGHNESS)) {
                    var mod = entry.modifier();

                    if (mod.operation() == AttributeModifier.Operation.ADD_VALUE) {
                        valence += (float) mod.amount();
                    }
                }
            }
        }

        valence += getEnchantLevel(stack, Enchantments.PROTECTION);

        return valence;
    }

    public static List<ItemStack> getAllItems() {
        ArrayList<ItemStack> list = new ArrayList<>(41);

        if (mc.player != null) {
            list.addAll(mc.player.getInventory().getNonEquipmentItems());
            list.add(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND));
            list.add(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
            list.add(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
            list.add(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
            list.add(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        }

        return list;
    }

    public static ItemStack getStackInHotbarSlot(int slot) {
        assert mc.player != null;

        if (slot >= 0 && slot < 9) {
            return mc.player.getInventory().getItem(slot);
        }

        return ItemStack.EMPTY;
    }

    public static boolean shouldNotWork() {
        return InventoryUtils.getAllItems().stream().anyMatch(item -> {
            if (!item.isEmpty()) {
                var string = item.getHoverName().getString();
                return string.contains("长按点击") || string.contains("点击使用") || string.contains("离开游戏") || string.contains("选择一个队伍") || string.contains("再来一局");
            }

            return false;
        });
    }

    public static boolean isGodAxe(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {

            return stack.getItem() == Items.GOLDEN_AXE && getEnchantLevel(stack, Enchantments.SHARPNESS) > 100;
        }
    }

    public static boolean isEnchantedGApple(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    public static boolean isEndCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.END_CRYSTAL;
    }

    public static boolean isKBBall(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return stack.getItem() == Items.SLIME_BALL && getEnchantLevel(stack, Enchantments.KNOCKBACK) > 1;
        }
    }

    public static boolean isKBStick(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return stack.getItem() == Items.STICK && getEnchantLevel(stack, Enchantments.KNOCKBACK) > 1;
        }
    }

    public static int getPunchLevel(ItemStack stack) {
        return getEnchantLevel(stack, Enchantments.PUNCH);
    }

    public static int getPowerLevel(ItemStack stack) {
        return getEnchantLevel(stack, Enchantments.POWER);
    }

    private InventoryUtils() {
    }

}
