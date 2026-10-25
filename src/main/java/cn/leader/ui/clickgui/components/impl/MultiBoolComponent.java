package cn.leader.ui.clickgui.components.impl;

import cn.leader.Client;
import cn.leader.module.impl.visual.HUD;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.MultiBoolValue;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.*;

public class MultiBoolComponent extends ValueComponent {
    private boolean expanded;

    public MultiBoolComponent(MultiBoolValue value) { super(value); }

    @Override
    public float render(GuiGraphicsExtractor context, int mouseX, int mouseY, float x, float y, float width) {
        float height = 15;
        MultiBoolValue multi = (MultiBoolValue) getValue();
        RenderUtil.drawRect(context, x, y, width, 15, new Color(0, 0, 0, 120).getRGB());
        FontUtil.drawStringWithShadow(context, multi.getName() + (expanded ? " -" : " +"), x + 6, y + 4, 0xFFAAAAAA);

        if (expanded) {
            for (BoolValue bool : multi.getValues()) {
                RenderUtil.drawRect(context, x, y + height, width, 15, new Color(0, 0, 0, 120).getRGB());
                FontUtil.drawStringWithShadow(context, bool.getName(), x + 14, y + height + 4, bool.getValue() ? Client.instance.getModuleManager().getModule(HUD.class).getColor() : 0xFFAAAAAA);
                height += 15;
            }
        }
        return lastHeight = height;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button, float x, float y, float width) {
        if (hovered(mouseX, mouseY, x, y, width, 15)) {
            if (button == 1) expanded = !expanded;
        } else if (expanded) {
            float offsetY = 15;
            for (BoolValue bool : ((MultiBoolValue) getValue()).getValues()) {
                if (button == 0 && hovered(mouseX, mouseY, x, y + offsetY, width, 15)) bool.toggle();
                offsetY += 15;
            }
        }
    }
}