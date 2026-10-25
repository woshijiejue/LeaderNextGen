package cn.leader.module.impl.player;

import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Filters server side NPCs out of targeting.
 *
 * <p>Two cheap and reliable signals: an entity that renders like a player but is missing from the
 * tab list is not a real player, and two entities sharing a name cannot both be real.</p>
 */
public final class AntiBot extends Module {

    private final BoolValue notInTab = new BoolValue("Not In Tab", true);
    private final BoolValue duplicateName = new BoolValue("Duplicate Name", true);

    public AntiBot() {
        super("AntiBot", Category.Player);
        setDescription("Ignore NPCs when picking targets");
    }

    public boolean isBot(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        if (mc.player == null || mc.world == null) return false;
        if (player == mc.player) return false;

        if (notInTab.getValue()) {
            if (mc.getNetworkHandler() == null) return false;
            if (mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null) {
                return true;
            }
        }

        if (duplicateName.getValue()) {
            String name = player.getName().getString();
            if (name.isEmpty()) return true;

            for (Player other : mc.world.getPlayers()) {
                if (other == player) continue;
                if (other.getName().getString().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        return false;
    }
}
