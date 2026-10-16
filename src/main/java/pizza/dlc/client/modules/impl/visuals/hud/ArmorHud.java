package pizza.dlc.client.modules.impl.visuals.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.render.Render2DUtil;

public final class ArmorHud implements QuickImports {
   private static final int SLOT = 20;
   private static final int H = 22;
   private static final int GAP = 4;
   private static final float RADIUS = 5.0F;
   private static final float DUR_FS = 6.0F;
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   public void render(GuiGraphicsExtractor ctx) {
      if (mc.player == null || mc.level == null) return;

      boolean any = false;

      for (EquipmentSlot s : SLOTS) {
         if (mc.player.getItemBySlot(s).isEmpty()) continue;

         any = true;
         break;
      }

      if (any) {
         int sw = ctx.guiWidth();
         int sh = ctx.guiHeight();
         int n = SLOTS.length;
         int stripW = n * SLOT + 2;
         int stripX = sw / 2 + 91 + GAP;
         int stripY = sh - H;
         HudStyle.panel(ctx, stripX, stripY, stripW, H, RADIUS, 1.0F);
         MsdfFont font = HudStyle.mono();

         for (int i = 0; i < n; i++) {
            ItemStack stack = mc.player.getItemBySlot(SLOTS[i]);
            int slotX = stripX + 1 + i * SLOT;
            if (stack.isEmpty()) {
               Render2DUtil.roundedRect(ctx, slotX + 7, stripY + 9, 6, 4, 2.0F, HudStyle.TRACK);
            } else {
               int itemX = slotX + 2;
               int itemY = stripY + 3;
               ctx.item(stack, itemX, itemY);
               ctx.itemDecorations(mc.font, stack, itemX, itemY);
               if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                  int max = stack.getMaxDamage();
                  int rem = max - stack.getDamageValue();
                  float frac = Math.max(0.0F, Math.min(1.0F, (float)rem / max));
                  int col = Render2DUtil.lerpColor(HudStyle.BAD, HudStyle.TEXT, Math.min(1.0F, frac * 2.0F));
                  String txt = Math.round(frac * 100.0F) + "%";
                  float tw = font.width(txt, DUR_FS);
                  float tx = slotX + (SLOT - tw) / 2.0F;
                  Render2DUtil.msdfText(ctx, font, txt, tx, stripY - 0.7F * DUR_FS - 2.5F, DUR_FS, col);
               }
            }
         }
      }
   }
}
