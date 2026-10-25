package cn.leader.module;

import cn.leader.Client;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.KeyEvent;
import cn.leader.module.impl.client.ClientSetting;
import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.impl.combat.Velocity;
import cn.leader.module.impl.misc.Timer;
import cn.leader.module.impl.movement.FastWeb;
import cn.leader.module.impl.movement.NoSlow;
import cn.leader.module.impl.movement.Sprint;
import cn.leader.module.impl.movement.Strafe;
import cn.leader.module.impl.player.AntiAFK;
import cn.leader.module.impl.player.AntiBot;
import cn.leader.module.impl.player.AutoRespawn;
import cn.leader.module.impl.player.AutoTool;
import cn.leader.module.impl.player.Blink;
import cn.leader.module.impl.player.FastPlace;
import cn.leader.module.impl.player.NoJumpDelay;
import cn.leader.module.impl.visual.*;
import cn.leader.module.value.Value;
import cn.leader.util.IMinecraft;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.*;

@Getter
public class ModuleManager implements IMinecraft {
    private final Map<String, Module> moduleMap = new LinkedHashMap<>();

    public ModuleManager() {
        instance.getEventManager().register(this);

        addModules(
                 new Sprint(),
                 new Timer(),
                 new NoJumpDelay(),
                 new Velocity(),
                 new ClickGUI(),
                 new HUD(),
                 new NoHurtCam(),
                 new NoFog(),
                 new Brightness(),
                 new ModuleList(),
                 new WaterMark(),
                 new Strafe(),
                 new WorldTweaks(),
                 new KillAura(),
                 new NoSlow(),
                 new Animation(),
                 new ClientSetting(),
                 new ESP(),
                 new FastWeb(),
                 new TargetHUD(),
                 new InfoHUD(),
                 new PotionHUD(),
                 new AutoTool(),
                 new FastPlace(),
                 new AutoRespawn(),
                 new AntiAFK(),
                 new AntiBot(),
                 new Blink()
        );

        sortModules();
    }

    public void addModules(Module... modulesArray) {
        for (Module module : modulesArray) {
            reflectModuleValues(module);
            moduleMap.put(module.getClass().getSimpleName(), module);
        }
    }

    private void reflectModuleValues(Module module) {
        try {
            Class<?> clazz = module.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Value.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object valueObject = field.get(module);
                        if (valueObject != null) module.getValues().add((Value) valueObject);
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            Client.logger.debug(e.getMessage());
        }
    }

    private void sortModules() {
        List<Module> moduleList = new ArrayList<>(moduleMap.values());
        moduleList.sort(Comparator.comparing(Module::getName));
        moduleMap.clear();
        for (Module module : moduleList) moduleMap.put(module.getClass().getSimpleName(), module);
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        return clazz.cast(moduleMap.get(clazz.getSimpleName()));
    }

    @EventTarget
    private void onKey(KeyEvent event) {
        if (event.getKey() == 0 || mc.currentScreen != null) return;

        for (Module module : moduleMap.values()) {
            if (module.getKey() == event.getKey()) {
                module.toggle();
            }
        }
    }
}