package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pizza.dlc.api.interfaces.AkrienOldState;

@Mixin(value = {EntityRenderState.class, ItemFeatureRenderer.Submit.class}, remap = false)
public class AkrienOldStateMixin implements AkrienOldState {
   @Unique private boolean pizzadlc$akrienOld;
   @Override public boolean pizzadlc$akrienOld() { return pizzadlc$akrienOld; }
   @Override public void pizzadlc$setAkrienOld(boolean enabled) { pizzadlc$akrienOld = enabled; }
}
