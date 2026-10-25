package cn.leader.ui.clickgui.components.impl;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ModeComponent extends ValueComponent {

    public ModeComponent(ModeValue value) {
        super(value);
    }

    @Override
    public float render(DrawContext context, int mouseX, int mouseY, float x, float y, float width) {
        RenderUtil.drawRect(context, x, y, width, 15, new Color(0, 0, 0, 120).getRGB());
        FontUtil.drawStringWithShadow(context, getValue().getName() + ": " + ((ModeValue)getValue()).getValue(), x + 6, y + 4, 0xFFAAAAAA);
        return lastHeight = 15;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button, float x, float y, float width) {
        if (button == 0 && hovered(mouseX, mouseY, x, y, width, 15)) {
            ModeValue mode = (ModeValue) getValue();
            List<String> modes = Arrays.asList(mode.getModes());
            mode.setValue(modes.get((modes.indexOf(mode.getValue()) + 1) % modes.size()));
        }
    }
}