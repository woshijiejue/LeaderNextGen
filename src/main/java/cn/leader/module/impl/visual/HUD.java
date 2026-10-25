package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.ChatGUIEvent;
import cn.leader.event.impl.Render2DEvent;
import cn.leader.event.impl.RenderSkiaEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.ColorValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.ColorUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@Getter
public class HUD extends Module {
    private final ModeValue color = new ModeValue("Color Setting", "Rainbow", "Rainbow", "Dynamic", "Fade", "Astolfo", "Custom");
    private final ColorValue mainColor = new ColorValue("Main Color", new Color(183, 0, 255));
    private final ColorValue secondColor = new ColorValue("Second Color", new Color(128, 255, 255), () -> color.is("Fade"));

    public HUD() {
        super("HUD", Category.Visual);
        setDescription("Display");
        setEnabled(true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.player ==  null || mc.level == null) return;

        for (Module module : instance.getModuleManager().getModuleMap().values()) {
            if (module instanceof Drag drag && drag.isEnabled()) {
                drag.render(event.getContext());
                drag.updatePos();
            }
        }
    }

    @EventTarget
    public void onRenderSkia(RenderSkiaEvent event) {
        if (mc.player ==  null || mc.level == null) return;
    }

    @EventTarget
    public void onChatGUI(ChatGUIEvent event) {
        if (mc.player ==  null || mc.level == null) return;

        for (Module module : instance.getModuleManager().getModuleMap().values()) {
            if (module instanceof Drag drag && drag.isEnabled()) {
                drag.onChatGUI(event.getMouseX(), event.getMouseY(), GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
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
            default -> ColorUtil.getCustom(alpha);
        };
    }
}
