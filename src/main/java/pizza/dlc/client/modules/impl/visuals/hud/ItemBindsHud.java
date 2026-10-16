package pizza.dlc.client.modules.impl.visuals.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.util.input.KeyNameUtil;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.impl.combat.AutoPotion;
import pizza.dlc.client.modules.impl.combat.AutoSwap;
import pizza.dlc.client.modules.impl.player.ClickAction;
import pizza.dlc.client.modules.impl.player.ElytraHelper;

public final class ItemBindsHud implements QuickImports {
   private static final int ICON = 10;
   private static final int PAD = 5;
   private static final int ICON_GAP = 3;
   private static final int SLOT_GAP = 7;
   private static final int H = 18;
   private static final int ROW_H = 14;
   private static final float RADIUS = 5.0F;
   private static final String EMPTY_TEXT = "Нет биндов";
   private final Draggable drag = new Draggable("ItemBinds", 0.0F, 0.0F, 0.0F, 0.0F);
   private static final List<Slot> SLOTS = List.of(
      new Slot("Паутина", Items.COBWEB, c -> null),
      new Slot("Перл", Items.ENDER_PEARL, c -> get(c, ClickAction.class, m -> m.pearlBind)),
      new Slot("Опыт", Items.EXPERIENCE_BOTTLE, c -> get(c, ClickAction.class, m -> m.expBind)),
      new Slot("Снежок", Items.SNOWBALL, c -> get(c, ClickAction.class, m -> m.snowballBind)),
      new Slot("Яйцо", Items.EGG, c -> get(c, ClickAction.class, m -> m.eggBind)),
      new Slot("Вихрь", Items.WIND_CHARGE, c -> get(c, ClickAction.class, m -> m.windBind)),
      new Slot("Элитра", Items.ELYTRA, c -> get(c, ElytraHelper.class, m -> m.swapBind)),
      new Slot("Фейерверк", Items.FIREWORK_ROCKET, c -> get(c, ElytraHelper.class, m -> m.fireworkBind)),
      new Slot("Оффхенд", Items.TOTEM_OF_UNDYING, c -> get(c, AutoSwap.class, m -> m.swapBind)),
      new Slot("Зелье", Items.SPLASH_POTION, c -> get(c, AutoPotion.class, m -> m.bind))
   );

   private static <T extends Module> BindSetting get(PizzaDLCClient c, Class<T> type, Function<T, BindSetting> field) {
      if (c != null && c.getModuleManager() != null) {
         T module = c.getModuleManager().get(type);
         return module != null ? field.apply(module) : null;
      }

      return null;
   }

   public static String[] labels() {
      return SLOTS.stream().map(Slot::label).toArray(String[]::new);
   }

   public static int keyOf(int index) {
      if (index >= 0 && index < SLOTS.size()) {
         BindSetting b = SLOTS.get(index).bind().apply(PizzaDLCClient.getInstance());
         return b != null ? b.getKey() : -1;
      }

      return -1;
   }

   public void render(GuiGraphicsExtractor ctx, boolean editMode, MultiModeSetting shown, boolean horizontal) {
      if (mc.player == null) return;

      List<Row> rows = new ArrayList<>();

      for (int i = 0; i < SLOTS.size(); i++) {
         if (shown != null && !shown.get(i).isEnabled()) continue;

         int key = keyOf(i);
         if (key != -1) {
            String text = KeyNameUtil.displayName(key);
            float pillW = HudStyle.pillWidth(text);
            rows.add(new Row(new ItemStack(SLOTS.get(i).item()), text, pillW, ICON + ICON_GAP + pillW));
         }
      }

      if (!rows.isEmpty() || editMode) {
         float panelW;
         float panelH;
         if (rows.isEmpty()) {
            panelW = PAD * 2 + HudStyle.body().width(pizza.dlc.api.translation.Translations.text(EMPTY_TEXT), HudStyle.ROW_FS);
            panelH = H;
         } else if (horizontal) {
            panelW = PAD * 2 + (rows.size() - 1) * SLOT_GAP;

            for (Row r : rows) {
               panelW += r.width();
            }

            panelH = H;
         } else {
            float maxW = 0.0F;

            for (Row r : rows) {
               maxW = Math.max(maxW, r.width());
            }

            panelW = PAD * 2 + maxW;
            panelH = (H - ROW_H) + rows.size() * ROW_H;
         }

         int pw = (int)Math.ceil(panelW);
         int ph = (int)Math.ceil(panelH);
         this.drag.setWidth(pw);
         this.drag.setHeight(ph);
         if (!this.drag.isPositioned()) {
            this.drag.setX(4.0F);
            this.drag.setY(94.0F);
            this.drag.syncRenderPosition();
            this.drag.setPositioned(true);
         }

         this.drag.update(ctx);
         int px = (int)this.drag.getRenderX();
         int py = (int)this.drag.getRenderY();
         HudStyle.panel(ctx, px, py, pw, ph, RADIUS, 1.0F);
         if (rows.isEmpty()) {
            Render2DUtil.msdfText(ctx, HudStyle.body(), pizza.dlc.api.translation.Translations.text(EMPTY_TEXT), px + PAD, HudStyle.textY(py, H, HudStyle.ROW_FS), HudStyle.ROW_FS, HudStyle.fade(HudStyle.TEXT_DIM, 0.7F));
         } else {
            float x = px + PAD;
            float y = py + (H - ROW_H) / 2.0F;

            for (Row r : rows) {
               float slotH = ROW_H;
               ctx.pose().pushMatrix();
               ctx.pose().translate(x, y + (slotH - ICON) / 2.0F);
               ctx.pose().scale(0.625F);
               ctx.item(r.stack(), 0, 0);
               ctx.pose().popMatrix();
               HudStyle.pill(ctx, x + ICON + ICON_GAP, y + (slotH - HudStyle.PILL_H) / 2.0F, r.pillW(), r.text(), HudStyle.TEXT, 1.0F);
               if (horizontal) {
                  x += r.width() + SLOT_GAP;
               } else {
                  y += ROW_H;
               }
            }
         }
      }
   }

   private record Row(ItemStack stack, String text, float pillW, float width) {
   }

   private record Slot(String label, Item item, Function<PizzaDLCClient, BindSetting> bind) {
   }
}
