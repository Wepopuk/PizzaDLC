package pizza.dlc.mixin.client;
import pizza.dlc.client.modules.impl.combat.AttackAura;
import pizza.dlc.client.modules.impl.combat.ElytraTarget;
import pizza.dlc.api.events.impl.combat.CombatWorldRenderEvent;
import pizza.dlc.api.util.render.combat.CombatDrawQueue;
import pizza.dlc.api.util.render.combat.CombatProjection;
import pizza.dlc.api.util.render.combat.CombatTargetEffects;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.combat.CombatRuntime;
/** World effects run during drawing, after LevelRenderer, never during COLLECT_SUBMITS extraction. */
@Mixin(value = GameRenderer.class, remap = false)
public class CombatWorldEffectsMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow @Final private SubmitNodeStorage handAndScreenSubmitNodeStorage;
    @Unique private final Matrix4f pizzadlc$projection = new Matrix4f();
    @Unique private boolean pizzadlc$effectsStarted;

    @Inject(remap = false, method = "renderLevel", at = @At(remap = false, value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"))
    private void pizzadlc$captureProjection(DeltaTracker tracker, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection) {
        if (!CombatRuntime.gameplay()) return;
        pizzadlc$projection.set(projection);
        CombatProjection.m_2695(new Matrix4f(minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.viewRotationMatrix), projection);
    }

    @Inject(remap = false, method = "renderLevel", at = @At(remap = false, value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", shift = At.Shift.AFTER))
    private void pizzadlc$drawWorld(DeltaTracker tracker, CallbackInfo ci) {
        if (!CombatRuntime.gameplay()) return;
        var modules = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager();
        if (!modules.get(AttackAura.class).isEnabled() && !modules.get(ElytraTarget.class).isEnabled()
                && (!pizzadlc$effectsStarted || !CombatTargetEffects.hasFadingTarget())) return;
        pizzadlc$effectsStarted = true;
        Matrix4f view = new Matrix4f(minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.viewRotationMatrix);
        PoseStack pose = new PoseStack();
        pose.mulPose(view);
        var buffer = CombatDrawQueue.m_885();
        CombatTargetEffects.beginFrame();
        try {
            var event = new CombatWorldRenderEvent(tracker, pose, buffer, minecraft.gameRenderer.mainCamera().position(),
                    tracker.getGameTimeDeltaPartialTick(false), new Matrix4f(pizzadlc$projection), view);
            CombatRuntime.post(event);
            if (!modules.get(AttackAura.class).isEnabled()) CombatTargetEffects.renderFade(event);
            buffer.m_884();
        } finally {
            buffer.m_883();
        }
    }

    @Inject(remap = false, method = "renderLevel", at = @At(remap = false, value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"))
    private void pizzadlc$compositeBeforeHands(DeltaTracker tracker, CallbackInfo ci) {
        if (!CombatRuntime.gameplay() || !pizzadlc$effectsStarted || !CombatTargetEffects.m_7940()) return;
        featureRenderDispatcher.renderAllFeatures(handAndScreenSubmitNodeStorage);
        CombatDrawQueue.m_885().m_884();
        CombatTargetEffects.compositeBeforeHands();
    }

    @Inject(remap = false, method = "render", at = @At(remap = false, value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void pizzadlc$compositeOutline(DeltaTracker tracker, boolean render, CallbackInfo ci) {
        if (CombatRuntime.gameplay() && pizzadlc$effectsStarted) CombatTargetEffects.compositeOutline();
    }
}
