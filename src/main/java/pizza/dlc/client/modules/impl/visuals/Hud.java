package pizza.dlc.client.modules.impl.visuals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.draggable.HudContextMenu;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.render.EventRenderHud;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.util.profiler.PizzaDLCProfiler;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.client.modules.impl.visuals.hud.ArmorHud;
import pizza.dlc.client.modules.impl.visuals.hud.ArrayListHud;
import pizza.dlc.client.modules.impl.visuals.hud.CooldownsHud;
import pizza.dlc.client.modules.impl.visuals.hud.HotKeysHud;
import pizza.dlc.client.modules.impl.visuals.hud.HudStyle;
import pizza.dlc.client.modules.impl.visuals.hud.ItemBindsHud;
import pizza.dlc.client.modules.impl.visuals.hud.MusicHud;
import pizza.dlc.client.modules.impl.visuals.hud.PotionsHud;
import pizza.dlc.client.modules.impl.visuals.hud.ProfilerHud;
import pizza.dlc.client.modules.impl.visuals.hud.StaffOnlineHud;
import pizza.dlc.client.modules.impl.visuals.hud.TargetHud;
import pizza.dlc.client.modules.impl.visuals.hud.TotemHud;

@ModuleInfo(
   name = "Hud",
   category = ModuleCategory.VISUALS,
   key = 344
)
public class Hud extends Module {
   private final MultiModeSetting elements = new MultiModeSetting(
      "Элементы",
      this,
      "Watermark",
      "ArrayList",
      "Armor",
      "HotKeys",
      "TargetHUD",
      "Potions",
      "Cooldowns",
      "Staff",
      "Music",
      "Totems",
      "ItemBinds",
      "Profiler",
      "TPS",
      "BPS"
   );
   private final MultiModeSetting itemBinds = new MultiModeSetting("ItemBinds: показывать", this, ItemBindsHud.labels());
   private final ModeSetting itemBindsOrient = new ModeSetting("ItemBinds: ориентация", this, "Горизонтально", "Вертикально");
   private final ModeSetting targetParticles;
   private final ModeSetting targetHpStyle;
   private final BooleanSetting potionIcons;
   private final MultiModeSetting arrayListHidden;
   private final BooleanSetting arrayListMode;
   private final BooleanSetting arrayListLower;
   private final BooleanSetting watermarkAnim;
   private double dispFps;
   private double serverTps;
   private long tpsGameTime;
   private long tpsNanos;
   private double bps;
   private double lastX;
   private double lastZ;
   private boolean haveLastPos;
   private final HotKeysHud hotKeysHud;
   private final TargetHud targetHud;
   private final PotionsHud potionsHud;
   private final CooldownsHud cooldownsHud;
   private final StaffOnlineHud staffOnlineHud;
   private final ArrayListHud arrayListHud;
   private final ArmorHud armorHud;
   private final MusicHud musicHud;
   private final TotemHud totemHud;
   private final ItemBindsHud itemBindsHud;
   private final ProfilerHud profilerHud;
   private final Draggable watermarkDrag;
   private static final String A_APPLES = "Голд-яблоки";
   private static final String A_TOTEMS = "Тотемы";
   private static final String A_POTIONS = "Зелья";
   private final MultiModeSetting alerts;
   private static final long ALERT_DURATION = 2500L;
   private final Map<UUID, UseState> useTracking;
   private static float watermarkX = -1.0F;
   private static float watermarkBottom = -1.0F;
   private static final int WM_H = 20;
   private static final float WM_PAD = 6.0F;
   private static final float WM_GEM = 12.0F;
   private static final float WM_GEM_GAP = 5.0F;
   private static final float WM_NAME_SIZE = 9.0F;
   private static final float WM_SIZE = 8.0F;
   private static final float WM_UNIT_SIZE = 7.0F;
   private static final float CHIP_GAP = 6.0F;
   private static final int STAT_H = 16;
   private static final float STAT_PAD = 6.0F;
   private static final float STAT_MARGIN = 4.0F;

