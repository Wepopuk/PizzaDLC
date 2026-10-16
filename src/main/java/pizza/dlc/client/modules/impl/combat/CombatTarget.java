package pizza.dlc.client.modules.impl.combat;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.render.combat.CombatHudText;


import net.minecraft.world.entity.LivingEntity;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.combat.AttackAura;

/** The active aura's target for HUD, ESP and modules that follow the combat target. */
public final class CombatTarget {
    private CombatTarget() { }

    public static LivingEntity current() {
        var client = PizzaDLCClient.getInstance();
        if (client == null || client.getModuleManager() == null) return null;
        var aura = client.getModuleManager().get(AttackAura.class);
        return aura != null && aura.isEnabled() ? aura.getTarget() : null;
    }
}
