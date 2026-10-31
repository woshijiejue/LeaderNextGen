package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.ChatGUIEvent;
import cn.leader.event.impl.RenderSkiaEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.ColorValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.ColorUtil;
import cn.leader.util.render.DragonTheme;
import cn.leader.util.render.SkiaRender;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class HUD extends Module {
    private final ModeValue color = new ModeValue("Theme", "Rainbow", "Rainbow", "Dynamic", "Fade", "Astolfo", "Custom");
    private final ColorValue mainColor = new ColorValue("Main Color", new Color(DragonTheme.WHITE, true),
            () -> color.is("Dynamic") || color.is("Fade") || color.is("Custom"));
    private final ColorValue secondColor = new ColorValue("Second Color", new Color(DragonTheme.GOLD, true), () -> color.is("Fade"));
    public final NumberValue radius = new NumberValue("Radius", 6, 0, 12, 0.5F);
    public final NumberValue backgroundAlpha = new NumberValue("Background Alpha", 0.55F, 0, 1, 0.05F);
    public final NumberValue rowSpacing = new NumberValue("Line Spacing", 2.5F, 0, 8, 0.5F);

    public HUD() {
        super("HUD", Category.Visual);
        setDescription("Display");
        setEnabled(true);
    }

    @EventTarget
    public void onRenderSkia(RenderSkiaEvent event) {
        if (event.getLayer() != RenderSkiaEvent.Layer.HUD) return;
        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return;

        boolean editing = mc.gui.screen() instanceof ChatScreen;

        for (Module module : instance.getModuleManager().getModuleMap().values()) {
            if (module instanceof Drag drag && drag.isEnabled()) {
                drag.updatePos();
                drag.render(event.getCanvas());

                if (editing && drag.width > 0 && drag.height > 0) {
                    float x = drag.isRightAnchored() ? drag.getRenderX() - drag.width : drag.getRenderX();
                    SkiaRender.roundRectOutline(event.getCanvas(), x - 2, drag.getRenderY() - 2, drag.width + 4, drag.height + 4,
                            radius.getValue() + 2, 1.0F, drag.dragging ? getColor(0) : 0x40FFFFFF);
                }
            }
        }
    }

    @EventTarget
    public void onChatGUI(ChatGUIEvent event) {
        if (mc.player ==  null || mc.level == null) return;

        for (Module module : instance.getModuleManager().getModuleMap().values()) {
            if (module instanceof Drag drag && drag.isEnabled()) {
                drag.onChatGUI(event.getMouseX(), event.getMouseY(), GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
            }
        }
    }

    public int getColor() {
        return getColor(0);
    }

    public int getColor(int counter) {
        return getColor(counter, 255);
    }

    public int getColor(int counter, int alpha) {
        return switch (color.getValue()) {
            case "Rainbow" -> ColorUtil.getRainbow(counter, alpha);
            case "Dynamic" -> ColorUtil.getDynamic(counter, alpha);
            case "Fade" -> ColorUtil.getFade(counter, alpha);
            case "Astolfo" -> ColorUtil.getAstolfo(counter, alpha);
            case "Custom" -> ColorUtil.getCustom(alpha);
            default -> ColorUtil.applyAlpha(DragonTheme.WHITE, alpha);
        };
    }

    public ColorValue getMainColor() {
        return this.mainColor;
    }

    public ColorValue getSecondColor() {
        return this.secondColor;
    }
}
