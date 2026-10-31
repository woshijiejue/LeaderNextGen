package cn.leader.module.impl.visual;

import cn.leader.Client;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.DragonTheme;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WaterMark extends Drag {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final BoolValue showVersion = new BoolValue("Show Version", true);
    private final BoolValue showFps = new BoolValue("Show FPS", true);
    private final BoolValue showTime = new BoolValue("Show Time", true);

    public WaterMark() {
        super("WaterMark");
        setDescription("Client WaterMark");
        this.percentX = 0.01f;
        this.percentY = 0.01f;
        setEnabled(true);
    }

    @Override
    public void render(Canvas canvas) {
        SkiaFont title = Fonts.bold(10);
        SkiaFont info = Fonts.regular(8.5F);
        SkiaFont number = Fonts.number(8.5F);
        boolean shadow = Fonts.shadow();

        String name = Client.name;
        String version = showVersion.getValue() ? Client.version : "";
        String fps = showFps.getValue() ? mc.getFps() + " fps" : "";
        String time = showTime.getValue() ? LocalTime.now().format(TIME) : "";

        float height = 22.0F;
        float padding = 7.0F;
        float dot = 6.0F;
        float separator = 9.0F;

        float contentWidth = dot + 5.0F + title.getStringWidth(name);
        if (!version.isEmpty()) contentWidth += 4.0F + info.getStringWidth(version);
        if (!fps.isEmpty()) contentWidth += separator + number.getStringWidth(fps);
        if (!time.isEmpty()) contentWidth += separator + number.getStringWidth(time);

        this.width = contentWidth + padding * 2.0F;
        this.height = height;

        float radius = PanelRenderer.radius();
        int first = PanelRenderer.accent(0);
        int second = PanelRenderer.accent(6);

        PanelRenderer.panel(canvas, renderX, renderY, this.width, height, radius, 1.0F, first);
        canvas.save();
        SkiaRender.clipRoundRect(canvas, renderX, renderY, this.width, height, radius);
        SkiaRender.horizontalGradient(canvas, renderX, renderY, this.width, 1.5F, 0, first, second);
        canvas.restore();

        float centerY = renderY + (height - 1.0F) / 2.0F;
        float x = renderX + padding;

        SkiaRender.roundRect(canvas, x - 1, centerY - 5, 10, 10, 3, SkiaRender.alpha(first, 0.18F));
        DragonTheme.drawCrest(canvas, x - 1, centerY - 5, 10, 1.0F);
        x += dot + 5.0F;

        title.drawStringCenteredY(canvas, name, x, centerY, 0xFFFFFFFF, shadow);
        x += title.getStringWidth(name);

        if (!version.isEmpty()) {
            x += 4.0F;
            info.drawStringCenteredY(canvas, version, x, centerY, 0xFFA8A8B3, shadow);
            x += info.getStringWidth(version);
        }

        for (String part : new String[]{fps, time}) {
            if (part.isEmpty()) continue;
            SkiaRender.line(canvas, x + separator / 2.0F, centerY - 4, x + separator / 2.0F, centerY + 4,
                    0.5F, 0x35FFFFFF);
            x += separator;
            number.drawStringCenteredY(canvas, part, x, centerY, 0xFFE4E4EA, shadow);
            x += number.getStringWidth(part);
        }
    }
}
