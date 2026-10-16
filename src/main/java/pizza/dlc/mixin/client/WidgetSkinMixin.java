package pizza.dlc.mixin.client;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.skin.UiSkin;

@Mixin(AbstractWidget.class)
public abstract class WidgetSkinMixin {
   @Shadow
   protected float alpha;
   @Shadow
   public boolean active;

   @Inject(method = "extractRenderState", at = @At("HEAD"))
   private void pizzadlc$skinBegin(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      UiSkin.begin(graphics, (AbstractWidget)(Object)this);
   }

   @Inject(method = "extractScrollingStringOverContents", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$skinLabel(ActiveTextCollector output, Component message, int margin, CallbackInfo ci) {
      if (UiSkin.drawLabel((AbstractWidget)(Object)this, message, margin, this.alpha, this.active)) {
         ci.cancel();
      }
   }
}
