package cn.leader.module.impl.visual;

import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.MultiBoolValue;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.skija.Canvas;

import java.util.ArrayList;
import java.util.List;

public final class InfoHUD extends Drag {

    private final MultiBoolValue lines = new MultiBoolValue("Lines",
            new BoolValue("FPS", true),
            new BoolValue("Ping", true),
            new BoolValue("Coordinates", true),
            new BoolValue("Speed", true),
            new BoolValue("Facing", false)
    );

    private final BoolValue background = new BoolValue("Background", true);
    private final BoolValue header = new BoolValue("Header", true);

    public InfoHUD() {
        super("InfoHUD");
        setDescription("FPS / ping / coordinates / speed");
        percentX = 0.01F;
        percentY = 0.08F;
        setEnabled(true);
    }

    @Override
    public void render(Canvas canvas) {
        if (mc.player == null) {
            this.width = 0;
            this.height = 0;
            return;
        }

        List<String[]> rows = new ArrayList<>();

        if (lines.isEnabled("FPS")) rows.add(new String[]{"FPS", String.valueOf(mc.getFps())});
        if (lines.isEnabled("Ping")) rows.add(new String[]{"Ping", latency() + " ms"});
        if (lines.isEnabled("Coordinates")) {
            rows.add(new String[]{"XYZ", mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ()});
        }
        if (lines.isEnabled("Speed")) {
            double speed = Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z) * 20.0D;
            rows.add(new String[]{"Speed", String.format(java.util.Locale.ROOT, "%.2f b/s", speed)});
        }
        if (lines.isEnabled("Facing")) rows.add(new String[]{"Facing", facing()});

        if (rows.isEmpty()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        SkiaFont label = Fonts.regular(8.5F);
        SkiaFont value = Fonts.number(8.5F);
        boolean shadow = Fonts.shadow();

        float padding = 7.0F;
        float rowHeight = 15.0F;
        float headerHeight = header.getValue() ? 17 : 0;

        float labelWidth = 0;
        float valueWidth = 0;
        for (String[] row : rows) {
            labelWidth = Math.max(labelWidth, label.getStringWidth(row[0]));
            valueWidth = Math.max(valueWidth, value.getStringWidth(row[1]));
        }

        this.width = Math.max(90, padding * 2 + 6 + labelWidth + 12 + valueWidth);
        this.height = padding * 2 + headerHeight + rowHeight * rows.size();

        if (background.getValue()) {
            PanelRenderer.panel(canvas, renderX, renderY, this.width, this.height);
        }

        if (header.getValue()) {
            float headerTop = renderY + padding;
            float headerCenter = headerTop + headerHeight / 2.0F;
            Fonts.semibold(8).drawStringCenteredY(canvas, "SESSION", renderX + padding, headerCenter,
                    PanelRenderer.accent(0), shadow);
            SkiaRender.line(canvas, renderX + padding, headerTop + headerHeight - 1.0F, renderX + this.width - padding,
                    headerTop + headerHeight - 1.0F, 0.5F, 0x24FFFFFF);
        }
        float y = renderY + padding + headerHeight;
        int index = 0;
        for (String[] row : rows) {
            float centerY = y + rowHeight / 2.0F;
            int accent = PanelRenderer.accent(index * 3);
            if (index % 2 == 0) SkiaRender.roundRect(canvas, renderX + 3, y, this.width - 6, rowHeight, 3, 0x0CFFFFFF);
            SkiaRender.roundRect(canvas, renderX + padding, centerY - 3.0F, 2.0F, 6.0F, 1.0F, accent);
            label.drawStringCenteredY(canvas, row[0], renderX + padding + 6, centerY, 0xFFA8A8B3, shadow);
            value.drawStringCenteredY(canvas, row[1], renderX + this.width - padding - value.getStringWidth(row[1]), centerY, 0xFFF2F2F5, shadow);
            y += rowHeight;
            index++;
        }
    }

    private int latency() {
        if (mc.getConnection() == null || mc.player == null) return 0;

        PlayerInfo entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return entry == null ? 0 : entry.getLatency();
    }

    private String facing() {
        return switch (mc.player.getDirection()) {
            case NORTH -> "North";
            case SOUTH -> "South";
            case EAST -> "East";
            case WEST -> "West";
            default -> "-";
        };
    }
}
