package cn.leader.ui.clickgui;

import cn.leader.module.Category;
import cn.leader.util.IMinecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI Screen
 * @author DSJ
 */
public class ClickGUIScreen extends Screen implements IMinecraft {
    private final List<Panel> panels = new ArrayList<>();

    public ClickGUIScreen() {
        super(Component.literal("ClickGUI"));
        float startX = 20;
        for (Category category : Category.values()) {
            panels.add(new Panel(category, startX, 20));
            startX += 120;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (Panel panel : panels) {
            panel.mouseClicked((int) mouseX, (int) mouseY, button);
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (Panel panel : panels) {
            panel.mouseReleased((int) mouseX, (int) mouseY, button);
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
