package pizza.dlc.client.modules.impl.visuals.hud;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pizza.dlc.api.animation.Animation;
import pizza.dlc.api.animation.Easing;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.mixin.client.ItemCooldownInstanceAccessor;
import pizza.dlc.mixin.client.ItemCooldownsAccessor;

public final class CooldownsHud implements QuickImports {
   private static final String HEADER_TEXT = "Cooldowns";
   private static final int ROW_H = 14;
   private static final int ICON_SLOT = 13;
   private static final float TIME_GAP = 12.0F;
   private static final float BAR_H = 1.0F;
   private static final long ROW_MS = 300L;
   private static final long PANEL_MS = 260L;
   private static final Easing ROW_EASE = Easing.EASE_OUT_CUBIC;
   private static final Easing PANEL_EASE = Easing.EASE_OUT_CUBIC;
   private final Map<Identifier, Row> entries = new LinkedHashMap<>();
   private final Draggable drag = new Draggable(HEADER_TEXT, 0.0F, 0.0F, 0.0F, 0.0F);
   private final Animation animW = new Animation(PANEL_EASE, PANEL_MS);
   private final Animation panelVis = new Animation(PANEL_EASE, PANEL_MS);
   private boolean defaultAnchored;
   private ItemStack phStack;
   private String phName2 = "Item";
   private static final Item[] PREVIEW_ITEMS = new Item[]{
      Items.ENDER_PEARL, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.CHORUS_FRUIT, Items.TOTEM_OF_UNDYING, Items.WIND_CHARGE
   };

   public void render(GuiGraphicsExtractor ctx, boolean editMode) {
      if (mc.player == null || mc.level == null) return;

      Map<Identifier, ActiveCooldown> active = this.activeCooldowns();

      for (Identifier id : active.keySet()) {
         this.entries.computeIfAbsent(id, k -> new Row());
      }

      MsdfFont font = HudStyle.body();
      float maxRowW = 0.0F;
      float contentH = 0.0F;
      int activeCount = 0;
      Iterator<Entry<Identifier, Row>> iterator = this.entries.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<Identifier, Row> entry = iterator.next();
         Row row = entry.getValue();
         ActiveCooldown cooldown = active.get(entry.getKey());
         boolean present = cooldown != null;
         if (present) {
            activeCount++;
            row.name = cooldown.name;
            row.time = cooldown.time;
            row.progress = cooldown.progress;
            if (row.stack == null) {
               row.stack = ((Item)BuiltInRegistries.ITEM.getValue(entry.getKey())).getDefaultInstance();
            }
         } else {
            row.progress = 0.0F;
         }

         float v = row.anim.run(present ? 1.0F : 0.0F);
         if (!present && v <= 0.001F) {
            iterator.remove();
         } else {
            contentH += v * ROW_H;
            float rowW = ICON_SLOT + font.width(row.name, HudStyle.ROW_FS) + TIME_GAP + HudStyle.pillWidth(row.time);
            maxRowW = Math.max(maxRowW, rowW);
         }
      }

      boolean placeholder = this.entries.isEmpty() && editMode;
      if (placeholder && this.phStack == null) {
         this.pickPreviewItem();
      }

      String phName = placeholder ? this.phName2 : "Item";
      String phTime = "0:00";
      if (placeholder) {
         contentH = ROW_H;
         maxRowW = Math.max(maxRowW, ICON_SLOT + font.width(phName, HudStyle.ROW_FS) + TIME_GAP + HudStyle.pillWidth(phTime));
      }

