package pizza.dlc.mixin.client;

import net.minecraft.client.DeltaTracker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.render.EventRender2D;
import pizza.dlc.api.events.impl.render.EventRenderHud;
import pizza.dlc.api.events.impl.render.EventRenderWorld2D;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.api.ui.menu.MainMenuScreen;

@Mixin({Gui.class})
public class GuiMixin {
   @Shadow
   @Final
   private GuiRenderState guiRenderState;

   @Inject(
      method = {"extractRenderState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
         shift = Shift.BEFORE
      )}
   )
   private void pizzadlc$onRenderWorld2D(DeltaTracker delta, boolean b1, boolean b2, CallbackInfo ci) {
      PizzaDLC instance = this.pizzadlc$active();
      if (instance != null) {
         GuiGraphicsExtractor context = new GuiGraphicsExtractor(Minecraft.getInstance(), this.guiRenderState, 0, 0);
         instance.getBus().post(new EventRenderWorld2D(context, delta));
         context.nextStratum();
      }
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
         shift = Shift.AFTER
      )}
   )
   private void pizzadlc$onRenderHud(DeltaTracker delta, boolean b1, boolean b2, CallbackInfo ci) {
      PizzaDLC instance = this.pizzadlc$active();
      if (instance != null) {
         GuiGraphicsExtractor hudContext = new GuiGraphicsExtractor(Minecraft.getInstance(), this.guiRenderState, 0, 0);
         hudContext.nextStratum();
         instance.getBus().post(new EventRenderHud(hudContext, delta));
      }
   }

   @Unique
   private PizzaDLC pizzadlc$active() {
      if (CleanRenderScope.isActive() || StreamProofRenderer.isVanillaGameplayActive()) return null;
      PizzaDLC instance = PizzaDLC.getInstance();
      if (instance != null && instance.getBus() != null) {
         Screen screen = Minecraft.getInstance().gui.screen();
         return !(screen instanceof LevelLoadingScreen) && !(screen instanceof ProgressScreen) && !(screen instanceof ConnectScreen) ? instance : null;
      }

      return null;
   }

   @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "FIELD",
      target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;"))
   private Screen pizzadlc$hideClientScreenInFeed(Screen screen) {
      return CleanRenderScope.isActive() && screen != null && !(screen instanceof MainMenuScreen)
         && screen.getClass().getName().startsWith("pizza.dlc.") ? null : screen;
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$onRender2D(DeltaTracker delta, boolean b1, boolean b2, CallbackInfo ci) {
      PizzaDLC instance = this.pizzadlc$active();
      if (instance != null) {
         GuiGraphicsExtractor context = new GuiGraphicsExtractor(Minecraft.getInstance(), this.guiRenderState, 0, 0);
         context.nextStratum();
         instance.getBus().post(new EventRender2D(context, delta));
      }
   }
}
