package cn.leader.module.impl.visual;

import cn.leader.module.Category;
import cn.leader.module.Module;

public class NoFog extends Module {
    public NoFog() {
        super("NoFog", Category.Visual);
        setDescription("Remove haze from the edge of the world");
    }
}
