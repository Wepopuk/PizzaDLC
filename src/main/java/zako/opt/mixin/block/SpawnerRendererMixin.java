package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.block.SpawnerCache;
import zako.opt.block.SpawnerReplay;

@Mixin(value={SpawnerRenderer.class})
public class SpawnerRendererMixin {
    @WrapOperation(method={"extractRenderState(Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/blockentity/TrialSpawnerRenderer;extractSpawnerData(Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;FLnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;DD)V")})
    private void zakoopt$skipFarDisplayEntity(SpawnerRenderState state, float partialTick, Entity entity, EntityRenderDispatcher dispatcher, double oSpin, double spin, Operation<Void> original, SpawnerBlockEntity spawner, SpawnerRenderState state2, float f, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        double d;
        double distSq = Vec3.atCenterOf((Vec3i)spawner.getBlockPos()).distanceToSqr(camera);
        if (ZakoOptConfig.spawnerCull() && distSq > (d = (double)ZakoOptConfig.spawnerDistance()) * d) {
            state.displayEntity = null;
            return;
        }
        if (entity == null || !SpawnerCache.enabled()) {
            original.call(new Object[]{state, Float.valueOf(partialTick), entity, dispatcher, oSpin, spin});
        } else {
            long tick = entity.level().getGameTime();
            EntityRenderState cached = SpawnerCache.get(spawner.getSpawner(), tick);
            if (cached == null) {
                original.call(new Object[]{state, Float.valueOf(partialTick), entity, dispatcher, oSpin, spin});
                SpawnerCache.put(spawner.getSpawner(), tick, state.displayEntity);
            } else {
                state.displayEntity = cached;
                cached.lightCoords = state.lightCoords;
                state.spin = (float)Mth.lerp((double)partialTick, (double)oSpin, (double)spin) * 10.0f;
                state.scale = 0.53125f;
                float size = Math.max(entity.getBbWidth(), entity.getBbHeight());
                if (size > 1.0f) {
                    state.scale /= size;
                }
            }
        }
        if (ZakoOptConfig.spawnerReplay() && distSq > 36.0) {
            state.spin = (float)spin * 10.0f;
        }
    }

    @WrapOperation(method={"submit(Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/blockentity/SpawnerRenderer;submitEntityInSpawner(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;FFLnet/minecraft/client/renderer/state/level/CameraRenderState;)V")})
    private void zakoopt$replayFar(PoseStack pose, SubmitNodeCollector collector, EntityRenderState entity, EntityRenderDispatcher dispatcher, float spin, float scale, CameraRenderState camera, Operation<Void> original, SpawnerRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        if (!ZakoOptConfig.spawnerReplay() || !SpawnerCache.enabled() || mc.level == null || Vec3.atCenterOf((Vec3i)state.blockPos).distanceToSqr(camera.pos) <= 36.0) {
            original.call(new Object[]{pose, collector, entity, dispatcher, Float.valueOf(spin), Float.valueOf(scale), camera});
            return;
        }
        SpawnerReplay.submit(entity, mc.level.getGameTime(), camera.pos, collector, c -> original.call(new Object[]{pose, c, entity, dispatcher, Float.valueOf(spin), Float.valueOf(scale), camera}));
    }
}

