package cn.leader.module.impl.visual;

import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;

public class CustomFont extends Module {
    public final ModeValue family = new ModeValue("Font", "Poppins", "Poppins", "MiSans", "JetBrains Mono");
    public final ModeValue numberFamily = new ModeValue("Number Font", "JetBrains Mono", "Poppins", "MiSans", "JetBrains Mono");
    public final BoolValue shadow = new BoolValue("Shadow", false);

    public CustomFont() {
        super("CustomFont", Category.Visual);
        setDescription("Client font renderer");
        setEnabled(true);
    }
}
