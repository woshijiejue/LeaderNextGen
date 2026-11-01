package cn.leader.util.player;

import cn.leader.util.IMinecraft;
import net.minecraft.world.inventory.ContainerInput;

public final class ClickSlotUtils implements IMinecraft {

    public static void clickSlot(int containerId, int slot, int button, ContainerInput input) {
        if (mc.player == null || mc.gameMode == null) return;

        mc.gameMode.handleContainerInput(containerId, slot, button, input, mc.player);
    }

    public static void clickSlot(int slot, int button, ContainerInput input) {
        if (mc.player == null) return;

        clickSlot(mc.player.inventoryMenu.containerId, slot, button, input);
    }

    public static void click(int containerId, int slot) {
        clickSlot(containerId, slot, 0, ContainerInput.PICKUP);
    }

    public static void click(int slot) {
        clickSlot(slot, 0, ContainerInput.PICKUP);
    }

    public static void shiftClick(int containerId, int slot) {
        clickSlot(containerId, slot, 0, ContainerInput.QUICK_MOVE);
    }

    public static void shiftClick(int slot) {
        clickSlot(slot, 0, ContainerInput.QUICK_MOVE);
    }

    public static void drop(int slot) {
        clickSlot(slot, 0, ContainerInput.THROW);
    }

    public static void dropAll(int slot) {
        clickSlot(slot, 1, ContainerInput.THROW);
    }

    public static void swap(int containerId, int slot, int hotbarSlot) {
        clickSlot(containerId, slot, hotbarSlot, ContainerInput.SWAP);
    }

    public static void swap(int slot, int hotbarSlot) {
        clickSlot(slot, hotbarSlot, ContainerInput.SWAP);
    }

    private ClickSlotUtils() {
    }
}
