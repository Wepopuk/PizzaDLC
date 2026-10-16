package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import zako.opt.entity.ItemLod;

// Ported for 26.2: item quads now go through SubmitNodeCollection.submitItem (was ItemRenderer.renderItem in 1.21.11)
@Mixin({SubmitNodeCollection.class})
public class ItemLodMixin {
    @ModifyVariable(method = {"submitItem"}, at = @At("HEAD"), argsOnly = true)
    private List<BakedQuad> zakoopt$lod(List<BakedQuad> quads, PoseStack poseStack, ItemDisplayContext context) {
        if (context.firstPerson() || context == ItemDisplayContext.GUI || !ItemLod.enabled()) {
            return quads;
        }

        Matrix4f m = poseStack.last().pose();
        float dSq = m.m30() * m.m30() + m.m31() * m.m31() + m.m32() * m.m32();
        return dSq > ItemLod.distanceSq() ? ItemLod.flat(quads) : quads;
    }
}
