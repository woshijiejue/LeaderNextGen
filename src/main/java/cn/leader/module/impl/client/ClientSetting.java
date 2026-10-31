package cn.leader.module.impl.client;

import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.MultiBoolValue;

public class ClientSetting extends Module {

    private final MultiBoolValue target = new MultiBoolValue("Target",
            new BoolValue("Player", true),
            new BoolValue("Dead", false),
            new BoolValue("Villager", false),
            new BoolValue("Invisible", false),
            new BoolValue("Mob", false),
            new BoolValue("Animal", false)
    );

    public ClientSetting() {
        super("ClientSetting", Category.Client);
        setEnabled(true);
        setHidden(true);
    }

    @Override
    public void onDisable() {
        setEnabled(true);
    }

    public MultiBoolValue getTarget() {
        return this.target;
    }
}
