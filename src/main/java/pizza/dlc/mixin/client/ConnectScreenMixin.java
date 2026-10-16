package pizza.dlc.mixin.client;

import pizza.dlc.api.translation.Translations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.ReconnectGate;

@Mixin({ConnectScreen.class})
public abstract class ConnectScreenMixin {
   @Shadow
   private Component status;
   @Unique
   private long pizzadlc$connectAt;
   @Unique
   private int pizzadlc$lastSecs = -1;
   @Unique
   private Minecraft pizzadlc$mc;
   @Unique
   private ServerAddress pizzadlc$addr;
   @Unique
   private ServerData pizzadlc$server;
   @Unique
   private TransferState pizzadlc$transfer;

   @Shadow
   private void connect(Minecraft minecraft, ServerAddress hostAndPort, ServerData server, TransferState transferState) {
      throw new AssertionError();
   }

   @Inject(
      method = {"connect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$delayReconnect(Minecraft minecraft, ServerAddress hostAndPort, ServerData server, TransferState transferState, CallbackInfo ci) {
      if (this.pizzadlc$connectAt == 0L && ReconnectGate.consume()) {
         this.pizzadlc$mc = minecraft;
         this.pizzadlc$addr = hostAndPort;
         this.pizzadlc$server = server;
         this.pizzadlc$transfer = transferState;
         this.pizzadlc$connectAt = System.currentTimeMillis() + 3000L;
         ci.cancel();
      }
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$tickDelay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
      if (this.pizzadlc$connectAt != 0L) {
         long remaining = this.pizzadlc$connectAt - System.currentTimeMillis();
         if (remaining <= 0L) {
            this.pizzadlc$connectAt = 0L;
            this.pizzadlc$lastSecs = -1;
            Minecraft mc = this.pizzadlc$mc;
            ServerAddress addr = this.pizzadlc$addr;
            ServerData srv = this.pizzadlc$server;
            TransferState tr = this.pizzadlc$transfer;
            mc.execute(() -> this.connect(mc, addr, srv, tr));
         } else {
            int secs = (int)Math.ceil(remaining / 1000.0);
            if (secs != this.pizzadlc$lastSecs) {
               this.pizzadlc$lastSecs = secs;
               this.status = Component.literal(Translations.text("Переподключение через ") + secs + "…");
            }
         }
      }
   }
}