      boolean wantVisible = !this.entries.isEmpty() || editMode;
      float pa = this.panelVis.run(wantVisible ? 1.0F : 0.0F);
      if (!wantVisible && pa < 0.01F) {
         this.animW.setValue(0.0F);
      } else {
         String badge = String.valueOf(activeCount);
         int targetW = (int)Math.ceil(Math.max(HudStyle.headerWidth(HEADER_TEXT, badge), HudStyle.PAD * 2 + maxRowW));
         if (this.animW.getValue() < 1.0F) {
            this.animW.setValue(targetW);
         }

         int panelW = Math.round(this.animW.run(targetW));
         float panelHf = HudStyle.listPanelH(contentH);
         int panelH = Math.round(panelHf);
         this.drag.setWidth(panelW);
         this.drag.setHeight(panelH);
         if (!this.drag.isPositioned()) {
            this.drag.setX(ctx.guiWidth() - 4 - panelW);
            this.drag.setY(121.0F);
            this.drag.syncRenderPosition();
            this.drag.setPositioned(true);
            this.defaultAnchored = true;
         } else if (this.defaultAnchored && !this.drag.isDragging()) {
            this.drag.setX(ctx.guiWidth() - 4 - panelW);
            this.drag.syncRenderPosition();
         }

         this.drag.update(ctx);
         if (this.drag.isDragging()) {
            this.defaultAnchored = false;
         }

         int panelX = (int)this.drag.getRenderX();
         int panelY = (int)this.drag.getRenderY();
         HudStyle.panel(ctx, panelX, panelY, panelW, panelHf, HudStyle.RADIUS, pa);
         HudStyle.header(ctx, panelX, panelY, panelW, HEADER_TEXT, badge, pa);
         float rowY = panelY + HudStyle.HEADER_H + HudStyle.ROWS_TOP;
         ctx.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
         if (placeholder) {
            this.drawRow(ctx, font, this.phStack, phName, phTime, 0.6F, panelX, panelW, rowY, ROW_H, 0.0F, 0.4F * pa);
         } else {
            for (Row rowx : this.entries.values()) {
               float v = rowx.anim.getValue();
               float slotH = v * ROW_H;
               if (v > 0.001F) {
                  rowx.shownProgress = rowx.shownProgress < 0.0F ? rowx.progress : rowx.shownProgress + (rowx.progress - rowx.shownProgress) * 0.25F;
                  this.drawRow(ctx, font, rowx.stack, rowx.name, rowx.time, rowx.shownProgress, panelX, panelW, rowY, slotH, (1.0F - v) * HudStyle.SLIDE, v * pa);
               }

               rowY += slotH;
            }
         }

         ctx.disableScissor();
      }
   }

   private void drawRow(
      GuiGraphicsExtractor ctx, MsdfFont font, ItemStack stack, String name, String time, float progress, int panelX, int panelW, float rowY, float slotH, float slide, float a
   ) {
      float x = panelX + HudStyle.PAD + slide;
      if (a > 0.05F) {
         drawItem(ctx, stack, x, rowY + (slotH - 9.0F) / 2.0F);
      }

      float pillW = HudStyle.pillWidth(time);
      float pillX = panelX + panelW - HudStyle.PAD - pillW + slide;
      float nameX = x + ICON_SLOT;
      float nameMax = pillX - 6.0F - nameX;
      String shown = HudStyle.truncate(font, name, HudStyle.ROW_FS, nameMax);
      float ty = HudStyle.textY(rowY, slotH, HudStyle.ROW_FS) - 1.0F;
      Render2DUtil.msdfText(ctx, font, shown, nameX, ty, HudStyle.ROW_FS, HudStyle.fade(HudStyle.TEXT, a));
      HudStyle.accentBar(ctx, nameX, rowY + slotH - 2.5F, Math.max(0.0F, nameMax), BAR_H, progress, a);
      HudStyle.pill(ctx, pillX, rowY + (slotH - HudStyle.PILL_H) / 2.0F, pillW, time, HudStyle.TEXT, a);
   }

   private Map<Identifier, ActiveCooldown> activeCooldowns() {
      Map<Identifier, ActiveCooldown> result = new LinkedHashMap<>();
      ItemCooldowns cooldowns = mc.player.getCooldowns();
      ItemCooldownsAccessor cooldownsAccessor = (ItemCooldownsAccessor)cooldowns;
      int tick = cooldownsAccessor.pizzadlc$getTickCount();

      for (Entry<Identifier, Object> entry : cooldownsAccessor.pizzadlc$getCooldowns().entrySet()) {
         if (!(entry.getValue() instanceof ItemCooldownInstanceAccessor cooldown)) continue;

         int remainingTicks = cooldown.pizzadlc$getEndTime() - tick;
         if (remainingTicks > 0) {
            String name = shortName(entry.getKey());
            int totalSec = (remainingTicks + 19) / 20;
            String time = totalSec / 60 + ":" + (totalSec % 60 < 10 ? "0" : "") + totalSec % 60;
            int total = Math.max(1, cooldown.pizzadlc$getEndTime() - cooldown.pizzadlc$getStartTime());
            float progress = Math.max(0.0F, Math.min(1.0F, (float)remainingTicks / total));
            result.put(entry.getKey(), new ActiveCooldown(name, time, progress));
         }
      }

      return result;
   }

   private static String shortName(Identifier id) {
      String path = id.getPath();

      return switch (path) {
         case "enchanted_golden_apple" -> "Ench. Gap";
         case "golden_apple" -> "Gapple";
         case "ender_pearl" -> "Pearl";
         case "chorus_fruit" -> "Chorus";
         case "totem_of_undying" -> "Totem";
         case "wind_charge" -> "Wind Charge";
         default -> fullNameOrPath(id);
      };
   }

   private static String fullNameOrPath(Identifier id) {
      Item item = (Item)BuiltInRegistries.ITEM.getValue(id);
      return item != null && item != Items.AIR ? item.getName(item.getDefaultInstance()).getString() : prettyPath(id.getPath());
   }

   private static String prettyPath(String path) {
      String[] parts = path.split("_");
      StringBuilder result = new StringBuilder();

      for (String part : parts) {
         if (part.isEmpty()) continue;

         if (!result.isEmpty()) {
            result.append(' ');
         }

         result.append(Character.toUpperCase(part.charAt(0)));
         if (part.length() > 1) {
            result.append(part.substring(1));
         }
      }

      return result.isEmpty() ? path : result.toString();
   }

   private static void drawItem(GuiGraphicsExtractor ctx, ItemStack stack, float x, float y) {
      if (stack == null || stack.isEmpty()) return;

      ctx.pose().pushMatrix();
      ctx.pose().translate(x, y);
      ctx.pose().scale(0.5625F);
      ctx.item(stack, 0, 0);
      ctx.pose().popMatrix();
   }

   private void pickPreviewItem() {
      Item it = PREVIEW_ITEMS[(int)(Math.random() * PREVIEW_ITEMS.length)];
      this.phStack = it.getDefaultInstance();
      this.phName2 = shortName(BuiltInRegistries.ITEM.getKey(it));
   }

   private static final class ActiveCooldown {
      final String name;
      final String time;
      final float progress;

      ActiveCooldown(String name, String time, float progress) {
         this.name = name;
         this.time = time;
         this.progress = progress;
      }
   }

   private static final class Row {
      final Animation anim = new Animation(CooldownsHud.ROW_EASE, ROW_MS);
      String name = "";
      String time = "";
      float progress = 0.0F;
      float shownProgress = -1.0F;
      ItemStack stack;
   }
}
