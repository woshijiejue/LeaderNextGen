package cn.leader.module.impl.visual;

import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.MultiBoolValue;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Compact session information: FPS, latency, position, speed and facing.
 */
public final class InfoHUD extends Drag {

    private final MultiBoolValue lines = new MultiBoolValue("Lines",
            new BoolValue("FPS", true),
            new BoolValue("Ping", true),
            new BoolValue("Coordinates", true),
            new BoolValue("Speed", true),
            new BoolValue("Facing", false)
    );

    private final BoolValue background = new BoolValue("Background", true);

    public InfoHUD() {
        super("InfoHUD");
        setDescription("FPS / ping / coordinates / speed");
        percentX = 0.02F;
        percentY = 0.05F;
        setEnabled(true);
    }

    @Override
    public void render(GuiGraphics context) {
        if (mc.player == null || mc.textRenderer == null) {
            this.width = 0;
            this.height = 0;
            return;
        }

        List<String> content = new ArrayList<>();

        if (lines.isEnabled("FPS")) {
            content.add("FPS " + mc.getCurrentFps());
        }

        if (lines.isEnabled("Ping")) {
            content.add("Ping " + latency() + "ms");
        }

        if (lines.isEnabled("Coordinates")) {
            content.add("XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ());
        }

        if (lines.isEnabled("Speed")) {
            double speed = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z) * 20.0D;
            content.add(String.format("Speed %.2f", speed));
        }

        if (lines.isEnabled("Facing")) {
            content.add("Facing " + facing());
        }

        if (content.isEmpty()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        float padding = 3.0F;
        float textHeight = FontUtil.getHeight();

        float maxWidth = 0.0F;
        for (String line : content) {
            maxWidth = Math.max(maxWidth, FontUtil.getStringWidth(line));
        }

        this.width = maxWidth + padding * 2.0F;
        this.height = textHeight * content.size() + padding * 2.0F;

        if (background.getValue()) {
            RenderUtil.drawRect(context, renderX, renderY, this.width, this.height, new Color(0, 0, 0, 120).getRGB());
        }

        float y = renderY + padding;
        for (String line : content) {
            FontUtil.drawStringWithShadow(context, line, renderX + padding, y, 0xFFE0E0E0);
            y += textHeight;
        }
    }

    private int latency() {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;

        PlayerInfo entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private String facing() {
        return switch (mc.player.getHorizontalFacing()) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "E";
            case WEST -> "W";
            default -> "-";
        };
    }
}
