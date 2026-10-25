package cn.leader.ui.clickgui;

import cn.leader.Client;
import cn.leader.module.Module;
import cn.leader.module.impl.visual.HUD;
import cn.leader.module.value.Value;
import cn.leader.module.value.impl.*;
import cn.leader.ui.clickgui.components.*;
import cn.leader.ui.clickgui.components.impl.*;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    private final Module module;
    private final List<ValueComponent> components = new ArrayList<>();
    private boolean expanded;
    @Getter
    private float height = 15;

    public ModuleButton(Module module) {
        this.module = module;
        for (Value value : module.getValues()) {
            if (value instanceof BoolValue bool) {
                components.add(new BoolComponent(bool));
            } else if (value instanceof ModeValue mode) {
                components.add(new ModeComponent(mode));
            } else if (value instanceof NumberValue num) {
                components.add(new NumberComponent(num));
            } else if (value instanceof ColorValue color) {
                components.add(new ColorComponent(color));
            } else if (value instanceof MultiBoolValue multi) {
                components.add(new MultiBoolComponent(multi));
            }
        }
    }

    public float render(GuiGraphics context, int mouseX, int mouseY, float x, float y, float width) {
        float offsetY = 15;
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 15;
        RenderUtil.drawRect(context, x, y, width, 15,  new Color(0, 0, 0, hovered ? 100 : 80).getRGB());
        FontUtil.drawString(context, module.getName(), x + 4, y + 4, module.isEnabled() ? Client.instance.getModuleManager().getModule(HUD.class).getColor(1) : 0xFFAAAAAA, true);

        if (expanded) {
            for (ValueComponent comp : components) {
                if (!comp.getValue().isVisible()) continue;
                offsetY += comp.render(context, mouseX, mouseY, x, y + offsetY, width);
            }
        }

        height = offsetY;
        return height;
    }

    public void mouseClicked(int mouseX, int mouseY, int button, float x, float y, float width) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 15) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                expanded = !expanded;
            }
        }

        if (expanded) {
            float offsetY = 15;
            for (ValueComponent comp : components) {
                if (!comp.getValue().isVisible()) continue;
                comp.mouseClicked(mouseX, mouseY, button, x, y + offsetY, width);
                offsetY += comp.getLastHeight();
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (expanded) {
            components.forEach(c -> c.mouseReleased(mouseX, mouseY, button));
        }
    }
}