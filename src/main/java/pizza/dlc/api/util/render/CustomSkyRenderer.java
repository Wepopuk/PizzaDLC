package pizza.dlc.api.util.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.GpuBufferSlice.MappedView;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.CustomSky;

public final class CustomSkyRenderer {
   private static MappableRingBuffer ubo;
   private static String brokenMode;

   private CustomSkyRenderer() {
   }

   public static boolean renderSky() {
      try {
         return renderSkyInner();
      } catch (Throwable t) {
         try {
            CustomSky sky = active();
            brokenMode = sky != null ? sky.getMode().get() : "?";
         } catch (Throwable ignored) {
         }
         return false;
      }
   }

   private static boolean renderSkyInner() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.gameRenderer.mainRenderTarget();
      CustomSky sky = active();
      if (main == null || sky == null) return false;
      RenderPipeline pipe = pipeFor(sky.getMode().get());
      if (pipe == null) return false;

      Camera cam = mc.gameRenderer.mainCamera();
      float yawRad = cam != null ? (float) Math.toRadians(-cam.yRot()) : 0.0F;
      float pitchRad = cam != null ? (float) Math.toRadians(cam.xRot()) : 0.0F;
      float fov = cam != null ? cam.getFov() : 70.0F;
      float w = Math.max(1, main.width);
      float h = Math.max(1, main.height);
      float time = (System.currentTimeMillis() - sky.getStartMillis()) / 1000.0F;

      boolean theme = sky.getUseThemeColor().isEnabled();
      int c1 = ThemeManager.accent();
      int c2 = ThemeManager.secondary();
      float r1 = theme ? (c1 >> 16 & 0xFF) / 255.0F : 1.0F;
      float g1 = theme ? (c1 >> 8 & 0xFF) / 255.0F : 1.0F;
      float b1 = theme ? (c1 & 0xFF) / 255.0F : 1.0F;
      float r2 = theme ? (c2 >> 16 & 0xFF) / 255.0F : 1.0F;
      float g2 = theme ? (c2 >> 8 & 0xFF) / 255.0F : 1.0F;
      float b2 = theme ? (c2 & 0xFF) / 255.0F : 1.0F;

      float rain = 0.0F;
      float thunder = 0.0F;
      try {
         if (mc.level != null) {
            rain = mc.level.getRainLevel(1.0F);
            thunder = mc.level.getThunderLevel(1.0F);
         }
      } catch (Throwable ignored) {
      }
      float night = sky.getMode().is("Летнее небо") && sky.getSummerTime().is("Ночное") ? 1.0F : 0.0F;

      if (ubo == null) {
         ubo = new MappableRingBuffer(() -> "pizzadlc_sky_ubo", 130, 128);
      }
      MappedView view = ubo.currentBuffer().map(false, true);
      try {
         Std140Builder b = Std140Builder.intoBuffer(view.data());
         b.putVec2(w, h);
         b.putFloat(time);
         b.putFloat(fov);
         b.putVec2(yawRad, pitchRad);
         b.putFloat(sky.getScale().getValue());
         b.putFloat(sky.getIntensity().getValue());
         b.putFloat(sky.getAlpha().getValue());
         b.putFloat(sky.getSpeed().getValue());
         b.putVec4(r1, g1, b1, sky.getAlpha().getValue());
         b.putVec4(r2, g2, b2, sky.getAlpha().getValue());
         b.putFloat(night);
         b.putFloat(rain);
         b.putFloat(thunder);
         b.putFloat((sky.getMode().is("Ночной") || theme) ? 1.0F : 0.0F);
      } catch (Throwable t) {
         try {
            view.close();
         } catch (Throwable ignored) {
         }
         throw new RuntimeException(t);
      }
      view.close();

      CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
      RenderPass p = enc.createRenderPass(() -> "pizzadlc_sky", main.getColorTextureView(), Optional.empty());
      try {
         p.setPipeline(pipe);
         RenderSystem.bindDefaultUniforms(p);
         p.setUniform("SkyConfig", ubo.currentBuffer());
         p.draw(3, 1, 0, 0);
      } catch (Throwable t) {
         try {
            p.close();
         } catch (Throwable ignored) {
         }
         throw new RuntimeException(t);
      }
      p.close();
      ubo.rotate();
      return true;
   }

   private static RenderPipeline pipeFor(String mode) {
      return switch (mode) {
         case "Сакура" -> PizzaDLCPipelines.SKY_SAKURA;
         case "Плазма" -> PizzaDLCPipelines.SKY_PLASMA;
         case "Плазма 2" -> PizzaDLCPipelines.SKY_PLASMA2;
         case "Северное сияние" -> PizzaDLCPipelines.SKY_NORTHERN;
         case "Ночной" -> PizzaDLCPipelines.SKY_NIGHT;
         case "Летнее небо" -> PizzaDLCPipelines.SKY_SUMMER;
         case "Каустик" -> PizzaDLCPipelines.SKY_CAUSTIC;
         default -> PizzaDLCPipelines.SKY_AURORA;
      };
   }

   private static CustomSky active() {
      try {
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         if (cc != null && cc.getModuleManager() != null) {
            CustomSky s = cc.getModuleManager().get(CustomSky.class);
            return s != null && s.isEnabled() ? s : null;
         }
      } catch (Throwable ignored) {
      }
      return null;
   }

   public static boolean shouldReplace() {
      CustomSky sky = active();
      if (sky == null) return false;
      return brokenMode == null || !brokenMode.equals(sky.getMode().get());
   }
}
