package pizza.dlc.api.rotation.combat;

/** Shared rotation plans and camera state for combat modules. */
public final class CombatRotations {
    private static final class Holder {
        static final CombatRotationPlans PLANS = new CombatRotationPlans();
        static final CombatRotationState STATE = new CombatRotationState();
    }
    private CombatRotations() { }
    public static CombatRotationPlans plans() { return Holder.PLANS; }
    public static CombatRotationState state() { return Holder.STATE; }
}
