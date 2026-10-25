package cn.leader;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

/**
 * Static client identity, deliberately free of {@code IMinecraft}.
 *
 * <p>This is read by the window title mixin, which can run before {@code Client} exists. Keeping
 * it in a plain class means the title never forces {@code Client} (and therefore the
 * {@code IMinecraft.mc} static field) to initialise too early, which would capture a null
 * {@code MinecraftClient} forever.</p>
 */
public final class ClientMeta {

    public static final String NAME = "Leader Nextgen";

    /** Taken from the mod metadata so it can never drift from the built jar. */
    public static final String VERSION = resolveVersion();

    public static final String MINECRAFT_VERSION = SharedConstants.getGameVersion().getName();

    public static final String LOADER_VERSION = resolveLoaderVersion();

    private ClientMeta() {
    }

    public static String title() {
        return NAME + " " + VERSION + "  |  Minecraft " + MINECRAFT_VERSION;
    }

    private static String resolveVersion() {
        return FabricLoader.getInstance()
                .getModContainer("clientbase")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    private static String resolveLoaderVersion() {
        return FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
