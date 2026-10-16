package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = RenderType.class, remap = false)
public interface CombatRenderTypeAccessor {
   @Invoker(value = "create", remap = false)
   static RenderType create(String var0, RenderSetup var1) {
      throw new AssertionError();
   }

   @Accessor(value = "state", remap = false)
   RenderSetup getRenderSetup();
}
