package cn.leader.module.impl.player;

import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AntiBot extends Module {

    private final BoolValue notInTab = new BoolValue("Not In Tab", true);
    private final BoolValue duplicateName = new BoolValue("Duplicate Name", true);

    public AntiBot() {
        super("AntiBot", Category.Player);
        setDescription("Ignore NPCs when picking targets");
    }

    public boolean isBot(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        if (mc.player == null || mc.level == null) return false;
        if (player == mc.player) return false;

        if (notInTab.getValue()) {
            if (mc.getConnection() == null) return false;
            if (mc.getConnection().getPlayerInfo(player.getUUID()) == null) {
                return true;
            }
        }

        if (duplicateName.getValue()) {
            String name = player.getName().getString();
            if (name.isEmpty()) return true;

            for (Player other : mc.level.players()) {
                if (other == player) continue;
                if (other.getName().getString().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        return false;
    }
}
