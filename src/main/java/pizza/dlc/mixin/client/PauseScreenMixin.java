package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.ReconnectGate;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin({PauseScreen.class})
public abstract class PauseScreenMixin extends Screen {
   @Shadow
   private Button disconnectButton;

   protected PauseScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$addReconnect(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      ServerData server = mc.getCurrentServer();
      if (this.disconnectButton != null && server != null) {
         int x = this.disconnectButton.getX();
         int y = this.disconnectButton.getY() + this.disconnectButton.getHeight() + 4;
         int w = this.disconnectButton.getWidth();
         Button reconnect = Button.builder(Component.literal("Reconnect"), b -> {
            mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
            ReconnectGate.arm();
            ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc, ServerAddress.parseString(server.ip), server, false, null);
         }).bounds(x, y, w, 20).build();
         CleanRenderScope.hideWidget(reconnect);
         this.addRenderableWidget(reconnect);
      }
   }
}
