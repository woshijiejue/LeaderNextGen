package cn.leader.ui.clickgui;

import cn.leader.module.Category;
import cn.leader.module.impl.visual.PostProcessing;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.IMinecraft;
import cn.leader.util.animation.Easing;
import cn.leader.util.animation.EasingAnimation;
import cn.leader.util.render.DragonTheme;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.ClipMode;
import org.jetbrains.skija.Rect;

import java.util.ArrayList;
import java.util.List;

public class ClickGUIScreen extends Screen implements IMinecraft {
    private final List<Panel> panels = new ArrayList<>();
    private final EasingAnimation openAnimation = new EasingAnimation(Easing.EASE_OUT_QUART, 250);
    private float scrollOffset;
    private float contentHeight;
    private int gridColumns = 1;
    private float gridGap = 8.0F;
    private float gridLeft = 136.0F;

    public ClickGUIScreen() {
        super(Component.literal("ClickGUI"));
        for (Category category : Category.values()) {
            panels.add(new Panel(category, 136, 62));
        }
    }

    @Override
    protected void init() {
        openAnimation.setValue(0);
        openAnimation.run(1);
    }

    private float mouseX() {
        return (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
    }

    private float mouseY() {
        return (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    public void renderSkia(Canvas canvas) {
        openAnimation.run(1);
        float progress = (float) openAnimation.getValue();
        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();

        // Give the screen a calm, opaque surface instead of letting the game
        // world compete with the controls behind every panel.
        SkiaRender.verticalGradient(canvas, 0, 0, width, height, 0,
                SkiaRender.alpha(0xFF0B0B12, 0.94F * progress),
                SkiaRender.alpha(0xFF171421, 0.94F * progress));

        PostProcessing post = instance.getModuleManager().getModule(PostProcessing.class);
        if (post == null || (post.isEnabled() && post.screenDim.getValue())) {
            SkiaRender.rect(canvas, 0, 0, width, height, SkiaRender.alpha(0x48000000, progress));
        }

        float mouseX = mouseX();
        float mouseY = mouseY();
        float contentMouseY = mouseY - scrollOffset - (1 - progress) * 20;

        layoutPanels(width, height);
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(0, 0, width, height), ClipMode.INTERSECT, true);
        canvas.translate(0, scrollOffset + (1 - progress) * 20);
        Panel hoveredPanel = null;
        for (Panel panel : panels) {
            panel.render(canvas, mouseX, contentMouseY, progress);
            if (panel.isHovered(mouseX, contentMouseY)) hoveredPanel = panel;
        }
        canvas.restore();

        renderPortrait(canvas, width, height, progress);

        String description = hoveredPanel == null ? null : hoveredPanel.hoveredDescription(mouseX, contentMouseY);
        if (description != null && !description.isEmpty() && progress > 0.9F) {
            SkiaFont font = Fonts.regular(8);
            float w = font.getStringWidth(description) + 10;
            float x = Math.min(mouseX + 8, width - w - 2);
            float y = mouseY + 8;
            PanelRenderer.panel(canvas, x, y, w, 13, 4, 1.0F, 0);
            font.drawStringCenteredY(canvas, description, x + 5, y + 6.5F, 0xFFE4E4EA, false);
        }
    }

    private void layoutPanels(float width, float height) {
        // Keep the portrait in the lower-right corner clear of the panel grid.
        float portraitSpace = width >= 300.0F ? 154.0F : 0.0F;
        gridLeft = 14.0F;
        gridGap = 10.0F;
        float panelWidth = Panel.WIDTH;
        float availableWidth = Math.max(panelWidth, width - portraitSpace - gridLeft - 10.0F);
        gridColumns = Math.max(1, (int) ((availableWidth + gridGap) / (panelWidth + gridGap)));

        float[] columnHeights = new float[gridColumns];
        for (int i = 0; i < gridColumns; i++) columnHeights[i] = 0.0F;
        for (int index = 0; index < panels.size(); index++) {
            Panel panel = panels.get(index);
            int column = index % gridColumns;
            float panelY = 16.0F + columnHeights[column];
            float panelX = gridLeft + column * (panelWidth + gridGap);
            panel.setPosition(panelX, panelY);
            panel.setDraggable(false);
            columnHeights[column] += panel.measureFullHeight() + gridGap;
        }
        contentHeight = 0.0F;
        for (float heightInColumn : columnHeights) contentHeight = Math.max(contentHeight, heightInColumn - gridGap);
    }

    private void renderPortrait(Canvas canvas, float width, float height, float alpha) {
        float portraitWidth = 145;
        float portraitHeight = 226;
        float x = Math.max(8.0F, width - portraitWidth - 10.0F);
        float y = Math.max(8.0F, height - portraitHeight - 10.0F);
        // The portrait is intentionally standalone: no card, label or overlay.
        DragonTheme.drawPortrait(canvas, x, y, portraitWidth, portraitHeight, alpha);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float mouseX = (float) event.x();
        float mouseY = (float) event.y() - scrollOffset - (1.0F - (float) openAnimation.getValue()) * 20.0F;
        int button = event.button();
        boolean handled = false;
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked(mouseX, mouseY, button)) {
                handled = true;
                break;
            }
        }

        return handled || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        float mouseX = (float) event.x();
        float mouseY = (float) event.y() - scrollOffset - (1.0F - (float) openAnimation.getValue()) * 20.0F;
        int button = event.button();
        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float viewHeight = mc.getWindow().getGuiScaledHeight();
        float bottom = 0.0F;
        for (Panel panel : panels) bottom = Math.max(bottom, panel.getBottom());
        bottom = Math.max(bottom, 16.0F + contentHeight);
        float minScroll = Math.min(0, viewHeight - bottom - 12.0F);
        scrollOffset = Math.max(minScroll, Math.min(0, scrollOffset + (float) scrollY * 28.0F));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
