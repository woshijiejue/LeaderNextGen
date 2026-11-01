package cn.leader.module.impl.player;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.misc.TimerUtil;
import cn.leader.util.player.ClickSlotUtils;
import cn.leader.util.player.InventoryUtils;
import cn.leader.util.player.ItemUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.StandingAndWallBlockItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ChestStealer extends Module {

    private final NumberValue minDelay = new NumberValue("Min Delay", 110, 0, 1000, 50);
    private final NumberValue maxDelay = new NumberValue("Max Delay", 140, 0, 1000, 50);
    private final BoolValue autoClose = new BoolValue("Auto Close", true);
    private final NumberValue closeDelay = new NumberValue("Close Delay", 100, 0, 1000, 1,
            () -> this.autoClose.getValue());
    private final BoolValue enderChest = new BoolValue("Ender Chest", false);

    private final TimerUtil timer = new TimerUtil();
    private final Random random = new Random();

    private Screen lastScreen;

    public ChestStealer() {
        super("ChestStealer", Category.Player);
        setDescription("Loots the content of chests");
    }

    @Override
    public void onEnable() {
        this.lastScreen = null;
        this.timer.reset();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null) return;

        Screen screen = mc.gui.screen();

        if (screen instanceof AbstractContainerScreen<?> container
                && container.getMenu() instanceof ChestMenu menu) {
            if (screen != this.lastScreen) {
                this.timer.reset();
            } else {
                handleChest(container, menu);
            }
        }

        this.lastScreen = screen;
    }

    private void handleChest(AbstractContainerScreen<?> container, ChestMenu menu) {
        String title = container.getTitle().getString();
        String chest = Component.translatable("container.chest").getString();
        String largeChest = Component.translatable("container.chestDouble").getString();
        String ender = Component.translatable("container.enderchest").getString();

        boolean target = title.equals(chest) || title.equals(largeChest) || title.equals("Chest")
                || (this.enderChest.getValue() && title.equals(ender));
        if (!target) return;

        int nextDelay = randomDelay();

        if (isChestEmpty(menu)) {
            if (this.timer.hasTimeElapsed(nextDelay)) {
                if (this.autoClose.getValue() && this.timer.hasTimeElapsed(this.closeDelay.getValue())) {
                    mc.player.closeContainer();
                    this.timer.reset();
                }
            }
            return;
        }

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, this.random);

        for (int slotId : slots) {
            ItemStack stack = menu.getSlot(slotId).getItem();
            if (!isItemUseful(stack) || !isBestItemInChest(menu, stack)) continue;
            if (!this.timer.hasTimeElapsed(nextDelay)) continue;

            ClickSlotUtils.shiftClick(menu.containerId, slotId);
            this.timer.reset();

            if (nextDelay > 0) break;
        }
    }

    private int randomDelay() {
        int min = Math.min(this.minDelay.getValue().intValue(), this.maxDelay.getValue().intValue());
        int max = Math.max(this.minDelay.getValue().intValue(), this.maxDelay.getValue().intValue());

        return min == max ? min : min + this.random.nextInt(max - min + 1);
    }

    public boolean isWorking() {
        return !this.timer.hasTimeElapsed(150L);
    }

    private boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (ItemUtils.isGodItem(stack) || ItemUtils.isSharpnessAxe(stack)) return true;

        if (ItemUtils.isArmor(stack)) {
            var type = ItemUtils.getEquipmentType(stack);
            if (type == null) return false;
            return InventoryUtils.getProtection(stack) > InventoryUtils.getBestArmorScore(type);
        }

        if (stack.is(ItemTags.SWORDS)) {
            return InventoryUtils.getSwordDamage(stack) > InventoryUtils.getBestSwordDamage();
        }
        if (stack.is(ItemTags.PICKAXES)) {
            return InventoryUtils.getToolScore(stack) > bestToolScore(ItemTags.PICKAXES);
        }
        if (stack.is(ItemTags.AXES)) {
            return InventoryUtils.getToolScore(stack) > bestToolScore(ItemTags.AXES);
        }
        if (stack.is(ItemTags.SHOVELS)) {
            return InventoryUtils.getToolScore(stack) > bestToolScore(ItemTags.SHOVELS);
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return InventoryUtils.getCrossbowScore(stack) > InventoryUtils.getBestCrossbowScore();
        }
        if (stack.getItem() instanceof BowItem && InventoryUtils.getPunchLevel(stack) > 0) {
            return InventoryUtils.getPunchBowScore(stack) > InventoryUtils.getBestPunchBowScore();
        }
        if (stack.getItem() instanceof BowItem && InventoryUtils.getPowerLevel(stack) > 0) {
            return InventoryUtils.getPowerBowScore(stack) > InventoryUtils.getBestPowerBowScore();
        }
        if (stack.getItem() == Items.COMPASS) {
            return !InventoryUtils.has(Items.COMPASS);
        }
        if (stack.getItem() == Items.WATER_BUCKET
                && InventoryUtils.getItemCount(Items.WATER_BUCKET) >= InvManager.waterBucketKeep()) {
            return false;
        }
        if (stack.getItem() == Items.LAVA_BUCKET
                && InventoryUtils.getItemCount(Items.LAVA_BUCKET) >= InvManager.lavaBucketKeep()) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem && ItemUtils.isValidBlock(stack)
                && InventoryUtils.getBlockCountInInventory() + stack.getCount() >= InvManager.maxBlockSize()) {
            return false;
        }
        if (stack.getItem() == Items.ARROW
                && InventoryUtils.getItemCount(Items.ARROW) + stack.getCount() >= InvManager.maxArrowSize()) {
            return false;
        }
        if (stack.getItem() instanceof FishingRodItem
                && InventoryUtils.getItemCount(Items.FISHING_ROD) >= 1) {
            return false;
        }
        if (stack.getItem() == Items.SNOWBALL || stack.getItem() == Items.EGG) {
            if (!InvManager.keepProjectiles()) return false;
            return InventoryUtils.getItemCount(Items.SNOWBALL) + InventoryUtils.getItemCount(Items.EGG)
                    + stack.getCount() < InvManager.maxProjectileSize();
        }

        return !(stack.getItem() instanceof StandingAndWallBlockItem) && ItemUtils.isCommonItemUseful(stack);
    }

    private boolean isBestItemInChest(ChestMenu menu, ItemStack stack) {
        if (ItemUtils.isGodItem(stack) || ItemUtils.isSharpnessAxe(stack)) return true;

        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack other = menu.getSlot(i).getItem();
            if (other.isEmpty() || other == stack) continue;

            if (ItemUtils.isArmor(stack) && ItemUtils.isArmor(other)) {
                if (ItemUtils.getEquipmentType(stack) == ItemUtils.getEquipmentType(other)
                        && InventoryUtils.getProtection(other) > InventoryUtils.getProtection(stack)) {
                    return false;
                }
            } else if (stack.is(ItemTags.SWORDS) && other.is(ItemTags.SWORDS)) {
                if (InventoryUtils.getSwordDamage(other) > InventoryUtils.getSwordDamage(stack)) {
                    return false;
                }
            } else if (stack.is(ItemTags.AXES) && other.is(ItemTags.AXES)) {
                if (InventoryUtils.getToolScore(other) > InventoryUtils.getToolScore(stack)) {
                    return false;
                }
            } else if (stack.is(ItemTags.PICKAXES) && other.is(ItemTags.PICKAXES)) {
                if (InventoryUtils.getToolScore(other) > InventoryUtils.getToolScore(stack)) {
                    return false;
                }
            } else if (stack.is(ItemTags.SHOVELS) && other.is(ItemTags.SHOVELS)) {
                if (InventoryUtils.getToolScore(other) > InventoryUtils.getToolScore(stack)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isChestEmpty(ChestMenu menu) {
        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty() && isItemUseful(stack) && isBestItemInChest(menu, stack)) {
                return false;
            }
        }

        return true;
    }

    private static float bestToolScore(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        return InventoryUtils.getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.is(tag) && ItemUtils.isValidItem(stack))
                .map(InventoryUtils::getToolScore)
                .max(Float::compareTo)
                .orElse(0.0F);
    }
}
