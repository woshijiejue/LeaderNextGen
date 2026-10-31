package cn.leader.ui.clickgui;

import cn.leader.module.Category;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.IMinecraft;
import cn.leader.util.animation.Easing;
import cn.leader.util.animation.EasingAnimation;
import cn.leader.util.misc.MouseUtil;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.DragonTheme;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

import java.util.ArrayList;
import java.util.List;

public class Panel implements IMinecraft {
    public static final float WIDTH = 126;
    public static final float HEADER = 25;

    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Category category;
    private final EasingAnimation openAnimation = new EasingAnimation(Easing.EASE_OUT_QUART, 250);
    private float x, y, dragX, dragY;
    private boolean dragging, open = true;
    private boolean draggable = true;
    private float lastBodyHeight;

    public Panel(Category category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.openAnimation.setValue(1);

        instance.getModuleManager().getModuleMap().values().stream()
                .filter(m -> m.getCategory() == category)
                .forEach(m -> buttons.add(new ModuleButton(m)));
    }

    public void render(Canvas canvas, float mouseX, float mouseY, float alpha) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }

        float contentHeight = 0;
        for (ModuleButton button : buttons) {
            contentHeight += button.measure();
        }

        float bodyTarget = contentHeight + 6;

        openAnimation.run(open ? 1 : 0);
        float body = bodyTarget * (float) openAnimation.getValue();
        lastBodyHeight = body;

        float radius = PanelRenderer.radius();
        int first = PanelRenderer.accent(0);
        int second = PanelRenderer.accent(6);

        PanelRenderer.panel(canvas, x, y, WIDTH, HEADER + body, radius, alpha, first);

        canvas.save();
        SkiaRender.clipRoundRect(canvas, x, y, WIDTH, HEADER, radius);
        SkiaRender.horizontalGradient(canvas, x, y, WIDTH, HEADER + radius, 0, SkiaRender.alpha(first, alpha), SkiaRender.alpha(second, alpha));
        canvas.restore();

        DragonTheme.drawCrest(canvas, x + 8, y + 5, 15, alpha);
        SkiaFont title = Fonts.bold(9.5F);
        title.drawStringCenteredY(canvas, category.getName(), x + 29, y + HEADER / 2, SkiaRender.alpha(0xFFFFFFFF, alpha), true);

        SkiaFont count = Fonts.number(7.5F);
        String countText = String.valueOf(buttons.size());
        float countWidth = count.getStringWidth(countText) + 10.0F;
        SkiaRender.roundRect(canvas, x + WIDTH - countWidth - 7, y + 6, countWidth, 13, 6.5F,
                SkiaRender.alpha(0xFFFFFFFF, 0.11F * alpha));
        count.drawStringCenteredY(canvas, countText, x + WIDTH - countWidth / 2 - 7, y + HEADER / 2,
                SkiaRender.alpha(0xD8FFFFFF, alpha), false);

        if (body > 1) {
            canvas.save();
            SkiaRender.clipRoundRect(canvas, x, y + HEADER, WIDTH, body, 0);
            float offsetY = y + HEADER + 3;
            boolean inside = MouseUtil.isHovered(mouseX, mouseY, x, y + HEADER, WIDTH, body);
            for (ModuleButton button : buttons) {
                offsetY += button.render(canvas, inside ? mouseX : -1, inside ? mouseY : -1, x, offsetY, WIDTH, alpha);
            }
            canvas.restore();
        }
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, x, y, WIDTH, HEADER + lastBodyHeight);
    }

    public float getBottom() {
        return y + HEADER + lastBodyHeight;
    }

    public float measureFullHeight() {
        float contentHeight = 0;
        for (ModuleButton button : buttons) contentHeight += button.measure();
        return HEADER + contentHeight + 6;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public String hoveredDescription(float mouseX, float mouseY) {
        if (!open || !MouseUtil.isHovered(mouseX, mouseY, x, y + HEADER, WIDTH, lastBodyHeight)) return null;
        float offsetY = y + HEADER + 2;
        for (ModuleButton button : buttons) {
            if (MouseUtil.isHovered(mouseX, mouseY, x, offsetY, WIDTH, ModuleButton.HEIGHT)) {
                return button.getModule().getDescription();
            }
            offsetY += button.getHeight();
        }
        return null;
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, x, y, WIDTH, HEADER)) {
            if (button == 0 && draggable) {
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
            } else if (button == 1) {
                open = !open;
            }
            return true;
        }

        if (open && MouseUtil.isHovered(mouseX, mouseY, x, y + HEADER, WIDTH, lastBodyHeight)) {
            float offsetY = y + HEADER + 2;
            for (ModuleButton btn : buttons) {
                btn.mouseClicked(mouseX, mouseY, button, x, offsetY, WIDTH);
                offsetY += btn.getHeight();
            }
            return true;
        }

        return false;
    }

    public void mouseReleased(float mouseX, float mouseY, int button) {
        dragging = false;
        buttons.forEach(btn -> btn.mouseReleased(mouseX, mouseY, button));
    }

}
