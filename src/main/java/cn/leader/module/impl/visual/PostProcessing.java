package cn.leader.module.impl.visual;

import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;

public class PostProcessing extends Module {
    public final BoolValue blur = new BoolValue("Blur", true);
    public final NumberValue blurRadius = new NumberValue("Blur Radius", 12, 2, 40, 1, blur::getValue);
    public final ModeValue blurQuality = new ModeValue("Blur Quality", "Medium", blur::getValue, "Low", "Medium", "High");
    public final BoolValue shadow = new BoolValue("Shadow", true);
    public final NumberValue shadowRadius = new NumberValue("Shadow Radius", 10, 2, 30, 1, shadow::getValue);
    public final NumberValue shadowAlpha = new NumberValue("Shadow Alpha", 0.5F, 0.05F, 1, 0.05F, shadow::getValue);
    public final BoolValue glow = new BoolValue("Glow", false);
    public final NumberValue glowRadius = new NumberValue("Glow Radius", 12, 2, 30, 1, glow::getValue);
    public final NumberValue glowAlpha = new NumberValue("Glow Alpha", 0.45F, 0.05F, 1, 0.05F, glow::getValue);
    public final BoolValue screenDim = new BoolValue("ClickGUI Dim", true);

    public PostProcessing() {
        super("PostProcessing", Category.Visual);
        setDescription("Blur, shadow and glow for the client UI");
        setEnabled(true);
    }

    public int downsample() {
        return switch (blurQuality.getValue()) {
            case "Low" -> 6;
            case "High" -> 2;
            default -> 4;
        };
    }

    public boolean blurActive() {
        return isEnabled() && blur.getValue();
    }

    public boolean shadowActive() {
        return isEnabled() && shadow.getValue();
    }

    public boolean glowActive() {
        return isEnabled() && glow.getValue();
    }
}
