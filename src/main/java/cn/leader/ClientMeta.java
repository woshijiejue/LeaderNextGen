package cn.leader;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

public final class ClientMeta {

    public static final String NAME = "Leader Nextgen";

    public static final String VERSION = resolveVersion();

    public static final String MINECRAFT_VERSION = SharedConstants.getCurrentVersion().name();

    public static final String LOADER_VERSION = resolveLoaderVersion();

    private ClientMeta() {
    }

    public static String title() {
        return NAME + " " + VERSION + "  |  Minecraft " + MINECRAFT_VERSION;
    }

    private static String resolveVersion() {
        return FabricLoader.getInstance()
                .getModContainer("leadernextgen")
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
