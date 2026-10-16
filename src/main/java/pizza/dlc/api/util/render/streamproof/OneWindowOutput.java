package pizza.dlc.api.util.render.streamproof;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;
import org.lwjgl.system.Platform;

final class OneWindowOutput implements AutoCloseable {
   private final Minecraft minecraft;
   private final CleanFeedWindow overlay;
   private TextureTarget clean;

   OneWindowOutput(Minecraft minecraft) throws Exception {
      this.minecraft = minecraft;
      if (Platform.get() != Platform.WINDOWS)
         throw new IllegalStateException("Одно окно поддерживается только на Windows");
      if (minecraft.getWindow().isFullscreen()) throw new IllegalStateException("Для режима «Одно окно» включи оконный режим Minecraft");
      this.overlay = new CleanFeedWindow(minecraft, true);
      this.overlay.syncOverlay();
   }

   void sync() { this.overlay.syncOverlay(); }

   boolean canPresent() { return this.overlay.canPresent(); }

   private boolean ensureTarget(RenderTarget main) {
      if (this.clean != null && this.clean.width == main.width && this.clean.height == main.height
         && this.clean.getColorTexture().getFormat() == main.getColorTexture().getFormat()) return false;
      if (this.clean != null) this.clean.destroyBuffers();
      this.clean = new TextureTarget("StreamProof clean frame", main.width, main.height, false, main.getColorTexture().getFormat());
      var encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.clearColorTexture(this.clean.getColorTexture(), new Vector4f(0, 0, 0, 1));
      encoder.submit();
      return true;
   }

   void rememberClean(RenderTarget main) {
      this.ensureTarget(main);
      copy(main, this.clean);
   }

   void presentClient(RenderTarget main) throws Exception {
      this.ensureTarget(main);
      if (this.minecraft.getWindow().isFullscreen()) throw new IllegalStateException("Режим «Одно окно» требует оконного режима");
      try {
         this.overlay.syncOverlay();
         if (this.overlay.canPresent()) this.overlay.present(main.getColorTextureView());
      } finally {
         copy(this.clean, main);
      }
   }

   private static void copy(RenderTarget source, RenderTarget target) {
      var encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.copyTextureToTexture(source.getColorTexture(), target.getColorTexture(), 0, 0, 0, 0, 0, source.width, source.height);
      encoder.submit();
   }

   @Override public void close() {
      try { this.overlay.close(); }
      finally {
         if (this.clean != null) { this.clean.destroyBuffers(); this.clean = null; }
      }
   }
}
