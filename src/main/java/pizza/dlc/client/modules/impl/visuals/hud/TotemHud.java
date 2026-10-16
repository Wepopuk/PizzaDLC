package pizza.dlc.client.modules.impl.visuals.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.render.Render2DUtil;

public final class TotemHud implements QuickImports {
   private static final int ICON = 10;
   private static final int PAD = 5;
   private static final int GAP = 4;
   private static final int H = 18;
   private static final float SIZE = 8.0F;
   private static final float RADIUS = 5.0F;
   private final Draggable drag = new Draggable("Totems", 0.0F, 0.0F, 0.0F, 0.0F);
   private ItemStack icon;

   public void render(GuiGraphicsExtractor ctx, boolean editMode) {
      if (mc.player == null) return;

      if (this.icon == null) {
         this.icon = new ItemStack(Items.TOTEM_OF_UNDYING);
      }

      int count = this.countTotems();
      MsdfFont font = HudStyle.title();
      String text = String.valueOf(count);
      int textW = (int)Math.ceil(font.width(text, SIZE));
      int panelW = PAD + ICON + GAP + textW + PAD + 1;
      this.drag.setWidth(panelW);
      this.drag.setHeight(H);
      if (!this.drag.isPositioned()) {
         this.drag.setX(4.0F);
         this.drag.setY(64.0F);
         this.drag.syncRenderPosition();
         this.drag.setPositioned(true);
      }

      this.drag.update(ctx);
      int px = (int)this.drag.getRenderX();
      int py = (int)this.drag.getRenderY();
      HudStyle.panel(ctx, px, py, panelW, H, RADIUS, 1.0F);
      ctx.pose().pushMatrix();
      ctx.pose().translate(px + PAD, py + (H - ICON) / 2.0F);
      ctx.pose().scale(0.625F);
      ctx.item(this.icon, 0, 0);
      ctx.pose().popMatrix();
      Render2DUtil.msdfText(ctx, font, text, px + PAD + ICON + GAP, HudStyle.textY(py, H, SIZE), SIZE, count > 0 ? HudStyle.TEXT : HudStyle.BAD);
   }

   private int countTotems() {
      Inventory inv = mc.player.getInventory();
      int total = 0;

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() == Items.TOTEM_OF_UNDYING) {
            total += s.getCount();
         }
      }

      return total;
   }
}
