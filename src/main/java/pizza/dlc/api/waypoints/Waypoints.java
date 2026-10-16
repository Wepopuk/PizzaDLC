package pizza.dlc.api.waypoints;

import java.util.Locale;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import pizza.dlc.api.events.impl.render.EventRenderWorld2D;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.render.ProjectionUtil;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.PizzaDLCClient;

public class Waypoints implements QuickImports {
   private static final float TAG_SIZE = 12.0F;

   @EventHandler
   public void onRender2D(EventRenderWorld2D e) {
      if (mc.player == null || mc.level == null) return;

      WaypointsManager manager = PizzaDLCClient.getInstance().getWaypointsManager();
      if (manager != null) {
         float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         manager.update(pt);
         if (!manager.getWaypoints().isEmpty()) {
            GuiGraphicsExtractor ctx = e.getContext();
            MsdfFont font = PizzaDLCFonts.sfpro();

            for (WaypointsManager.Waypoint w : manager.getWaypoints().values()) {
               Vector2f s = ProjectionUtil.worldToScreen(w.pos.add(0.0, 0.5, 0.0));
               if (s != null) {
                  float dist = (float)mc.player.position().distanceTo(w.pos);
                  float scale = Mth.clamp(1.0F - dist / 20.0F, 0.5F, 1.0F);
                  String text = w.name + " (" + String.format(Locale.ROOT, "%.1f", dist) + "m)";
                  float tw = Render2DUtil.msdfWidth(font, text, TAG_SIZE);
                  float pillW = tw + 6.0F;
                  float pillH = 14.4F;
                  float x = -pillW / 2.0F;
                  float y = -pillH / 2.0F;
                  ctx.pose().pushMatrix();
                  ctx.pose().translate(s.x, s.y);
                  ctx.pose().scale(scale);
                  Render2DUtil.roundedRect(ctx, x, y, pillW, pillH, 2.0F, 0xC81C1F24);
                  Render2DUtil.msdfText(ctx, font, text, x + 3.0F, y + 3.0F, TAG_SIZE, -1);
                  ctx.pose().popMatrix();
               }
            }
         }
      }
   }
}
