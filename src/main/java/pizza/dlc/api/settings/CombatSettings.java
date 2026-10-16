package pizza.dlc.api.settings;

import java.util.Arrays;
import java.util.function.Supplier;
import pizza.dlc.client.modules.Module;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;

/** Helpers for explicit defaults on PizzaDLC's native settings. */
public final class CombatSettings {
    private CombatSettings() { }

    public static BooleanSetting bool(String name, Module parent, boolean enabled) {
        var setting = new BooleanSetting(name, parent);
        setting.setEnabled(enabled);
        return setting;
    }

    public static ModeSetting mode(ModeSetting setting, int index) {
        if (!setting.getModes().isEmpty()) setting.setCurrentMode(setting.getModes().get(Math.clamp(index, 0, setting.getModes().size()-1)));
        return setting;
    }

    public static MultiModeSetting multi(String name, Module parent, Option... options) {
        var setting = new MultiModeSetting(name, parent, Arrays.stream(options).map(Option::name).toArray(String[]::new));
        for (var option : options) {
            var child = setting.get(option.name());
            child.setEnabled(option.enabled());
            child.setVisible(option.visibility());
        }
        return setting;
    }

    public record Option(String name, boolean enabled, Supplier<Boolean> visibility) {
        public Option(String name) { this(name, false); }
        public Option(String name, boolean enabled) { this(name, enabled, () -> true); }
        public Option visible(Supplier<Boolean> condition) { return new Option(name, enabled, condition); }
    }
}
