package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.visuals.Removals;

@Mixin({Hud.class})
public class HudRemovalsMixin {
   @Inject(
      method = {"extractTextureOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removeTextureOverlay(GuiGraphicsExtractor graphics, Identifier texture, float alpha, CallbackInfo ci) {
      String path = texture.getPath();
      if (path.contains("pumpkinblur") && Removals.active("Тыква")) {
         ci.cancel();
      }

      if (path.contains("powder_snow") && Removals.active("Снег")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"extractPortalOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removePortalOverlay(GuiGraphicsExtractor graphics, float alpha, CallbackInfo ci) {
      if (Removals.active("Портал")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"extractConfusionOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removeNauseaOverlay(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
      if (Removals.active("Плохие эффекты")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"extractVignette"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removeVignette(GuiGraphicsExtractor graphics, Entity camera, CallbackInfo ci) {
      if (Removals.active("Виньетка")) {
         ci.cancel();
      }
   }
}
