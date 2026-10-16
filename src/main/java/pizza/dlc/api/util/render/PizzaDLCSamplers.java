package pizza.dlc.api.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;

public final class PizzaDLCSamplers {
   private PizzaDLCSamplers() {
   }

   public static GpuSampler linear() {
      return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
   }

   public static GpuSampler nearest() {
      return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
   }
}
