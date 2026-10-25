package cn.leader.ui.clickgui;

import cn.leader.module.Category;
import cn.leader.util.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI Screen
 * @author DSJ
 */
public class ClickGUIScreen extends Screen implements IMinecraft {
    private final List<Panel> panels = new ArrayList<>();

    public ClickGUIScreen() {
        super(Text.literal("ClickGUI"));
        float startX = 20;
        for (Category category : Category.values()) {
            panels.add(new Panel(category, startX, 20));
            startX += 120;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Panel panel : panels) {
            panel.mouseClicked((int) mouseX, (int) mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Panel panel : panels) {
            panel.mouseReleased((int) mouseX, (int) mouseY, button);
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}