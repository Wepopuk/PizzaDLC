package pizza.dlc.api.rotation.combat.impl;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
/** Compatibility plan identity; NeuroSystem owns actual visible camera updates. No policy.bin or formula fallback. */
public class NeuroAim extends CombatRotationPlan {
 public NeuroAim(Vec3 point){super(point,UpdateRate.SLOW);setPlayerCamera(true);setMovementCorrection(false);}
 @Override public Vector2f rotate(Vector2f current,Vector2f desired){return mc.player==null?new Vector2f(current):new Vector2f(mc.player.getYRot(),mc.player.getXRot());}
}
