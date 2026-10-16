package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pizza.dlc.api.interfaces.IChamsState;

@Mixin({EntityRenderState.class})
public class EntityRenderStateChamsMixin implements IChamsState {
   @Unique
   private int pizzadlc$chamsSolid;
   @Unique
   private int pizzadlc$chamsGlow;
   @Unique
   private boolean pizzadlc$chamsHideSkin;

   @Override
   public int pizzadlc$getChamsSolid() {
      return this.pizzadlc$chamsSolid;
   }

   @Override
   public int pizzadlc$getChamsGlow() {
      return this.pizzadlc$chamsGlow;
   }

   @Override
   public boolean pizzadlc$isChamsHideSkin() {
      return this.pizzadlc$chamsHideSkin;
   }

   @Override
   public void pizzadlc$setChams(int solidArgb, int glowArgb, boolean hideSkin) {
      this.pizzadlc$chamsSolid = solidArgb;
      this.pizzadlc$chamsGlow = glowArgb;
      this.pizzadlc$chamsHideSkin = hideSkin;
   }
}
