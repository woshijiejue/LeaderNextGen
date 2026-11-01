package cn.leader.module.impl.player;

import cn.leader.event.base.annotation.EventPriority;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.PacketEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.impl.movement.Scaffold;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.misc.TimerUtil;
import cn.leader.util.player.ClickSlotUtils;
import cn.leader.util.player.EquipmentType;
import cn.leader.util.player.InventoryUtils;
import cn.leader.util.player.ItemUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class InvManager extends Module {

    private static InvManager module;

    private final ModeValue mode = new ModeValue("Mode", "Silent", "Silent", "Inventory");
    private final BoolValue pauseOnEat = new BoolValue("Pause On Eat", true);
    private final BoolValue pauseOnKillAura = new BoolValue("Pause On KillAura", true);
    private final BoolValue pauseOnScaffold = new BoolValue("Pause On Scaffold", true);
    private final BoolValue pauseOnMove = new BoolValue("Pause On Move", true);
    private final BoolValue stopSprinting = new BoolValue("Stop Sprinting", true,
            () -> this.mode.is("Silent"));

    private final NumberValue minDelay = new NumberValue("Min Delay", 95, 0, 1000, 50);
    private final NumberValue maxDelay = new NumberValue("Max Delay", 135, 0, 1000, 50);

    private final ModeValue offhandItem = new ModeValue("Offhand Item", "None",
            "None", "Golden Apple", "Projectile", "Fishing Rod", "Block");

    private final BoolValue autoArmor = new BoolValue("Auto Armor", true);

    private final BoolValue switchSword = new BoolValue("Switch Sword", true);
    private final NumberValue swordSlot = new NumberValue("Sword Slot", 1, 1, 9, 1,
            () -> this.switchSword.getValue());

    private final BoolValue switchBlock = new BoolValue("Switch Block", true,
            () -> !this.offhandItem.is("Block"));
    private final NumberValue blockSlot = new NumberValue("Block Slot", 2, 1, 9, 1,
            () -> this.switchBlock.getValue() && !this.offhandItem.is("Block"));
    private final NumberValue maxBlockSize = new NumberValue("Max Block Size", 256, 64, 512, 64,
            () -> this.switchBlock.getValue());

    private final BoolValue switchPickaxe = new BoolValue("Switch Pickaxe", true);
    private final NumberValue pickaxeSlot = new NumberValue("Pickaxe Slot", 3, 1, 9, 1,
            () -> this.switchPickaxe.getValue());

    private final BoolValue switchAxe = new BoolValue("Switch Axe", true);
    private final NumberValue axeSlot = new NumberValue("Axe Slot", 4, 1, 9, 1,
            () -> this.switchAxe.getValue());

    private final BoolValue switchBow = new BoolValue("Switch Bow or Crossbow", true);
    private final NumberValue bowSlot = new NumberValue("Bow Slot", 5, 1, 9, 1,
            () -> this.switchBow.getValue());
    private final ModeValue bowPriority = new ModeValue("Bow Priority", "Crossbow",
            () -> this.switchBow.getValue(), "Crossbow", "Power Bow", "Punch Bow");
    private final NumberValue maxArrowSize = new NumberValue("Max Arrow Size", 256, 64, 512, 64,
            () -> this.switchBow.getValue());

    private final BoolValue switchWaterBucket = new BoolValue("Switch Water Bucket", true);
    private final NumberValue waterBucketSlot = new NumberValue("Water Bucket Slot", 6, 1, 9, 1,
            () -> this.switchWaterBucket.getValue());

    private final BoolValue switchEnderPearl = new BoolValue("Switch Ender Pearl", true);
    private final NumberValue enderPearlSlot = new NumberValue("Ender Pearl Slot", 7, 1, 9, 1,
            () -> this.switchEnderPearl.getValue());

    private final BoolValue switchFireball = new BoolValue("Switch Fireball", true);
    private final NumberValue fireballSlot = new NumberValue("Fireball Slot", 8, 1, 9, 1,
            () -> this.switchFireball.getValue());

    private final BoolValue switchGoldenApple = new BoolValue("Switch Golden Apple", true,
            () -> !this.offhandItem.is("Golden Apple"));
    private final NumberValue goldenAppleSlot = new NumberValue("Golden Apple Slot", 9, 1, 9, 1,
            () -> this.switchGoldenApple.getValue() && !this.offhandItem.is("Golden Apple"));

    private final BoolValue throwItems = new BoolValue("Throw Items", true);
    private final NumberValue waterBucketCount = new NumberValue("Keep Water Buckets", 1, 0, 5, 1,
            () -> this.throwItems.getValue());
    private final NumberValue lavaBucketCount = new NumberValue("Keep Lava Buckets", 1, 0, 5, 1,
            () -> this.throwItems.getValue());

    private final BoolValue keepProjectile = new BoolValue("Keep Eggs & Snowballs", true);
    private final BoolValue switchProjectile = new BoolValue("Switch Eggs & Snowballs", false,
            () -> this.keepProjectile.getValue() && !this.offhandItem.is("Projectile"));
    private final NumberValue projectileSlot = new NumberValue("Eggs & Snowballs Slot", 9, 1, 9, 1,
            () -> this.switchProjectile.getValue() && !this.offhandItem.is("Projectile"));
    private final NumberValue maxProjectileSize = new NumberValue("Max Eggs & Snowballs Size", 64, 16, 256, 16,
            () -> this.keepProjectile.getValue());

    private final BoolValue switchRod = new BoolValue("Switch Rod", false,
            () -> !this.offhandItem.is("Fishing Rod"));
    private final NumberValue rodSlot = new NumberValue("Rod Slot", 9, 1, 9, 1,
            () -> this.switchRod.getValue() && !this.offhandItem.is("Fishing Rod"));

    private final TimerUtil timer = new TimerUtil();
    private final Random random = new Random();

    private int noMoveTicks;
    private int stillTicks;
    private boolean clickOffHand;
    private boolean inventoryOpen;
    private boolean wasSprinting;
    private boolean actionConsumed;
    private int delayMs;
    private ActionState actionState = ActionState.IDLE;

    private enum ActionState {
        IDLE,
        WAITING_FOR_SPRINT_STOP,
        READY_TO_EXECUTE,
        WAITING_FOR_INVENTORY_CLOSE
    }

    public InvManager() {
        super("InvManager", Category.Player);
        setDescription("Sorts your inventory and offhand");
        module = this;
    }

    @Override
    public void onEnable() {
        this.clickOffHand = false;
        this.inventoryOpen = false;
        this.actionState = ActionState.IDLE;
        this.actionConsumed = false;
        this.stillTicks = 0;
        this.delayMs = randomDelay();
        this.timer.reset();
    }

    @Override
    public void onDisable() {
        boolean closePending = this.inventoryOpen && this.mode.is("Silent")
                && mc.getConnection() != null && mc.player != null;

        this.clickOffHand = false;
        this.inventoryOpen = false;
        this.actionState = ActionState.IDLE;
        this.actionConsumed = false;
        this.delayMs = 0;

        if (closePending) {
            mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;
        if (event.getType() != PacketEvent.Type.Send) return;

        if (event.getPacket() instanceof ServerboundContainerClosePacket) {
            this.inventoryOpen = false;
            if (this.actionState == ActionState.WAITING_FOR_INVENTORY_CLOSE) {
                this.actionState = ActionState.IDLE;
            }
            return;
        }

        if (!this.inventoryOpen || !this.mode.is("Silent")) return;

        if (event.getPacket() instanceof ServerboundMovePlayerPacket
                || event.getPacket() instanceof ServerboundUseItemOnPacket
                || event.getPacket() instanceof ServerboundUseItemPacket
                || event.getPacket() instanceof ServerboundInteractPacket
                || event.getPacket() instanceof ServerboundAttackPacket
                || event.getPacket() instanceof ServerboundPlayerActionPacket) {
            this.inventoryOpen = false;

            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
            }
        }
    }

    @EventTarget
    @EventPriority(1)
    public void onSprintTick(UpdateEvent event) {
        if (mc.player == null) return;

        if (stopSprinting.getValue() && this.mode.is("Silent")
                && this.actionState == ActionState.WAITING_FOR_SPRINT_STOP) {
            mc.player.setSprinting(false);
            if (!this.wasSprinting) {
                this.actionState = ActionState.READY_TO_EXECUTE;
            }
        }

        this.wasSprinting = mc.player.isSprinting();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null) return;

        this.actionConsumed = false;

        if (InventoryUtils.shouldNotWork()) {
            cancelPendingAction();
            return;
        }

        if (isMoving()) {
            this.noMoveTicks = 0;
        } else {
            this.noMoveTicks++;
        }

        if (isStationary()) {
            this.stillTicks++;
        } else {
            this.stillTicks = 0;
        }

        if (this.pauseOnEat.getValue() && isEating()) {
            cancelPendingAction();
            return;
        }
        if (this.pauseOnKillAura.getValue() && killAuraBusy()) {
            cancelPendingAction();
            return;
        }
        if (this.pauseOnScaffold.getValue() && scaffoldEnabled()) {
            cancelPendingAction();
            return;
        }
        if (chestStealerWorking()) {
            this.clickOffHand = false;
            cancelPendingAction();
            return;
        }

        Screen screen = mc.gui.screen();

        if (this.mode.is("Inventory")) {
            if (!(screen instanceof InventoryScreen)) {
                this.clickOffHand = false;
                cancelPendingAction();
                return;
            }
        }

        if (screen instanceof AbstractContainerScreen<?> container
                && container.getMenu().containerId != mc.player.inventoryMenu.containerId) {
            cancelPendingAction();
            return;
        }

        if (this.autoArmor.getValue()) {
            handleArmor();
        }

        if (this.clickOffHand && canExecuteInventoryAction()) {
            if (click(45)) {
                this.clickOffHand = false;
            }
        }

        handleOffhand();
        handleSwitches();
        handleThrow();
    }

    private void handleArmor() {
        EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

        for (int i = 0; i < armorSlots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(armorSlots[i]);
            if (!ItemUtils.isArmor(stack)) continue;

            EquipmentType type = armorType(armorSlots[i]);
            if (!stack.isEmpty() && canExecuteInventoryAction()
                    && InventoryUtils.getBestArmorScore(type) > InventoryUtils.getProtection(stack)) {
                dropAll(4 + (4 - i));
            }
        }

        int size = mc.player.getInventory().getNonEquipmentItems().size();
        for (int ix = 0; ix < size; ix++) {
            ItemStack stack = inv(ix);
            if (stack.isEmpty() || !ItemUtils.isArmor(stack)) continue;

            EquipmentSlot slot = armorSlot(stack);
            if (slot == null) continue;

            float score = InventoryUtils.getProtection(stack);
            EquipmentType type = armorType(slot);
            boolean isBest = InventoryUtils.getBestArmorScore(type) == score;
            boolean isBetter = InventoryUtils.getProtection(mc.player.getItemBySlot(slot)) < score;

            if (isBest && isBetter && canExecuteInventoryAction()) {
                shiftClick(containerSlot(ix));
            }
        }
    }

    private void handleOffhand() {
        if (this.offhandItem.is("Golden Apple")) {
            ItemStack offHand = mc.player.getOffhandItem();
            Item item = offHand.getItem();

            int egapSlot = findItemSlot(Items.ENCHANTED_GOLDEN_APPLE);
            int gapSlot = findItemSlot(Items.GOLDEN_APPLE);

            if (item == Items.ENCHANTED_GOLDEN_APPLE) {
                if (offHand.getCount() < offHand.getMaxStackSize() && egapSlot != -1 && canExecuteInventoryAction()) {
                    if (click(containerSlot(egapSlot))) {
                        this.clickOffHand = true;
                    }
                }
            } else if (item == Items.GOLDEN_APPLE) {
                if (egapSlot != -1) {
                    if (canExecuteInventoryAction()) swapOffHand(egapSlot);
                } else if (offHand.getCount() < offHand.getMaxStackSize() && gapSlot != -1
                        && canExecuteInventoryAction()) {
                    if (click(containerSlot(gapSlot))) {
                        this.clickOffHand = true;
                    }
                }
            } else if (egapSlot != -1) {
                if (canExecuteInventoryAction()) swapOffHand(egapSlot);
            } else if (gapSlot != -1) {
                if (canExecuteInventoryAction()) swapOffHand(gapSlot);
            }
            return;
        }

        if (this.offhandItem.is("Projectile")) {
            ItemStack offHand = mc.player.getOffhandItem();
            ItemStack best = InventoryUtils.getBestThrowable();
            if (best != null) {
                int slot = findStackSlot(best);
                boolean shouldSwap = (offHand.getItem() != Items.EGG && offHand.getItem() != Items.SNOWBALL)
                        || offHand.getCount() < best.getCount();
                if (shouldSwap && slot != -1 && canExecuteInventoryAction()) swapOffHand(slot);
            }
            return;
        }

        if (this.offhandItem.is("Fishing Rod")) {
            int slot = findItemSlot(Items.FISHING_ROD);
            if (slot != -1 && canExecuteInventoryAction()
                    && mc.player.getOffhandItem().getItem() != Items.FISHING_ROD) {
                swapOffHand(slot);
            }
            return;
        }

        if (this.offhandItem.is("Block")) {
            ItemStack offHand = mc.player.getOffhandItem();
            ItemStack best = InventoryUtils.getBestBlock();
            if (best != null) {
                int slot = findStackSlot(best);
                boolean shouldSwap = !ItemUtils.isValidBlock(offHand) || offHand.getCount() < best.getCount();
                if (shouldSwap && slot != -1 && canExecuteInventoryAction()) swapOffHand(slot);
            }
        }
    }

    private void handleSwitches() {
        if (this.switchGoldenApple.getValue() && !this.offhandItem.is("Golden Apple")) {
            int target = this.goldenAppleSlot.getValue().intValue() - 1;
            int egapSlot = findItemSlot(Items.ENCHANTED_GOLDEN_APPLE);
            int gapSlot = findItemSlot(Items.GOLDEN_APPLE);
            int bestSlot = egapSlot != -1 ? egapSlot : gapSlot;

            if (bestSlot != -1) {
                ItemStack current = inv(target);
                ItemStack best = inv(bestSlot);
                if (current.getItem() != best.getItem()
                        || (current.getItem() == best.getItem() && current.getCount() < best.getCount())) {
                    swapItemTo(target, best);
                }
            }
        }

        if (this.switchBlock.getValue()) {
            int target = this.blockSlot.getValue().intValue() - 1;
            ItemStack current = inv(target);
            ItemStack best = InventoryUtils.getBestBlock();

            if (best != null && (best.getCount() > current.getCount() || !ItemUtils.isValidBlock(current))
                    && !this.offhandItem.is("Block")) {
                swapItemTo(target, best);
            }
            if (InventoryUtils.getBlockCountInInventory() > this.maxBlockSize.getValue().intValue()) {
                throwItem(InventoryUtils.getWorstBlock());
            }
        }

        if (this.switchSword.getValue()) {
            int target = this.swordSlot.getValue().intValue() - 1;
            ItemStack current = inv(target);
            ItemStack best = bestSword();

            ItemStack specialAxe = bestSpecialAxe();
            if (specialAxe != null && score(specialAxe) > swordScore(best)) {
                best = specialAxe;
            }

            if (best != null && best != current) {
                if (!current.is(ItemTags.SWORDS) || score(best) > score(current)) {
                    swapItemTo(target, best);
                }
            }
        }

        if (this.switchPickaxe.getValue()) {
            int target = this.pickaxeSlot.getValue().intValue() - 1;
            ItemStack best = InventoryUtils.getBestPickaxe();
            ItemStack current = inv(target);

            if (best != null && best.is(ItemTags.PICKAXES)
                    && (InventoryUtils.getToolScore(best) > InventoryUtils.getToolScore(current)
                    || !current.is(ItemTags.PICKAXES))) {
                swapItemTo(target, best);
            }
        }

        if (this.switchAxe.getValue()) {
            int target = this.axeSlot.getValue().intValue() - 1;
            ItemStack reserved = inv(this.swordSlot.getValue().intValue() - 1);
            ItemStack best = bestAxeExcluding(reserved);
            ItemStack current = inv(target);

            if (best != null && best.is(ItemTags.AXES)
                    && (InventoryUtils.getToolScore(best) > InventoryUtils.getToolScore(current)
                    || !current.is(ItemTags.AXES))) {
                swapItemTo(target, best);
            }
        }

        if (this.switchRod.getValue() && !this.offhandItem.is("Fishing Rod")) {
            int target = this.rodSlot.getValue().intValue() - 1;
            ItemStack current = inv(target);
            if (!(current.getItem() instanceof FishingRodItem)) {
                swapItemTypeTo(target, Items.FISHING_ROD);
            }
        }

        if (this.switchBow.getValue()) {
            int target = this.bowSlot.getValue().intValue() - 1;
            ItemStack current = inv(target);
            ItemStack best;
            float bestScore;
            float currentScore;

            if (this.bowPriority.is("Power Bow")) {
                best = InventoryUtils.getBestPowerBow();
                bestScore = InventoryUtils.getPowerBowScore(best);
                currentScore = InventoryUtils.getPowerBowScore(current);
            } else if (this.bowPriority.is("Punch Bow")) {
                best = InventoryUtils.getBestPunchBow();
                bestScore = InventoryUtils.getPunchBowScore(best);
                currentScore = InventoryUtils.getPunchBowScore(current);
            } else {
                best = InventoryUtils.getBestCrossbow();
                bestScore = InventoryUtils.getCrossbowScore(best);
                currentScore = InventoryUtils.getCrossbowScore(current);
            }

            if (best == null) {
                best = InventoryUtils.getBestCrossbow();
                bestScore = InventoryUtils.getCrossbowScore(best);
                currentScore = InventoryUtils.getCrossbowScore(current);
            }
            if (best == null) {
                best = InventoryUtils.getBestPowerBow();
                bestScore = InventoryUtils.getPowerBowScore(best);
                currentScore = InventoryUtils.getPowerBowScore(current);
            }
            if (best == null) {
                best = InventoryUtils.getBestPunchBow();
                bestScore = InventoryUtils.getPunchBowScore(best);
                currentScore = InventoryUtils.getPunchBowScore(current);
            }

            boolean bowSlot = current.getItem() instanceof CrossbowItem || current.getItem() instanceof BowItem;
            if (best != null && best != current && (bestScore > currentScore || !bowSlot)) {
                swapItemTo(target, best);
            }
            if (InventoryUtils.getItemCount(Items.ARROW) > this.maxArrowSize.getValue().intValue()) {
                throwItem(worstArrow());
            }
        }

        if (this.switchEnderPearl.getValue()) {
            swapItemTypeTo(this.enderPearlSlot.getValue().intValue() - 1, Items.ENDER_PEARL);
        }
        if (this.switchWaterBucket.getValue()) {
            swapItemTypeTo(this.waterBucketSlot.getValue().intValue() - 1, Items.WATER_BUCKET);
        }
        if (this.switchFireball.getValue()) {
            swapItemTypeTo(this.fireballSlot.getValue().intValue() - 1, Items.FIRE_CHARGE);
        }

        if (this.keepProjectile.getValue()) {
            int projectiles = InventoryUtils.getItemCount(Items.EGG) + InventoryUtils.getItemCount(Items.SNOWBALL);
            if (projectiles > this.maxProjectileSize.getValue().intValue()) {
                throwItem(InventoryUtils.getWorstThrowable());
            }

            if (this.switchProjectile.getValue() && !this.offhandItem.is("Projectile")) {
                int target = this.projectileSlot.getValue().intValue() - 1;
                if (InventoryUtils.getItemCount(Items.EGG) > 0) {
                    swapItemTypeTo(target, Items.EGG);
                } else if (InventoryUtils.getItemCount(Items.SNOWBALL) > 0) {
                    swapItemTypeTo(target, Items.SNOWBALL);
                }
            }
        }
    }

    private void handleThrow() {
        if (!this.throwItems.getValue()) return;

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < mc.player.getInventory().getNonEquipmentItems().size(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, this.random);

        for (int slot : slots) {
            ItemStack stack = inv(slot);
            if (stack.isEmpty() || isItemUseful(stack)) continue;
            if (!canExecuteInventoryAction()) continue;

            if (throwItem(stack)) return;
        }
    }

    private boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ItemUtils.isGodItem(stack)) return true;
        if (stack.getHoverName().getString().contains("点击使用")) return true;

        if (ItemUtils.isArmor(stack)) {
            EquipmentSlot slot = armorSlot(stack);
            if (slot == null) return false;

            float protection = InventoryUtils.getProtection(stack);
            if (InventoryUtils.getProtection(mc.player.getItemBySlot(slot)) >= protection) return false;

            return !(protection < InventoryUtils.getBestArmorScore(armorType(slot)));
        }

        if (stack.is(ItemTags.SWORDS)) {
            ItemStack best = InventoryUtils.getBestSword();
            return best != null && best == stack;
        }
        if (stack.is(ItemTags.PICKAXES)) {
            ItemStack best = InventoryUtils.getBestPickaxe();
            return best != null && best == stack;
        }
        if (stack.is(ItemTags.AXES) && !ItemUtils.isSharpnessAxe(stack)) {
            ItemStack best = InventoryUtils.getBestAxe();
            return best != null && best == stack;
        }
        if (stack.is(ItemTags.SHOVELS)) {
            ItemStack best = InventoryUtils.getBestShovel();
            return best != null && best == stack;
        }
        if (stack.getItem() instanceof CrossbowItem) {
            ItemStack best = InventoryUtils.getBestCrossbow();
            return best != null && best == stack;
        }
        if (stack.getItem() instanceof BowItem && InventoryUtils.getPunchLevel(stack) > 0) {
            ItemStack best = InventoryUtils.getBestPunchBow();
            return best != null && best == stack;
        }
        if (stack.getItem() instanceof BowItem && InventoryUtils.getPowerLevel(stack) > 0) {
            ItemStack best = InventoryUtils.getBestPowerBow();
            return best != null && best == stack;
        }
        if (stack.getItem() instanceof BowItem && InventoryUtils.getItemCount(Items.BOW) > 1) return false;

        if (stack.getItem() == Items.WATER_BUCKET
                && InventoryUtils.getItemCount(Items.WATER_BUCKET) > this.waterBucketCount.getValue()) {
            return false;
        }
        if (stack.getItem() == Items.LAVA_BUCKET
                && InventoryUtils.getItemCount(Items.LAVA_BUCKET) > this.lavaBucketCount.getValue()) {
            return false;
        }
        if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) > 1) {
            return false;
        }
        if ((stack.getItem() == Items.SNOWBALL || stack.getItem() == Items.EGG)
                && !this.keepProjectile.getValue()) {
            return false;
        }
        if (stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) return true;

        return !(stack.getItem() instanceof StandingAndWallBlockItem) && ItemUtils.isCommonItemUseful(stack);
    }

    private void swapOffHand(int slot) {
        swap(containerSlot(slot), 40);
    }

    private boolean throwItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!ItemUtils.isValidItem(stack)) return false;
        if (!canExecuteInventoryAction()) return false;

        int slot = findStackSlot(stack);
        if (slot == -1) return false;

        return dropAll(containerSlot(slot));
    }

    private void swapItemTypeTo(int target, Item item) {
        ItemStack current = inv(target);
        if (!isReplaceable(current)) return;
        if (current.getItem() == item) return;

        ItemStack found = findStack(item);
        if (found == null) return;

        swapItemTo(target, found);
    }

    private static ItemStack bestSword() {
        ItemStack best = null;
        float bestScore = 0.0F;

        for (ItemStack stack : InventoryUtils.getAllItems()) {
            if (stack.isEmpty() || !stack.is(ItemTags.SWORDS) || !ItemUtils.isValidItem(stack)) continue;

            float score = swordScore(stack);
            if (score > bestScore) {
                bestScore = score;
                best = stack;
            }
        }

        return best;
    }

    private static float swordScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ItemTags.SWORDS)) return 0.0F;

        return InventoryUtils.getWeaponScore(stack);
    }

    private static float score(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0F;

        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                ? InventoryUtils.getWeaponScore(stack)
                : InventoryUtils.getAttackDamage(stack);
    }

    private static ItemStack bestSpecialAxe() {
        ItemStack best = null;
        float bestDamage = 0.0F;

        for (ItemStack stack : InventoryUtils.getAllItems()) {
            if (stack.isEmpty() || !stack.is(ItemTags.AXES)) continue;
            if (!ItemUtils.isSharpnessAxe(stack) && !InventoryUtils.isGodAxe(stack)) continue;
            if (!ItemUtils.isValidItem(stack)) continue;

            float damage = InventoryUtils.getWeaponScore(stack);
            if (damage > bestDamage) {
                bestDamage = damage;
                best = stack;
            }
        }

        return best;
    }

    private static ItemStack bestAxeExcluding(ItemStack exclude) {
        ItemStack best = null;
        float bestScore = 0.0F;

        for (ItemStack stack : InventoryUtils.getAllItems()) {
            if (stack.isEmpty() || stack == exclude || !stack.is(ItemTags.AXES)) continue;
            if (!ItemUtils.isValidItem(stack)) continue;

            float score = InventoryUtils.getWeaponScore(stack);
            if (score > bestScore) {
                bestScore = score;
                best = stack;
            }
        }

        return best;
    }

    private void swapItemTo(int target, ItemStack best) {
        ItemStack current = inv(target);
        if (!isReplaceable(current)) return;
        if (best == null || best == current) return;
        if (!canExecuteInventoryAction()) return;

        int bestSlot = findStackSlot(best);
        if (bestSlot == -1) return;

        swap(containerSlot(bestSlot), target);
    }

    private boolean prepareInventoryAction() {
        if (!(this.mode.is("Silent") && this.stopSprinting.getValue())) {
            if (this.actionState == ActionState.WAITING_FOR_SPRINT_STOP) {
                this.actionState = ActionState.IDLE;
            }
            return true;
        }
        if (this.actionState == ActionState.WAITING_FOR_SPRINT_STOP) return false;
        if (this.actionState == ActionState.READY_TO_EXECUTE) return true;

        if (mc.player.isSprinting() || this.wasSprinting) {
            this.actionState = ActionState.WAITING_FOR_SPRINT_STOP;
            mc.player.setSprinting(false);
            return false;
        }

        return true;
    }

    private void completeInventoryAction() {
        this.timer.reset();
        this.delayMs = randomDelay();
        this.inventoryOpen = true;
        this.actionConsumed = true;
        this.actionState = this.mode.is("Silent")
                ? ActionState.WAITING_FOR_INVENTORY_CLOSE
                : ActionState.IDLE;
    }

    private boolean canExecuteInventoryAction() {
        if (this.actionConsumed) return false;
        if (this.pauseOnMove.getValue() && this.stillTicks < 2) return false;

        return this.timer.hasTimeElapsed(this.delayMs);
    }

    /**
     * Grim's MultiActionsC flags a container click while the player is sprinting, sneaking or
     * holding a movement key, so every click waits for a tick where all three are false.
     */
    private static boolean isStationary() {
        if (mc.player == null) return false;
        if (mc.player.isShiftKeyDown() || mc.player.isSprinting()) return false;

        var keys = mc.player.input.keyPresses;
        return !keys.forward() && !keys.backward() && !keys.left() && !keys.right();
    }

    private void cancelPendingAction() {
        if (this.actionState != ActionState.IDLE && this.actionState != ActionState.WAITING_FOR_INVENTORY_CLOSE) {
            this.actionState = ActionState.IDLE;
        }
    }

    private boolean click(int slot) {
        if (!prepareInventoryAction()) return false;

        ClickSlotUtils.click(slot);
        completeInventoryAction();
        return true;
    }

    private boolean swap(int slot, int hotbarSlot) {
        if (!prepareInventoryAction()) return false;

        ClickSlotUtils.swap(slot, hotbarSlot);
        completeInventoryAction();
        return true;
    }

    private boolean shiftClick(int slot) {
        if (!prepareInventoryAction()) return false;

        ClickSlotUtils.shiftClick(slot);
        completeInventoryAction();
        return true;
    }

    private boolean dropAll(int slot) {
        if (!prepareInventoryAction()) return false;

        ClickSlotUtils.dropAll(slot);
        completeInventoryAction();
        return true;
    }

    private int randomDelay() {
        int min = Math.min(this.minDelay.getValue().intValue(), this.maxDelay.getValue().intValue());
        int max = Math.max(this.minDelay.getValue().intValue(), this.maxDelay.getValue().intValue());

        return min == max ? min : min + this.random.nextInt(max - min + 1);
    }

    private static ItemStack inv(int index) {
        return mc.player.getInventory().getItem(index);
    }

    private static int containerSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }

    private static int findItemSlot(Item item) {
        for (int i = 0; i < 36; i++) {
            if (inv(i).getItem() == item) return i;
        }
        return -1;
    }

    private static ItemStack findStack(Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv(i);
            if (stack.getItem() == item) return stack;
        }
        return null;
    }

    private static int findStackSlot(ItemStack stack) {
        if (stack == null) return -1;

        for (int i = 0; i < 36; i++) {
            if (inv(i) == stack) return i;
        }
        return -1;
    }

    private static ItemStack worstArrow() {
        ItemStack worst = null;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv(i);
            if (!stack.is(Items.ARROW)) continue;
            if (worst == null || stack.getCount() < worst.getCount()) worst = stack;
        }

        return worst;
    }

    private static boolean isReplaceable(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.getItem() instanceof PlayerHeadItem) return false;

        String name = stack.getHoverName().getString();
        return !name.contains("Click") && !name.contains("Right") && !name.contains("点击")
                && !name.contains("Teleport") && !name.contains("使用")
                && !name.contains("传送") && !name.contains("再来");
    }

    private static EquipmentSlot armorSlot(ItemStack stack) {
        EquipmentType type = ItemUtils.getEquipmentType(stack);
        if (type == null) return null;

        return switch (type) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
        };
    }

    private static EquipmentType armorType(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentType.HELMET;
            case CHEST -> EquipmentType.CHESTPLATE;
            case LEGS -> EquipmentType.LEGGINGS;
            default -> EquipmentType.BOOTS;
        };
    }

    private static boolean isMoving() {
        var move = mc.player.input.getMoveVector();
        return move.x != 0.0F || move.y != 0.0F;
    }

    private static boolean isEating() {
        return mc.player.isUsingItem() && ItemUtils.isConsumable(mc.player.getUseItem());
    }

    private static boolean killAuraBusy() {
        if (instance == null || instance.getModuleManager() == null) return false;

        KillAura aura = instance.getModuleManager().getModule(KillAura.class);
        return aura != null && aura.isEnabled() && aura.getTarget() != null;
    }

    private static boolean scaffoldEnabled() {
        if (instance == null || instance.getModuleManager() == null) return false;

        Scaffold scaffold = instance.getModuleManager().getModule(Scaffold.class);
        return scaffold != null && scaffold.isEnabled();
    }

    private static boolean chestStealerWorking() {
        if (instance == null || instance.getModuleManager() == null) return false;

        ChestStealer stealer = instance.getModuleManager().getModule(ChestStealer.class);
        return stealer != null && stealer.isEnabled() && stealer.isWorking();
    }

    public static int waterBucketKeep() {
        return module != null ? module.waterBucketCount.getValue().intValue() : 1;
    }

    public static int lavaBucketKeep() {
        return module != null ? module.lavaBucketCount.getValue().intValue() : 1;
    }

    public static int maxBlockSize() {
        return module != null ? module.maxBlockSize.getValue().intValue() : 256;
    }

    public static int maxArrowSize() {
        return module != null ? module.maxArrowSize.getValue().intValue() : 256;
    }

    public static boolean keepProjectiles() {
        return module != null && module.keepProjectile.getValue();
    }

    public static int maxProjectileSize() {
        return module != null ? module.maxProjectileSize.getValue().intValue() : 64;
    }
}
