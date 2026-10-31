package cn.leader.util.render;

import cn.leader.module.impl.visual.CustomFont;
import cn.leader.ui.font.FontManager;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.IMinecraft;

public final class Fonts implements IMinecraft {

    private static FontManager.Family family() {
        CustomFont module = instance.getModuleManager().getModule(CustomFont.class);
        if (module == null || !module.isEnabled()) return FontManager.Family.MISANS;
        return FontManager.Family.byName(module.family.getValue());
    }

    private static FontManager.Family numberFamily() {
        CustomFont module = instance.getModuleManager().getModule(CustomFont.class);
        if (module == null || !module.isEnabled()) return FontManager.Family.MISANS;
        return FontManager.Family.byName(module.numberFamily.getValue());
    }

    public static boolean shadow() {
        CustomFont module = instance.getModuleManager().getModule(CustomFont.class);
        return module != null && module.isEnabled() && module.shadow.getValue();
    }

    public static SkiaFont regular(float size) {
        return instance.getFontManager().get(family(), FontManager.Weight.REGULAR, size);
    }

    public static SkiaFont medium(float size) {
        return instance.getFontManager().get(family(), FontManager.Weight.MEDIUM, size);
    }

    public static SkiaFont semibold(float size) {
        return instance.getFontManager().get(family(), FontManager.Weight.SEMIBOLD, size);
    }

    public static SkiaFont bold(float size) {
        return instance.getFontManager().get(family(), FontManager.Weight.BOLD, size);
    }

    public static SkiaFont number(float size) {
        return instance.getFontManager().get(numberFamily(), FontManager.Weight.MEDIUM, size);
    }

    private Fonts() {
    }
}
