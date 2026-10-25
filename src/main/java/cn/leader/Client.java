package cn.leader;

import cn.leader.command.CommandManager;
import cn.leader.config.ConfigManager;
import cn.leader.event.base.EventManager;
import cn.leader.manager.RotationManager;
import cn.leader.manager.TargetManager;
import cn.leader.module.ModuleManager;
import cn.leader.ui.clickgui.ClickGUIScreen;
import cn.leader.ui.font.FontManager;
import cn.leader.ui.skia.SkiaManager;
import cn.leader.util.IMinecraft;
import cn.leader.util.Util;
import net.minecraft.ChatFormatting;
import org.apache.logging.log4j.Logger;

/**
 * Client Base.
 * @author DSJ
 */
public class Client implements IMinecraft {
    public static Client instance;
    public static Logger logger;

    public static String name = ClientMeta.NAME;
    public static String version = ClientMeta.VERSION;
    public static String minecraftVersion = ClientMeta.MINECRAFT_VERSION;
    public static net.minecraft.client.gui.screens.Screen currentScreen;
    public static String loaderVersion = ClientMeta.LOADER_VERSION;

    private EventManager eventManager;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private TargetManager targetManager;
    private RotationManager rotationManager;
    private ClickGUIScreen clickGUIScreen;
    private SkiaManager skiaManager;
    private FontManager fontManager;

    private boolean announced;

    public void init() {
        eventManager =  new EventManager();
        moduleManager  = new ModuleManager();
        commandManager = new CommandManager();
        configManager = new ConfigManager();
        targetManager = new TargetManager();
        rotationManager  = new RotationManager();

        clickGUIScreen  = new ClickGUIScreen();
        skiaManager = new SkiaManager();
        fontManager  = new FontManager();

        logger.info("{} {} initialized (Minecraft {}, Fabric Loader {})", name, version, minecraftVersion, loaderVersion);
    }

    /** One chat line on the first tick that has a player, i.e. once per game session. */
    public void announce() {
        if (announced || mc.player == null) return;
        announced = true;

        Util.log(name + " " + ChatFormatting.GRAY + version + ChatFormatting.DARK_GRAY + "  |  "
                + ChatFormatting.GRAY + "MC " + minecraftVersion);
    }

    public void shutdown() {
        configManager.saveAll();
        skiaManager.destroy();
        fontManager.destroy();
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }

    public ModuleManager getModuleManager() {
        return this.moduleManager;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public TargetManager getTargetManager() {
        return this.targetManager;
    }

    public RotationManager getRotationManager() {
        return this.rotationManager;
    }

    public ClickGUIScreen getClickGUIScreen() {
        return this.clickGUIScreen;
    }

    public SkiaManager getSkiaManager() {
        return this.skiaManager;
    }

    public FontManager getFontManager() {
        return this.fontManager;
    }

    public boolean isAnnounced() {
        return this.announced;
    }

}
