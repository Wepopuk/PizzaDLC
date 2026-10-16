package pizza.dlc.api.settings.impl;

import java.util.function.Supplier;
import pizza.dlc.api.settings.api.Parent;
import pizza.dlc.api.util.combat.CombatMath;

/** A native slider with a mode-dependent upper bound and deterministic step rounding. */
public final class CombatSliderSetting extends SliderSetting {
    private Supplier<Float> maximum;

    public CombatSliderSetting(String name, Parent parent, float value, float min, float max) {
        super(name, parent, value, min, max, 0);
    }

    public CombatSliderSetting withStep(float step) { setStep(step); return this; }
    public CombatSliderSetting withMax(Supplier<Float> supplier) {
        maximum = supplier;
        setCurrentValue(getValue());
        return this;
    }
    @Override public float getMax() { return maximum == null ? super.getMax() : maximum.get(); }
    @Override public void setCurrentValue(float value) {
        if (!Float.isFinite(value)) return;
        float rounded = getStep() > 0 ? (float)CombatMath.roundToStep(value, getStep()) : value;
        setValue(Math.clamp(rounded, getMin(), getMax()));
    }
}
