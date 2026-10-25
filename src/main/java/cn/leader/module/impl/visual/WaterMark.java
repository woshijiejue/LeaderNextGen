package cn.leader.module.impl.visual;

import cn.leader.Client;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.FontUtil;
import net.minecraft.client.gui.DrawContext;

public class WaterMark extends Drag {

    private static final int VERSION_COLOR = 0xFF9E9E9E;

    private final BoolValue showVersion = new BoolValue("Show Version", true);

    public WaterMark() {
        super("WaterMark");
        setDescription("Client WaterMark");
        this.percentX = 0.1f;
        this.percentY = 0.1f;
    }

    @Override
    public void render(DrawContext context) {
        if (mc.player == null) return;

        HUD hud = getModule(HUD.class);

        String name = Client.name;
        String version = showVersion.getValue() ? " " + Client.version : "";

        this.width = FontUtil.getStringWidth(name + version) + 8;
        this.height = FontUtil.getHeight();

        FontUtil.drawStringWithShadow(context, name, renderX + 4, renderY + 4, hud.getColor(1));

        if (!version.isEmpty()) {
            FontUtil.drawStringWithShadow(context, version,
                    renderX + 4 + FontUtil.getStringWidth(name), renderY + 4, VERSION_COLOR);
        }
    }
}