   public Hud() {
      this.itemBinds.setVisible(() -> this.elements.get("ItemBinds").isEnabled());
      this.itemBindsOrient.setVisible(() -> this.elements.get("ItemBinds").isEnabled());
      String[] bindLabels = ItemBindsHud.labels();

      for (int i = 0; i < bindLabels.length; i++) {
         int idx = i;
         this.itemBinds.get(i).setVisible(() -> this.elements.get("ItemBinds").isEnabled() && ItemBindsHud.keyOf(idx) != -1);
      }

      this.targetParticles = new ModeSetting("Партиклы цели", this, "Звёзды", "Кружки");
      this.targetHpStyle = new ModeSetting("HP цели", this, "Кружок", "Полоска");
      this.potionIcons = new BooleanSetting("Показывать иконки зелий", this);
      this.arrayListHidden = new MultiModeSetting("ArrayList: скрыть категории", this, "Combat", "Movement", "Player", "Visuals", "Other", "Environment");
      this.arrayListMode = new BooleanSetting("ArrayList: режим модуля", this);
      this.arrayListLower = new BooleanSetting("ArrayList: с маленькой буквы", this);
      this.watermarkAnim = new BooleanSetting("Watermark: анимированный", this);
      this.serverTps = 20.0;
      this.tpsGameTime = Long.MIN_VALUE;
      this.hotKeysHud = new HotKeysHud();
      this.targetHud = new TargetHud();
      this.potionsHud = new PotionsHud();
      this.cooldownsHud = new CooldownsHud();
      this.staffOnlineHud = new StaffOnlineHud();
      this.arrayListHud = new ArrayListHud();
      this.armorHud = new ArmorHud();
      this.musicHud = new MusicHud();
      this.totemHud = new TotemHud();
      this.itemBindsHud = new ItemBindsHud();
      this.profilerHud = new ProfilerHud();
      this.watermarkDrag = new Draggable("Watermark", 4.0F, 4.0F, 0.0F, 0.0F);
      this.alerts = new MultiModeSetting("Уведомления", this, A_APPLES, A_TOTEMS, A_POTIONS);
      this.useTracking = new HashMap<>();
      this.elements.get("Watermark").setEnabled(true);
      this.elements.get("ArrayList").setEnabled(true);
      this.elements.get("Armor").setEnabled(true);
      this.elements.get("TPS").setEnabled(true);
      this.elements.get("BPS").setEnabled(true);
      this.arrayListMode.setEnabled(true);
      this.watermarkAnim.setEnabled(true);
      this.alerts.get(A_APPLES).setEnabled(true);
      this.alerts.get(A_TOTEMS).setEnabled(true);
      this.alerts.get(A_POTIONS).setEnabled(true);
      this.wireContextMenu("TargetHUD", this.targetParticles);
      this.wireContextMenu("ArrayList", this.arrayListMode, this.arrayListLower);
      this.wireContextMenu("Watermark", this.watermarkAnim);
   }

