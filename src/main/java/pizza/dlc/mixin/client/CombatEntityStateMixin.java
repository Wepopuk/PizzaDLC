package pizza.dlc.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.interfaces.ResolvedRotationAccess;
import pizza.dlc.api.interfaces.WebStateAccess;

@Mixin(value = Entity.class, remap = false)
public class CombatEntityStateMixin implements ResolvedRotationAccess, WebStateAccess {
    @Unique private Float pizzadlc$resolvedYaw;
    @Unique private Float pizzadlc$resolvedPitch;
    @Unique private Vec3 pizzadlc$resolvedPos;
    @Unique private boolean pizzadlc$inWeb;

    @Override public float getResolvedYaw() { return pizzadlc$resolvedYaw == null ? ((Entity)(Object)this).getYRot() : pizzadlc$resolvedYaw; }
    @Override public float getResolvedPitch() { return pizzadlc$resolvedPitch == null ? ((Entity)(Object)this).getXRot() : pizzadlc$resolvedPitch; }
    @Override public Vec3 getResolvedPos() { return pizzadlc$resolvedPos == null ? ((Entity)(Object)this).position() : pizzadlc$resolvedPos; }
    @Override public void setResolvedYaw(float yaw) { pizzadlc$resolvedYaw = yaw; }
    @Override public void setResolvedPitch(float pitch) { pizzadlc$resolvedPitch = pitch; }
    @Override public void setResolvedPos(Vec3 position) { pizzadlc$resolvedPos = position; }
    @Override public Vec3 getResolvedForward() { return Vec3.directionFromRotation(getResolvedPitch(), getResolvedYaw()); }
    @Override public boolean isInWeb() { return pizzadlc$inWeb; }
    @Override public void setInWeb(boolean inWeb) { pizzadlc$inWeb = inWeb; }

    @Inject(remap = false, method = "move", at = @At(remap = false, value = "HEAD"))
    private void pizzadlc$resetWeb(MoverType type, Vec3 movement, CallbackInfo ci) { pizzadlc$inWeb = false; }
}
