package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pizza.dlc.api.interfaces.CustomModelCarrier;

@Mixin({EntityRenderState.class})
public abstract class EntityRenderStateCustomModelMixin implements CustomModelCarrier {
   @Unique
   private String pizzadlc$customModel;

   @Override
   public String pizzadlc$customModel() {
      return this.pizzadlc$customModel;
   }

   @Override
   public void pizzadlc$setCustomModel(String model) {
      this.pizzadlc$customModel = model;
   }
}