   private void wireContextMenu(String name, Setting... settings) {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getDraggableManager() != null) {
         Draggable d = cc.getDraggableManager().getDraggable(name);
         if (d != null) {
            d.setContextSettings(List.of(settings));
         }
      }
   }

   public boolean isPotionsHudActive() {
      return this.isEnabled() && this.elements.get("Potions").isEnabled();
   }

   private boolean arrayListShows(Module m) {
      if (m.getCategory() == ModuleCategory.THEMES) {
         return false;
      }

      if (m.getName().equalsIgnoreCase("Sounds")) {
         return false;
      }

      BooleanSetting opt = this.arrayListHidden.get(m.getCategory().getDisplayName());
      return opt == null || !opt.isEnabled();
   }

   @EventHandler
   public void onRender2D(EventRenderHud e) {
      if (mc.level == null) {
         watermarkBottom = -1.0F;
         watermarkX = -1.0F;
      } else {
         GuiGraphicsExtractor ctx = e.getContext();
         boolean editMode = mc.gui.screen() instanceof ChatScreen;
         PizzaDLCProfiler.setEnabled(this.elements.get("Profiler").isEnabled());
         long t = PizzaDLCProfiler.begin();
         if (this.elements.get("Watermark").isEnabled()) {
            this.drawWatermark(ctx);
         } else {
            watermarkBottom = -1.0F;
            watermarkX = -1.0F;
         }

         PizzaDLCProfiler.end("Watermark", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("ArrayList").isEnabled()) {
            this.arrayListHud.render(ctx, editMode, this::arrayListShows, this.arrayListMode.isEnabled(), this.arrayListLower.isEnabled());
         }

         PizzaDLCProfiler.end("ArrayList", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("Armor").isEnabled()) {
            this.armorHud.render(ctx);
         }

         PizzaDLCProfiler.end("Armor", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("HotKeys").isEnabled()) {
            this.hotKeysHud.render(ctx, editMode);
         }

         PizzaDLCProfiler.end("HotKeys", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("TargetHUD").isEnabled()) {
            this.targetHud.render(ctx, editMode, this.targetParticles.is("Звёзды"), this.targetHpStyle.is("Полоска"));
         }

         PizzaDLCProfiler.end("TargetHUD", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("Potions").isEnabled()) {
            this.potionsHud.render(ctx, editMode, this.potionIcons.isEnabled());
         }

         PizzaDLCProfiler.end("Potions", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("Cooldowns").isEnabled()) {
            this.cooldownsHud.render(ctx, editMode);
         }

         PizzaDLCProfiler.end("Cooldowns", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("Staff").isEnabled()) {
            this.staffOnlineHud.render(ctx, editMode);
         }

         PizzaDLCProfiler.end("Staff", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("Music").isEnabled()) {
            this.musicHud.render(ctx, editMode);
         }

         PizzaDLCProfiler.end("Music", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("Totems").isEnabled()) {
            this.totemHud.render(ctx, editMode);
         }

         PizzaDLCProfiler.end("Totems", t);
         t = PizzaDLCProfiler.begin();
         if (this.elements.get("ItemBinds").isEnabled()) {
            this.itemBindsHud.render(ctx, editMode, this.itemBinds, this.itemBindsOrient.is("Горизонтально"));
         }

         PizzaDLCProfiler.end("ItemBinds", t);
         if (this.elements.get("Profiler").isEnabled()) {
            this.profilerHud.render(ctx, editMode);
         }

         boolean tps = this.elements.get("TPS").isEnabled();
         boolean bps = this.elements.get("BPS").isEnabled();
         if (tps || bps) {
            this.drawStats(ctx, tps, bps);
         }

         HudContextMenu.render(ctx);
         PizzaDLCProfiler.endFrame();
      }
   }

   @EventHandler
   public void onTimeSync(PacketEvent.Receive e) {
      if (!(e.getPacket() instanceof ClientboundSetTimePacket pkt)) return;

      long gameTime = pkt.gameTime();
      long now = System.nanoTime();
      if (this.tpsGameTime != Long.MIN_VALUE) {
         long dticks = gameTime - this.tpsGameTime;
         double dt = (now - this.tpsNanos) / 1.0E9;
         if (dticks > 0L && dticks <= 1200L && dt > 0.05 && dt < 10.0) {
            double sample = Math.min(20.0, dticks / dt);
            this.serverTps = this.serverTps + (sample - this.serverTps) * 0.4;
         } else {
            this.serverTps = 20.0;
         }
      }

      this.tpsGameTime = gameTime;
      this.tpsNanos = now;
   }

   @EventHandler
   public void onBpsTick(EventTick e) {
      if (mc.player == null) {
         this.haveLastPos = false;
      } else {
         double x = mc.player.getX();
         double z = mc.player.getZ();
         if (this.haveLastPos) {
            double dx = x - this.lastX;
            double dz = z - this.lastZ;
            double sample = Math.sqrt(dx * dx + dz * dz) * 20.0;
            this.bps = this.bps + (sample - this.bps) * 0.35;
         }

         this.lastX = x;
         this.lastZ = z;
         this.haveLastPos = true;
      }
   }

   private void drawStats(GuiGraphicsExtractor ctx, boolean tps, boolean bps) {
      if (mc.gui.screen() instanceof ChatScreen) return;

      List<String[]> stats = new ArrayList<>();
      if (bps) {
         stats.add(new String[]{String.format(Locale.ROOT, "%.1f", this.bps), "bps"});
      }

      if (tps) {
         stats.add(new String[]{String.format(Locale.ROOT, "%.1f", this.serverTps), "tps"});
      }

      float w = chipsWidth(stats) + STAT_PAD * 2;
      float x = ctx.guiWidth() - STAT_MARGIN - w;
      float y = ctx.guiHeight() - STAT_MARGIN - STAT_H;
      HudStyle.panel(ctx, x, y, w, STAT_H, 5.0F, 1.0F);
      this.drawChips(ctx, stats, x + STAT_PAD, y, STAT_H, tps ? tpsColor(this.serverTps) : HudStyle.TEXT);
   }

   private static float chipsWidth(List<String[]> chips) {
      float w = 0.0F;

      for (int i = 0; i < chips.size(); i++) {
         if (i > 0) {
            w += CHIP_GAP * 2 + 1;
         }

         w += HudStyle.title().width(chips.get(i)[0], WM_SIZE) + 2.5F + HudStyle.body().width(chips.get(i)[1], WM_UNIT_SIZE);
      }

      return w;
   }

   private void drawChips(GuiGraphicsExtractor ctx, List<String[]> chips, float x, float y, float h, int lastColor) {
      float cx = x;

      for (int i = 0; i < chips.size(); i++) {
         if (i > 0) {
            cx += CHIP_GAP;
            Render2DUtil.rect(ctx, Math.round(cx), Math.round(y + (h - 9.0F) / 2.0F), 1, 9, 0x22FFFFFF);
            cx += 1 + CHIP_GAP;
         }

         String value = chips.get(i)[0];
         String unit = chips.get(i)[1];
         int valueColor = i == chips.size() - 1 ? lastColor : HudStyle.TEXT;
         Render2DUtil.msdfText(ctx, HudStyle.title(), value, cx, HudStyle.textY(y, h, WM_SIZE), WM_SIZE, valueColor);
         cx += HudStyle.title().width(value, WM_SIZE) + 2.5F;
         float unitY = HudStyle.textY(y, h, WM_SIZE) + 0.7F * (WM_SIZE - WM_UNIT_SIZE);
         Render2DUtil.msdfText(ctx, HudStyle.body(), unit, cx, unitY, WM_UNIT_SIZE, HudStyle.TEXT_DIM);
         cx += HudStyle.body().width(unit, WM_UNIT_SIZE);
      }
   }

   private static int tpsColor(double tps) {
      if (tps >= 18.0) {
         return HudStyle.TEXT;
      }

      return tps >= 14.0 ? HudStyle.WARN : HudStyle.BAD;
   }

   public static float watermarkX() {
      return watermarkX;
   }

   public static float watermarkBottom() {
      return watermarkBottom;
   }

   private void drawWatermark(GuiGraphicsExtractor ctx) {
      this.dispFps = this.dispFps + (mc.getFps() - this.dispFps) * 0.08;
      int fps = (int)Math.round(this.dispFps);
      int ping = this.ping();
      String name = HudStyle.CLIENT_NAME;
      MsdfFont titleFont = HudStyle.title();
      List<String[]> chips = List.of(new String[]{String.valueOf(fps), "fps"}, new String[]{String.valueOf(Math.max(0, ping)), "ms"});
      float nameW = titleFont.width(name, WM_NAME_SIZE);
      float textX = WM_PAD + WM_GEM + WM_GEM_GAP;
      int bw = (int)Math.ceil(textX + nameW + CHIP_GAP * 1.5F + 1 + CHIP_GAP + chipsWidth(chips) + WM_PAD + 1);
      int bh = WM_H;
      this.watermarkDrag.setWidth(bw);
      this.watermarkDrag.setHeight(bh);
      this.watermarkDrag.update(ctx);
      int bx = (int)this.watermarkDrag.getRenderX();
      int by = (int)this.watermarkDrag.getRenderY();
      watermarkX = bx;
      watermarkBottom = by + bh;
      HudStyle.panel(ctx, bx, by, bw, bh, HudStyle.RADIUS, 1.0F);
      HudStyle.gem(ctx, bx + WM_PAD + WM_GEM / 2.0F, by + bh / 2.0F, WM_GEM, 1.0F);
      float nameY = HudStyle.textY(by, bh, WM_NAME_SIZE);
      if (this.watermarkAnim.isEnabled()) {
         drawShimmerName(ctx, titleFont, name, bx + textX, nameY, WM_NAME_SIZE);
      } else {
         Render2DUtil.msdfText(ctx, titleFont, name, bx + textX, nameY, WM_NAME_SIZE, HudStyle.TEXT);
      }

      float divX = bx + textX + nameW + CHIP_GAP * 1.5F;
      Render2DUtil.rect(ctx, Math.round(divX), Math.round(by + (bh - 9.0F) / 2.0F), 1, 9, 0x22FFFFFF);
      this.drawChips(ctx, chips, divX + 1 + CHIP_GAP, by, bh, ping < 0 ? HudStyle.TEXT_DIM : pingColor(ping));
   }

   private static int pingColor(int ping) {
      if (ping <= 120) {
         return HudStyle.TEXT;
      }

      return ping <= 250 ? HudStyle.WARN : HudStyle.BAD;
   }

   private static void drawShimmerName(GuiGraphicsExtractor ctx, MsdfFont font, String text, float x, float y, float size) {
      float speed = 0.35F;
      float width = 0.45F;
      float gap = 0.9F;
      float phase = (float)(System.nanoTime() / 1.0E9 * speed % 1.0);
      float glint = phase * (1.0F + 2.0F * gap) - gap;
      int n = text.length();
      float cx = x;

      for (int i = 0; i < n; i++) {
         String ch = String.valueOf(text.charAt(i));
         float frac = n <= 1 ? 0.5F : (float)i / (n - 1);
         float m = Math.max(0.0F, 1.0F - Math.abs(frac - glint) / width);
         m = m * m * (3.0F - 2.0F * m);
         int col = Render2DUtil.lerpColor(HudStyle.TEXT_OFF, 0xFFFFFFFF, 0.35F + 0.65F * m);
         Render2DUtil.msdfText(ctx, font, ch, cx, y, size, col);
         cx += font.width(ch, size);
      }
   }

   private boolean alertOn(String event) {
      BooleanSetting s = this.alerts.get(event);
      return s != null && s.isEnabled();
   }

   @EventHandler
   public void onTotemAlert(PacketEvent.Receive e) {
      if (mc.level == null || !this.alertOn(A_TOTEMS)) return;

      if (e.getPacket() instanceof ClientboundEntityEventPacket eep && eep.getEventId() == 35) {
         mc.execute(() -> {
            if (mc.level != null && eep.getEntity(mc.level) instanceof Player p && p != mc.player) {
               NotificationManager.add(p.getName().getString(), "тотем!", NotificationType.WARNING, ALERT_DURATION, new ItemStack(Items.TOTEM_OF_UNDYING), true);
            }
         });
      }
   }

   @EventHandler
   public void onUseAlert(EventTick e) {
      if (mc.level == null || mc.player == null) return;

      for (Player p : mc.level.players()) {
         if (p == mc.player) continue;

         UUID id = p.getUUID();
         ItemStack use = p.isUsingItem() ? p.getUseItem() : null;
         Item item = use != null && !use.isEmpty() ? use.getItem() : null;
         boolean apple = item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
         boolean potion = item == Items.POTION;
         boolean notable = apple && this.alertOn(A_APPLES) || potion && this.alertOn(A_POTIONS);
         UseState st = this.useTracking.get(id);
         if (notable) {
            int remaining = p.getUseItemRemainingTicks();
            if (st != null && st.stack.getItem() == item) {
               st.minRemaining = Math.min(st.minRemaining, remaining);
            } else {
               this.useTracking.put(id, new UseState(use.copy(), potion, remaining));
            }
         } else if (st != null) {
            this.useTracking.remove(id);
            if (st.minRemaining <= 2) {
               NotificationManager.add(
                  p.getName().getString(),
                  (st.potion ? "выпил " : "съел ") + st.stack.getHoverName().getString(),
                  NotificationType.INFO,
                  ALERT_DURATION,
                  st.stack,
                  true
               );
            }
         }
      }
   }

   private int ping() {
      if (mc.player != null && mc.getConnection() != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         return info == null ? -1 : Math.max(0, info.getLatency());
      }

      return -1;
   }

   private static final class UseState {
      final ItemStack stack;
      final boolean potion;
      int minRemaining;

      UseState(ItemStack stack, boolean potion, int remaining) {
         this.stack = stack;
         this.potion = potion;
         this.minRemaining = remaining;
      }
   }
}
