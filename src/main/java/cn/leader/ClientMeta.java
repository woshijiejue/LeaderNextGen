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
        FabricLoader loader = FabricLoader.getInstance();

        for (String id : new String[]{"leadernextgen", "clientbase"}) {
            var container = loader.getModContainer(id);
            if (container.isPresent()) {
                return container.get().getMetadata().getVersion().getFriendlyString();
            }
        }

        return "dev";
    }

    private static String resolveLoaderVersion() {
        return FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
