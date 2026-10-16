package pizza.dlc.client.render;

import meteordevelopment.orbit.EventHandler;
import pizza.dlc.api.events.impl.render.EventRenderHud;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.notification.NotificationManager;

public class NotificationRenderer implements QuickImports {
   @EventHandler
   public void onRenderHud(EventRenderHud e) {
      if (mc.level == null) return;

      NotificationManager.render(e.getContext());
   }
}
