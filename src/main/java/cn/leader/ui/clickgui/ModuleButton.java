package cn.leader.ui.clickgui;

import cn.leader.module.Module;
import cn.leader.module.value.Value;
import cn.leader.module.value.impl.*;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.ui.clickgui.components.impl.*;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.animation.Easing;
import cn.leader.util.animation.EasingAnimation;
import cn.leader.util.misc.MouseUtil;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    public static final float HEIGHT = 20;

    private final Module module;
    private final List<ValueComponent> components = new ArrayList<>();
    private final EasingAnimation expandAnimation = new EasingAnimation(Easing.EASE_OUT_QUART, 220);
    private final EasingAnimation toggleAnimation = new EasingAnimation(Easing.EASE_OUT_QUART, 200);
    private final EasingAnimation hoverAnimation = new EasingAnimation(Easing.EASE_OUT_QUART, 150);
    private boolean expanded;
    private float height = HEIGHT;

    public ModuleButton(Module module) {
        this.module = module;
        this.toggleAnimation.setValue(module.isEnabled() ? 1 : 0);
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

    private float componentsHeight() {
        float total = 0;
        for (ValueComponent comp : components) {
            if (comp.getValue().isVisible()) total += comp.getHeight();
        }
        return total;
    }

    public float measure() {
        expandAnimation.run(expanded ? 1 : 0);
        height = HEIGHT + (components.isEmpty() ? 0 : componentsHeight() * (float) expandAnimation.getValue());
        return height;
    }

    public float render(Canvas canvas, float mouseX, float mouseY, float x, float y, float width, float alpha) {
        boolean hovered = MouseUtil.isHovered(mouseX, mouseY, x, y, width, HEIGHT);
        hoverAnimation.run(hovered ? 1 : 0);
        toggleAnimation.run(module.isEnabled() ? 1 : 0);
        float hover = (float) hoverAnimation.getValue();
        float toggle = (float) toggleAnimation.getValue();
        int accent = PanelRenderer.accent(0);

        SkiaRender.roundRect(canvas, x + 4, y + 1, width - 8, HEIGHT - 2, 5,
                SkiaRender.alpha(0xFFFFFFFF, (0.04F * hover + 0.055F * toggle) * alpha));
        if (toggle > 0.01F) {
            SkiaRender.roundRect(canvas, x + 4, y + 5, 2, (HEIGHT - 10) * toggle, 1, SkiaRender.alpha(accent, alpha));
        }

        SkiaFont font = Fonts.medium(8.5F);
        int textColor = module.isEnabled() ? 0xFFFFFFFF : 0xFF9A9AA6;
        font.drawStringCenteredY(canvas, module.getName(), x + 11 + 2 * toggle, y + HEIGHT / 2, SkiaRender.alpha(textColor, alpha), false);

        if (!components.isEmpty()) {
            float cx = x + width - 12;
            float cy = y + HEIGHT / 2;
            float expand = (float) expandAnimation.getValue();
            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate(90 * expand);
            SkiaRender.line(canvas, -1.5F, -3, 1.5F, 0, 1.2F, SkiaRender.alpha(0xFFA8A8B3, alpha));
            SkiaRender.line(canvas, 1.5F, 0, -1.5F, 3, 1.2F, SkiaRender.alpha(0xFFA8A8B3, alpha));
            canvas.restore();
        }

        if (height > HEIGHT + 0.5F) {
            canvas.save();
            SkiaRender.clipRoundRect(canvas, x, y + HEIGHT, width, height - HEIGHT, 0);
            float offsetY = y + HEIGHT;
            for (ValueComponent comp : components) {
                if (!comp.getValue().isVisible()) continue;
                offsetY += comp.render(canvas, mouseX, mouseY, x + 4, offsetY, width - 8, alpha);
            }
            canvas.restore();
        }

        return height;
    }

    public void mouseClicked(float mouseX, float mouseY, int button, float x, float y, float width) {
        if (MouseUtil.isHovered(mouseX, mouseY, x, y, width, HEIGHT)) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1 && !components.isEmpty()) {
                expanded = !expanded;
            }
            return;
        }

        if (expanded) {
            float offsetY = y + HEIGHT;
            for (ValueComponent comp : components) {
                if (!comp.getValue().isVisible()) continue;
                comp.mouseClicked(mouseX, mouseY, button, x + 4, offsetY, width - 8);
                offsetY += comp.getHeight();
            }
        }
    }

    public void mouseReleased(float mouseX, float mouseY, int button) {
        components.forEach(c -> c.mouseReleased(mouseX, mouseY, button));
    }

    public Module getModule() {
        return this.module;
    }

    public float getHeight() {
        return this.height;
    }
}
