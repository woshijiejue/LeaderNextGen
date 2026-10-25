package cn.leader.config;

import cn.leader.config.impl.ModuleConfig;
import cn.leader.util.IMinecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ConfigManager implements IMinecraft {
    @Getter
    private final List<Config> configs = new ArrayList<>();

    public ConfigManager() {
        instance.getEventManager().register(this);

        addConfigs(
                new ModuleConfig()
        );

        configs.forEach(Config::load);
    }

    public Config getConfig(final String name) {
        return this.configs.stream()
                .filter(config -> config.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public void addConfigs(final Config... configsArray) {
        configs.addAll(Arrays.asList(configsArray));
    }

    public void saveAll() {
        configs.forEach(Config::save);
    }

    public void loadAll() {
        configs.forEach(Config::load);
    }
}