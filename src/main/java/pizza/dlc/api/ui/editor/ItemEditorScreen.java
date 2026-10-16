package pizza.dlc.api.ui.editor;

import pizza.dlc.api.translation.Translations;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import pizza.dlc.api.ui.TypingScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.mixin.client.AbstractContainerScreenAccessor;

public class ItemEditorScreen extends Screen implements QuickImports, TypingScreen {
   public static KeyMapping EDITOR_BIND;
   private static final int LCOL = 290;
   private static final int EFF_DURATION = 9600;
   private static final Identifier MOD_ID = Identifier.fromNamespaceAndPath("pizzadlc", "editor");
   private static final int[] COLORS = new int[]{
      0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
   };
   private ItemStack stack;
   private final int creativeSlot;
   private final Field name = new Field(96, false);
   private final Field count = new Field(3, true);
   private final Field search = new Field(48, false);
   private final Area raw = new Area();
   private Edit focused = null;

   @Override
   public boolean isTyping() {
      return this.focused != null;
   }
   private String rawOriginal = "";
   private int nameColor = -1;
   private boolean bold;
   private boolean italic;
   private boolean underline;
   private boolean strike;
   private boolean obf;
   private String flashMsg = "";
   private int flashColor;
   private long flashAt;
   private int picker = 0;
   private float listScroll = 0.0F;
   private List<Ench> enchList = null;
   private List<Eff> effList = null;
   private List<Attr> attrList = null;

   public ItemEditorScreen(ItemStack stack, int creativeSlot) {
      super(Component.literal(Translations.text("Item Editor")));
      this.stack = stack;
      this.creativeSlot = creativeSlot;
      this.initFromStack();
   }

   public static boolean tryOpen(KeyEvent event) {
      if (EDITOR_BIND != null && EDITOR_BIND.matches(event)) {
         Minecraft m = Minecraft.getInstance();
         if (m.player == null) {
            return false;
         }

         Screen sc = m.gui.screen();
         if (sc instanceof AbstractContainerScreen<?> acs) {
            Slot s = ((AbstractContainerScreenAccessor)acs).pizzadlc$hoveredSlot();
            if (s != null && s.hasItem()) {
               open(s.getItem().copy(), creativeSlotFor(m, s));
               return true;
            }

            return false;
         }

         if (sc == null) {
            ItemStack held = m.player.getMainHandItem();
            if (!held.isEmpty()) {
               open(held.copy(), 36 + m.player.getInventory().getSelectedSlot());
               return true;
            }
         }

         return false;
      }

      return false;
   }

   private static void open(ItemStack stack, int creativeSlot) {
      Minecraft.getInstance().setScreenAndShow(new ItemEditorScreen(stack, creativeSlot));
   }

   private static int creativeSlotFor(Minecraft m, Slot s) {
      if (m.player != null && s.container instanceof Inventory) {
         int inv = s.getContainerSlot();
         if (inv >= 0 && inv < 9) {
            return 36 + inv;
         }

         if (inv >= 9 && inv < 36) {
            return inv;
         }

         return inv == 40 ? 45 : -1;
      }

      return -1;
   }

   private static int invFromMenu(int menu) {
      if (menu >= 36 && menu <= 44) {
         return menu - 36;
      }

      if (menu >= 9 && menu <= 35) {
         return menu;
      }

      return menu == 45 ? 40 : -1;
   }

   private void initFromStack() {
      this.count.set(String.valueOf(this.stack.getCount()));
      Component cn = (Component)this.stack.get(DataComponents.CUSTOM_NAME);
      if (cn != null) {
         this.name.set(cn.getString());
         Style st = cn.getStyle();
         TextColor tc = st.getColor();
         this.nameColor = tc != null ? tc.getValue() & 0xFFFFFF : -1;
         this.bold = st.isBold();
         this.italic = st.isItalic();
         this.underline = st.isUnderlined();
         this.strike = st.isStrikethrough();
         this.obf = st.isObfuscated();
      } else {
         this.name.set("");
         this.nameColor = -1;
         this.bold = this.italic = this.underline = this.strike = this.obf = false;
      }

      this.raw.set(this.currentSnbt());
      this.rawOriginal = this.raw.text;
      this.enchList = null;
      this.effList = null;
      this.attrList = null;
   }

   private void apply() {
      if (mc.player == null || mc.level == null) return;

      if (!mc.player.isCreative()) {
         this.fail(Translations.text("работает только в креативе (сервер игнорит правку предметов в выживании)"));
      } else if (this.creativeSlot < 0) {
         this.fail(Translations.text("слот не распознан — открой редактор на предмете своего инвентаря/хотбара/руки"));
      } else {
         ItemStack out;
         if (!this.raw.text.equals(this.rawOriginal)) {
            ItemStack parsed = this.parseStack(this.raw.text.trim());
            if (parsed == null || parsed.isEmpty()) {
               this.fail(Translations.text("сырой NBT не распарсился"));
               return;
            }

            out = parsed;
         } else {
            out = this.stack.copy();

            try {
               out.setCount(Math.max(1, Math.min(99, Integer.parseInt(this.count.text.trim()))));
            } catch (Exception ignored) {
            }

            if (this.name.text.isBlank()) {
               out.remove(DataComponents.CUSTOM_NAME);
            } else {
               out.set(DataComponents.CUSTOM_NAME, this.buildName());
            }
         }

         int inv = invFromMenu(this.creativeSlot);
         if (inv >= 0) {
            mc.player.getInventory().setItem(inv, out);
         }

         mc.player.connection.send(new ServerboundSetCreativeModeSlotPacket(this.creativeSlot, out));
         this.stack = out;
         this.initFromStack();
         this.msg(Translations.text("§aItemEditor: применено → слот ") + this.creativeSlot + ", x" + out.getCount());
         this.flash(Translations.text("✓ Применено"), 0xFF7BE0A0);
      }
   }

   private void fail(String why) {
      this.msg("§cItemEditor: " + why);
      this.flash("✗ " + why, 0xFFFF6666);
   }

   private void msg(String s) {
      if (mc.player == null) return;

      mc.player.sendSystemMessage(Component.literal(s));
   }

   private Component buildName() {
      Style st = Style.EMPTY;
      if (this.nameColor >= 0) {
         st = st.withColor(this.nameColor);
      }

      if (this.bold) {
         st = st.withBold(true);
      }

      if (this.italic) {
         st = st.withItalic(true);
      }

      if (this.underline) {
         st = st.withUnderlined(true);
      }

      if (this.strike) {
         st = st.withStrikethrough(true);
      }

      if (this.obf) {
         st = st.withObfuscated(true);
      }

      return Component.literal(this.name.text).withStyle(st);
   }

   private String currentSnbt() {
      try {
         RegistryOps<Tag> ops = mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
         Tag tag = (Tag)ItemStack.CODEC.encodeStart(ops, this.stack).result().orElse(null);
         return tag != null ? new SnbtPrinterTagVisitor().visit(tag) : "{}";
      } catch (Exception ignored) {
         return "{}";
      }
   }

   private ItemStack parseStack(String snbt) {
      try {
         CompoundTag tag = TagParser.parseCompoundFully(snbt);
         RegistryOps<Tag> ops = mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
         return (ItemStack)ItemStack.CODEC.parse(ops, tag).result().orElse(null);
      } catch (Exception ignored) {
         return null;
      }
   }

   private void flash(String msg, int color) {
      this.flashMsg = msg;
      this.flashColor = color;
      this.flashAt = System.nanoTime();
   }

   private void refreshRaw() {
      this.raw.set(this.currentSnbt());
      this.rawOriginal = this.raw.text;
   }

   private void buildEnch() {
      this.enchList = new ArrayList<>();

      try {
         Registry<Enchantment> reg = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
         reg.listElements()
            .forEach(
               h -> this.enchList
                  .add(
                     new Ench(
                        h,
                        ((Enchantment)h.value()).description().getString(),
                        h.getRegisteredName(),
                        ((Enchantment)h.value()).getMaxLevel(),
                        ((Enchantment)h.value()).canEnchant(this.stack)
                     )
                  )
            );
         this.enchList.sort(Comparator.<Ench>comparingInt(e -> e.fits() ? 0 : 1).thenComparing(Ench::name));
      } catch (Exception ignored) {
      }
   }

   private void buildEff() {
      this.effList = new ArrayList<>();

      try {
         Registry<MobEffect> reg = mc.level.registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
         reg.listElements()
            .forEach(h -> this.effList.add(new Eff(h, ((MobEffect)h.value()).getDisplayName().getString(), h.getRegisteredName())));
         this.effList.sort(Comparator.comparing(Eff::name));
      } catch (Exception ignored) {
      }
   }

   private void buildAttr() {
      this.attrList = new ArrayList<>();

      try {
         Registry<Attribute> reg = mc.level.registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
         reg.listElements()
            .forEach(
               h -> this.attrList
                  .add(new Attr(h, Component.translatable(((Attribute)h.value()).getDescriptionId()).getString(), h.getRegisteredName()))
            );
         this.attrList.sort(Comparator.comparing(Attr::name));
      } catch (Exception ignored) {
      }
   }

   private int levelOf(Holder<Enchantment> h) {
      return ((ItemEnchantments)this.stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)).getLevel(h);
   }

   private void setEnch(Holder<Enchantment> h, int level) {
      Mutable m = new Mutable((ItemEnchantments)this.stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
      m.set(h, Math.max(0, Math.min(255, level)));
      this.stack.set(DataComponents.ENCHANTMENTS, m.toImmutable());
      this.refreshRaw();
   }

   private int ampOf(Holder<MobEffect> h) {
      for (MobEffectInstance mi : ((PotionContents)this.stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)).customEffects()) {
         if (!mi.getEffect().equals(h)) continue;

         return mi.getAmplifier();
      }

      return -1;
   }

   private void setEff(Holder<MobEffect> h, int amp) {
      PotionContents cur = (PotionContents)this.stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      List<MobEffectInstance> list = new ArrayList<>(cur.customEffects());
      list.removeIf(mi -> mi.getEffect().equals(h));
      if (amp >= 0) {
         list.add(new MobEffectInstance(h, EFF_DURATION, Math.min(amp, 255)));
      }

      this.stack.set(DataComponents.POTION_CONTENTS, new PotionContents(cur.potion(), cur.customColor(), list, cur.customName()));
      this.refreshRaw();
   }

   private double amountOf(Holder<Attribute> h) {
      for (Entry en : ((ItemAttributeModifiers)this.stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)).modifiers()) {
         if (!en.matches(h, MOD_ID)) continue;

         return en.modifier().amount();
      }

      return 0.0;
   }

   private void setAttr(Holder<Attribute> h, double amount) {
      ItemAttributeModifiers cur = (ItemAttributeModifiers)this.stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      List<Entry> kept = new ArrayList<>();

      for (Entry en : cur.modifiers()) {
         if (en.matches(h, MOD_ID)) continue;

         kept.add(en);
      }

      ItemAttributeModifiers base = new ItemAttributeModifiers(kept);
      ItemAttributeModifiers next = amount != 0.0
         ? base.withModifierAdded(h, new AttributeModifier(MOD_ID, amount, Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
         : base;
      this.stack.set(DataComponents.ATTRIBUTE_MODIFIERS, next);
      this.refreshRaw();
   }

   private List<Row> filteredRows() {
      String q = this.search.text.toLowerCase();
      List<Row> out = new ArrayList<>();
      if (this.picker == 1) {
         if (this.enchList == null) {
            this.buildEnch();
         }

         for (Ench e : this.enchList) {
            if (!match(q, e.name(), e.id())) continue;

            Holder<Enchantment> h = e.holder();
            int lvl = this.levelOf(h);
            out.add(
               new Row(
                  e.name(),
                  e.fits(),
                  lvl > 0 ? String.valueOf(lvl) : "0",
                  lvl > 0,
                  () -> this.setEnch(h, this.levelOf(h) - 1),
                  () -> this.setEnch(h, this.levelOf(h) + 1),
                  () -> this.setEnch(h, this.levelOf(h) > 0 ? 0 : 1)
               )
            );
         }
      } else if (this.picker == 2) {
         if (this.effList == null) {
            this.buildEff();
         }

         for (Eff ex : this.effList) {
            if (!match(q, ex.name(), ex.id())) continue;

            Holder<MobEffect> h = ex.holder();
            int amp = this.ampOf(h);
            out.add(
               new Row(
                  ex.name(),
                  true,
                  amp >= 0 ? String.valueOf(amp + 1) : "0",
                  amp >= 0,
                  () -> this.setEff(h, this.ampOf(h) - 1),
                  () -> this.setEff(h, this.ampOf(h) + 1),
                  () -> this.setEff(h, this.ampOf(h) >= 0 ? -1 : 0)
               )
            );
         }
      } else if (this.picker == 3) {
         if (this.attrList == null) {
            this.buildAttr();
         }

         for (Attr a : this.attrList) {
            if (!match(q, a.name(), a.id())) continue;

            Holder<Attribute> h = a.holder();
            double amt = this.amountOf(h);
            out.add(
               new Row(
                  a.name(),
                  true,
                  amt != 0.0 ? fmt(amt) : "0",
                  amt != 0.0,
                  () -> this.setAttr(h, this.amountOf(h) - 1.0),
                  () -> this.setAttr(h, this.amountOf(h) + 1.0),
                  () -> this.setAttr(h, this.amountOf(h) != 0.0 ? 0.0 : 1.0)
               )
            );
         }
      }

      return out;
   }

   private static boolean match(String q, String name, String id) {
      return q.isEmpty() || name.toLowerCase().contains(q) || id.contains(q);
   }

   private static String fmt(double v) {
      return v == Math.floor(v) && !Double.isInfinite(v) ? String.valueOf((long)v) : String.format(Locale.US, "%.1f", v);
   }

   private int winW() {
      return Math.min(this.width - 30, 720);
   }

   private int winH() {
      return Math.min(this.height - 30, 440);
   }

   private int winX() {
      return (this.width - this.winW()) / 2;
   }

   private int winY() {
      return (this.height - this.winH()) / 2;
   }

   private int bx() {
      return this.winX() + 14;
   }

   private int rcolX() {
      return this.winX() + 14 + LCOL + 14;
   }

   private int rcolW() {
      return this.winW() - 318 - 14;
   }

   private int[] nameRect() {
      return new int[]{this.bx(), this.winY() + 50, LCOL, 16};
   }

   private int[] swatchRect(int i) {
      return new int[]{this.bx() + i * 17, this.winY() + 74, 15, 15};
   }

   private int[] resetRect() {
      return new int[]{this.bx() + 272, this.winY() + 74, 15, 15};
   }

   private int[] toggleRect(int i) {
      return new int[]{this.bx() + i * 26, this.winY() + 96, 22, 16};
   }

   private int[] countRect() {
      return new int[]{this.bx(), this.winY() + 128, 70, 16};
   }

   private int[] pickRect(int i) {
      return new int[]{this.bx() + i % 2 * 146, this.winY() + 154 + i / 2 * 22, 138, 16};
   }

   private int[] applyRect() {
      return new int[]{this.bx(), this.winY() + this.winH() - 14 - 20, 130, 18};
   }

   private int[] closeRect() {
      return new int[]{this.bx() + 140, this.winY() + this.winH() - 14 - 20, 100, 18};
   }

   private int[] rawRect() {
      return new int[]{this.rcolX(), this.winY() + 30, this.rcolW(), this.winH() - 30 - 14};
   }

   private int[] backRect() {
      return new int[]{this.rcolX(), this.winY() + 12, 30, 16};
   }

   private int[] searchRect() {
      return new int[]{this.rcolX() + 36, this.winY() + 12, this.rcolW() - 36, 16};
   }

   private int[] listRect() {
      return new int[]{this.rcolX(), this.winY() + 34, this.rcolW(), this.winH() - 34 - 14};
   }

   private static boolean hit(int[] r, int mx, int my) {
      return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      int accent = ThemeManager.accent();
      Render2DUtil.rect(ctx, 0, 0, this.width, this.height, 0x90000000);
      Render2DUtil.roundedRect(ctx, this.winX(), this.winY(), this.winW(), this.winH(), 10.0F, 0xF01E1E1E);
      Render2DUtil.roundedOutline(ctx, this.winX(), this.winY(), this.winW(), this.winH(), 10.0F, 1.0F, 0x40FFFFFF);
      int bx = this.bx();
      int y = this.winY();
      ctx.item(this.stack, bx, y + 12);
      Render2DUtil.msdfText(ctx, Translations.text("Item Editor"), bx + 22, y + 13, 9.0F, 0xFFEFEFEF);
      Render2DUtil.msdfText(ctx, clip(this.stack.getHoverName().getString(), 266), bx + 22, y + 24, 7.0F, 0xFF9A9A9A);
      Render2DUtil.msdfText(ctx, Translations.text("Название"), bx, y + 40, 7.5F, 0xFFEFEFEF);
      this.drawField(ctx, this.nameRect(), this.name);

      for (int i = 0; i < COLORS.length; i++) {
         int[] r = this.swatchRect(i);
         Render2DUtil.roundedRect(ctx, r[0], r[1], r[2], r[3], 2.0F, 0xFF000000 | COLORS[i]);
         if (this.nameColor == COLORS[i]) {
            Render2DUtil.roundedOutline(ctx, r[0], r[1], r[2], r[3], 2.0F, 1.5F, accent);
         }
      }

      int[] rs = this.resetRect();
      Render2DUtil.roundedRect(ctx, rs[0], rs[1], rs[2], rs[3], 2.0F, 0xCC15171C);
      Render2DUtil.msdfText(ctx, "×", rs[0] + 4.5F, rs[1] + 4.0F, 7.0F, this.nameColor < 0 ? accent : 0xFF9A9A9A);
      String[] tg = new String[]{"B", "I", "U", "S", "O"};
      boolean[] on = new boolean[]{this.bold, this.italic, this.underline, this.strike, this.obf};

      for (int ix = 0; ix < 5; ix++) {
         int[] r = this.toggleRect(ix);
         Render2DUtil.roundedRect(ctx, r[0], r[1], r[2], r[3], 3.0F, on[ix] ? ThemeManager.accentAlpha(128) : 0xCC15171C);
         Render2DUtil.roundedOutline(ctx, r[0], r[1], r[2], r[3], 3.0F, 1.0F, on[ix] ? accent : 0x40FFFFFF);
         float tw = Render2DUtil.msdfWidth(tg[ix], 8.0F);
         Render2DUtil.msdfText(ctx, tg[ix], r[0] + (r[2] - tw) / 2.0F, r[1] + (r[3] - 6) / 2.0F, 8.0F, 0xFFEFEFEF);
      }

      Render2DUtil.msdfText(ctx, Translations.text("Количество"), bx, y + 118, 7.5F, 0xFFEFEFEF);
      this.drawField(ctx, this.countRect(), this.count);
      String[] pk = new String[]{Translations.text("Зачары"), Translations.text("Эффекты"), Translations.text("Атрибуты"), Translations.text("Скрытие")};

      for (int ix = 0; ix < 4; ix++) {
         int[] r = this.pickRect(ix);
         boolean act = this.picker == ix + 1;
         Render2DUtil.roundedRect(ctx, r[0], r[1], r[2], r[3], 4.0F, act ? ThemeManager.accentAlpha(128) : 0xCC15171C);
         Render2DUtil.roundedOutline(ctx, r[0], r[1], r[2], r[3], 4.0F, 1.0F, act ? accent : 0x40FFFFFF);
         float tw = Render2DUtil.msdfWidth(pk[ix], 8.0F);
         Render2DUtil.msdfText(ctx, pk[ix], r[0] + (r[2] - tw) / 2.0F, r[1] + (r[3] - 6) / 2.0F, 8.0F, 0xFFEFEFEF);
      }

      if (this.picker == 0) {
         boolean rawEdited = !this.raw.text.equals(this.rawOriginal);
         Render2DUtil.msdfText(
            ctx,
            rawEdited ? Translations.text("Сырой NBT (изменён → заменит весь предмет)") : Translations.text("Сырой NBT (текущий — правь на месте)"),
            this.rcolX(),
            y + 16,
            7.5F,
            rawEdited ? 0xFFFFAA55 : 0xFFEFEFEF
         );
         this.drawArea(ctx, this.rawRect(), this.raw);
      } else {
         this.drawPicker(ctx, accent);
      }

      this.drawButton(ctx, this.applyRect(), Translations.text("Применить"), accent);
      this.drawButton(ctx, this.closeRect(), Translations.text("Закрыть"), 0x40FFFFFF);
      if (this.creativeSlot < 0) {
         Render2DUtil.msdfText(ctx, Translations.text("⚠ применение недоступно (не слот инвентаря игрока)"), bx, y + this.winH() - 14 - 30, 6.5F, 0xFFFFAA55);
      } else if (System.nanoTime() - this.flashAt < 1500000000L) {
         Render2DUtil.msdfText(ctx, this.flashMsg, bx, y + this.winH() - 14 - 30, 7.0F, this.flashColor);
      }
   }

   private void drawPicker(GuiGraphicsExtractor ctx, int accent) {
      this.drawButton(ctx, this.backRect(), "<", 0x40FFFFFF);
      int[] lr = this.listRect();
      if (this.picker == 4) {
         Render2DUtil.roundedRect(ctx, lr[0], lr[1], lr[2], lr[3], 4.0F, 0xCC15171C);
         Render2DUtil.roundedOutline(ctx, lr[0], lr[1], lr[2], lr[3], 4.0F, 1.0F, 0x40FFFFFF);
         Render2DUtil.msdfText(ctx, Translations.text("в разработке — следующим этапом"), lr[0] + 10, lr[1] + 10, 7.5F, 0xFF9A9A9A);
      } else {
         this.drawField(ctx, this.searchRect(), this.search);
         Render2DUtil.roundedRect(ctx, lr[0], lr[1], lr[2], lr[3], 4.0F, 0xCC15171C);
         Render2DUtil.roundedOutline(ctx, lr[0], lr[1], lr[2], lr[3], 4.0F, 1.0F, 0x40FFFFFF);
         List<Row> f = this.filteredRows();
         float maxScroll = Math.max(0, f.size() * 18 - (lr[3] - 8));
         this.listScroll = Math.max(0.0F, Math.min(this.listScroll, maxScroll));
         ctx.enableScissor(lr[0] + 1, lr[1] + 1, lr[0] + lr[2] - 1, lr[1] + lr[3] - 1);
         int rx = lr[0] + lr[2];

         for (int i = 0; i < f.size(); i++) {
            float ry = lr[1] + 4 + i * 18 - this.listScroll;
            if (!(ry + 18.0F < lr[1]) && !(ry > lr[1] + lr[3])) {
               Row r = f.get(i);
               if (r.active()) {
                  Render2DUtil.roundedRect(ctx, lr[0] + 2, (int)ry, lr[2] - 4, 17, 3.0F, ThemeManager.accentAlpha(34));
               }

               Render2DUtil.msdfText(ctx, clip(r.name(), lr[2] - 60), lr[0] + 6, ry + 6.0F, 8.0F, r.fits() ? 0xFFEFEFEF : 0xFF9A9A9A);
               Render2DUtil.roundedRect(ctx, rx - 46, (int)ry + 2, 14, 14, 3.0F, 0xF01E1E1E);
               Render2DUtil.msdfText(ctx, "-", rx - 41, ry + 5.0F, 8.0F, 0xFFEFEFEF);
               String vs = r.value();
               Render2DUtil.msdfText(ctx, vs, rx - 24 - Render2DUtil.msdfWidth(vs, 8.0F) / 2.0F, ry + 6.0F, 8.0F, r.active() ? accent : 0xFF9A9A9A);
               Render2DUtil.roundedRect(ctx, rx - 16, (int)ry + 2, 14, 14, 3.0F, 0xF01E1E1E);
               Render2DUtil.msdfText(ctx, "+", rx - 12.5F, ry + 5.0F, 8.0F, 0xFFEFEFEF);
            }
         }

         ctx.disableScissor();
      }
   }

   private void drawField(GuiGraphicsExtractor ctx, int[] r, Field f) {
      boolean foc = this.focused == f;
      Render2DUtil.roundedRect(ctx, r[0], r[1], r[2], r[3], 3.0F, 0xCC15171C);
      Render2DUtil.roundedOutline(ctx, r[0], r[1], r[2], r[3], 3.0F, 1.0F, foc ? ThemeManager.accent() : 0x40FFFFFF);
      int innerX = r[0] + 4;
      int innerW = r[2] - 8;
      float caretX = Render2DUtil.msdfWidth(f.text.substring(0, f.cursor), 8.0F);
      if (caretX - f.scroll > innerW) {
         f.scroll = caretX - innerW;
      }

      if (caretX - f.scroll < 0.0F) {
         f.scroll = caretX;
      }

      if (f.scroll < 0.0F) {
         f.scroll = 0.0F;
      }

      ctx.enableScissor(innerX, r[1], innerX + innerW, r[1] + r[3]);
      float tx = innerX - f.scroll;
      float ty = r[1] + (r[3] - 6) / 2.0F;
      if (foc && f.hasSel()) {
         float sx = tx + Render2DUtil.msdfWidth(f.text.substring(0, f.selStart()), 8.0F);
         float ex = tx + Render2DUtil.msdfWidth(f.text.substring(0, f.selEnd()), 8.0F);
         Render2DUtil.roundedRect(ctx, (int)sx, r[1] + 3, Math.max(1, (int)(ex - sx)), r[3] - 6, 0.0F, 0x803B6EA5);
      }

      Render2DUtil.msdfText(ctx, f.text, tx, ty, 8.0F, 0xFFEFEFEF);
      if (foc && System.currentTimeMillis() / 500L % 2L == 0L) {
         Render2DUtil.roundedRect(ctx, (int)(tx + caretX), r[1] + 3, 1, r[3] - 6, 0.0F, 0xFFEFEFEF);
      }

      ctx.disableScissor();
   }

   private void drawArea(GuiGraphicsExtractor ctx, int[] r, Area a) {
      boolean foc = this.focused == a;
      Render2DUtil.roundedRect(ctx, r[0], r[1], r[2], r[3], 4.0F, 0xCC15171C);
      Render2DUtil.roundedOutline(ctx, r[0], r[1], r[2], r[3], 4.0F, 1.0F, foc ? ThemeManager.accent() : 0x40FFFFFF);
      int innerX = r[0] + 5;
      int innerW = r[2] - 10;
      int top = r[1] + 4;
      int visLines = (r[3] - 8) / 11;
      int caretLine = a.lineOf(a.cursor);
      if (caretLine < a.scrollLine) {
         a.scrollLine = caretLine;
      }

      if (caretLine >= a.scrollLine + visLines) {
         a.scrollLine = caretLine - visLines + 1;
      }

      if (a.scrollLine < 0) {
         a.scrollLine = 0;
      }

      String[] lines = a.lines();
      ctx.enableScissor(innerX, r[1] + 1, innerX + innerW, r[1] + r[3] - 1);
      int selS = a.selStart();
      int selE = a.selEnd();

      for (int i = 0; i < visLines && a.scrollLine + i < lines.length; i++) {
         int li = a.scrollLine + i;
         String line = lines[li];
         float ly = top + i * 11;
         int ls = a.lineStart(li);
         int le = ls + line.length();
         if (foc && selE > ls && selS < le + 1) {
            int cs = Math.max(0, selS - ls);
            int ce = Math.min(line.length(), selE - ls);
            if (selE > le) {
               ce = line.length();
            }

            float hx = innerX + Render2DUtil.msdfWidth(line.substring(0, cs), 8.0F);
            float hw = Render2DUtil.msdfWidth(line.substring(cs, Math.max(cs, ce)), 8.0F);
            Render2DUtil.roundedRect(ctx, (int)hx, (int)ly - 1, Math.max(2, (int)hw), 11, 0.0F, 0x803B6EA5);
         }

         Render2DUtil.msdfText(ctx, line, innerX, ly, 8.0F, 0xFFEFEFEF);
      }

      if (foc && System.currentTimeMillis() / 500L % 2L == 0L) {
         int cl = caretLine - a.scrollLine;
         if (cl >= 0 && cl < visLines) {
            int col = a.cursor - a.lineStart(caretLine);
            float cx = innerX + Render2DUtil.msdfWidth(lines[caretLine].substring(0, col), 8.0F);
            Render2DUtil.roundedRect(ctx, (int)cx, top + cl * 11 - 1, 1, 11, 0.0F, 0xFFEFEFEF);
         }
      }

      ctx.disableScissor();
   }

   private void drawButton(GuiGraphicsExtractor ctx, int[] r, String label, int outline) {
      Render2DUtil.roundedRect(ctx, r[0], r[1], r[2], r[3], 4.0F, 0xCC15171C);
      Render2DUtil.roundedOutline(ctx, r[0], r[1], r[2], r[3], 4.0F, 1.0F, outline);
      float tw = Render2DUtil.msdfWidth(label, 8.0F);
      Render2DUtil.msdfText(ctx, label, r[0] + (r[2] - tw) / 2.0F, r[1] + (r[3] - 6) / 2.0F, 8.0F, 0xFFEFEFEF);
   }

   private static String clip(String s, int maxW) {
      if (Render2DUtil.msdfWidth(s, 7.0F) <= maxW) {
         return s;
      }

      while (s.length() > 1 && Render2DUtil.msdfWidth(s + "…", 7.0F) > maxW) {
         s = s.substring(0, s.length() - 1);
      }

      return s + "…";
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
      int mx = (int)e.x();
      int my = (int)e.y();
      this.focused = null;
      if (hit(this.nameRect(), mx, my)) {
         this.focused = this.name;
         this.placeCaret(this.name, this.nameRect(), mx);
         return true;
      }

      if (hit(this.countRect(), mx, my)) {
         this.focused = this.count;
         this.placeCaret(this.count, this.countRect(), mx);
         return true;
      }

      for (int i = 0; i < COLORS.length; i++) {
         if (!hit(this.swatchRect(i), mx, my)) continue;

         this.nameColor = COLORS[i];
         return true;
      }

      if (hit(this.resetRect(), mx, my)) {
         this.nameColor = -1;
         return true;
      }

      for (int ix = 0; ix < 5; ix++) {
         if (!hit(this.toggleRect(ix), mx, my)) continue;

         this.toggleFormat(ix);
         return true;
      }

      for (int ixx = 0; ixx < 4; ixx++) {
         if (!hit(this.pickRect(ixx), mx, my)) continue;

         this.picker = this.picker == ixx + 1 ? 0 : ixx + 1;
         this.listScroll = 0.0F;
         this.search.set("");
         return true;
      }

      if (hit(this.applyRect(), mx, my)) {
         this.apply();
         return true;
      }

      if (hit(this.closeRect(), mx, my)) {
         this.onClose();
         return true;
      }

      if (this.picker == 0) {
         if (hit(this.rawRect(), mx, my)) {
            this.focused = this.raw;
            this.placeCaretArea(this.raw, this.rawRect(), mx, my);
            return true;
         }
      } else {
         if (hit(this.backRect(), mx, my)) {
            this.picker = 0;
            return true;
         }

         if (this.picker >= 1 && this.picker <= 3) {
            if (hit(this.searchRect(), mx, my)) {
               this.focused = this.search;
               this.placeCaret(this.search, this.searchRect(), mx);
               return true;
            }

            int[] lr = this.listRect();
            if (hit(lr, mx, my)) {
               this.clickList(mx, my, lr);
               return true;
            }
         }
      }

      return super.mouseClicked(e, dbl);
   }

   private void clickList(int mx, int my, int[] lr) {
      List<Row> f = this.filteredRows();
      int idx = (int)((my - (lr[1] + 4) + this.listScroll) / 18.0F);
      if (idx >= 0 && idx < f.size()) {
         Row r = f.get(idx);
         int rx = lr[0] + lr[2];
         if (mx >= rx - 46 && mx <= rx - 32) {
            r.dec().run();
         } else if (mx >= rx - 16 && mx <= rx - 2) {
            r.inc().run();
         } else {
            r.toggle().run();
         }
      }
   }

   private void placeCaret(Field f, int[] r, int mx) {
      f.cursor = nearestCol(f.text, mx - (r[0] + 4) + f.scroll);
      f.sel = f.cursor;
   }

   private void placeCaretArea(Area a, int[] r, int mx, int my) {
      int innerX = r[0] + 5;
      int top = r[1] + 4;
      String[] lines = a.lines();
      int li = a.scrollLine + Math.max(0, (my - top) / 11);
      li = Math.max(0, Math.min(li, lines.length - 1));
      a.cursor = a.lineStart(li) + nearestCol(lines[li], mx - innerX);
      a.sel = a.cursor;
   }

   private static int nearestCol(String line, float localX) {
      int best = 0;
      float bestD = Float.MAX_VALUE;

      for (int i = 0; i <= line.length(); i++) {
         float d = Math.abs(Render2DUtil.msdfWidth(line.substring(0, i), 8.0F) - localX);
         if (d < bestD) {
            bestD = d;
            best = i;
         }
      }

      return best;
   }

   private void toggleFormat(int i) {
      switch (i) {
         case 0:
            this.bold = !this.bold;
            break;
         case 1:
            this.italic = !this.italic;
            break;
         case 2:
            this.underline = !this.underline;
            break;
         case 3:
            this.strike = !this.strike;
            break;
         case 4:
            this.obf = !this.obf;
      }
   }

   public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
      if (this.picker >= 1 && this.picker <= 3 && hit(this.listRect(), (int)mx, (int)my)) {
         this.listScroll = (float)(this.listScroll - vertical * 18.0);
         return true;
      }

      return super.mouseScrolled(mx, my, horizontal, vertical);
   }

   public boolean keyPressed(KeyEvent e) {
      int k = e.key();
      if (this.focused != null) {
         boolean ctrl = (e.modifiers() & 2) != 0;
         boolean shift = (e.modifiers() & 1) != 0;
         if (ctrl) {
            if (k == 65) {
               this.focused.selectAll();
               return true;
            }

            if (k == 67) {
               this.copy(this.focused);
               return true;
            }

            if (k == 88) {
               this.copy(this.focused);
               if (this.focused.hasSel()) {
                  this.focused.deleteSel();
               } else {
                  this.focused.clearAll();
               }

               return true;
            }

            if (k == 86) {
               this.focused.insert(mc.keyboardHandler.getClipboard());
               return true;
            }
         }

         if (k == 259) {
            this.focused.backspace();
         } else if (k == 261) {
            this.focused.delete();
         } else if (k == 263) {
            this.focused.left(shift);
         } else if (k == 262) {
            this.focused.right(shift);
         } else if (k == 265) {
            this.focused.up(shift);
         } else if (k == 264) {
            this.focused.down(shift);
         } else if (k == 268) {
            this.focused.home(shift);
         } else if (k == 269) {
            this.focused.end(shift);
         } else if (k == 257) {
            if (this.focused == this.raw) {
               this.raw.insert("\n");
            } else {
               this.focused = null;
            }
         } else if (k == 256) {
            this.focused = null;
         }

         return true;
      }

      if (k == 256 && this.picker != 0) {
         this.picker = 0;
         return true;
      }

      return super.keyPressed(e);
   }

   public boolean charTyped(CharacterEvent e) {
      if (this.focused == null) {
         return super.charTyped(e);
      }

      this.focused.insert(e.codepointAsString());
      return true;
   }

   private void copy(Edit f) {
      String s = f.hasSel() ? f.selected() : "";
      if (s.isEmpty() && f instanceof Field fl) {
         s = fl.text;
      }

      if (s.isEmpty() && f instanceof Area a) {
         s = a.text;
      }

      if (!s.isEmpty()) {
         mc.keyboardHandler.setClipboard(s);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      Minecraft.getInstance().setScreenAndShow(null);
   }

   private static final class Area implements Edit {
      String text = "";
      int cursor = 0;
      int sel = 0;
      int scrollLine = 0;

      void set(String t) {
         this.text = t == null ? "" : t;
         this.cursor = Math.min(this.cursor, this.text.length());
         this.sel = this.cursor;
         this.scrollLine = 0;
      }

      String[] lines() {
         return this.text.split("\n", -1);
      }

      int lineOf(int idx) {
         int l = 0;

         for (int i = 0; i < idx && i < this.text.length(); i++) {
            if (this.text.charAt(i) != '\n') continue;

            l++;
         }

         return l;
      }

      int lineStart(int line) {
         if (line <= 0) {
            return 0;
         }

         int i = 0;

         for (int l = 0; i < this.text.length(); i++) {
            if (this.text.charAt(i) != '\n') continue;

            if (++l == line) {
               return i + 1;
            }
         }

         return this.text.length();
      }

      int lineEndFrom(int ls) {
         int nl = this.text.indexOf(10, ls);
         return nl < 0 ? this.text.length() : nl;
      }

      @Override
      public boolean hasSel() {
         return this.sel != this.cursor;
      }

      int selStart() {
         return Math.min(this.sel, this.cursor);
      }

      int selEnd() {
         return Math.max(this.sel, this.cursor);
      }

      @Override
      public String selected() {
         return this.text.substring(this.selStart(), this.selEnd());
      }

      @Override
      public void deleteSel() {
         int s = this.selStart();
         int e = this.selEnd();
         this.text = this.text.substring(0, s) + this.text.substring(e);
         this.cursor = s;
         this.sel = s;
      }

      @Override
      public void insert(String in) {
         if (in == null || in.isEmpty()) return;

         if (this.hasSel()) {
            this.deleteSel();
         }

         this.text = this.text.substring(0, this.cursor) + in + this.text.substring(this.cursor);
         this.cursor = this.cursor + in.length();
         this.sel = this.cursor;
      }

      @Override
      public void backspace() {
         if (this.hasSel()) {
            this.deleteSel();
         } else {
            if (this.cursor > 0) {
               this.text = this.text.substring(0, this.cursor - 1) + this.text.substring(this.cursor);
               this.cursor--;
               this.sel = this.cursor;
            }
         }
      }

      @Override
      public void delete() {
         if (this.hasSel()) {
            this.deleteSel();
         } else {
            if (this.cursor < this.text.length()) {
               this.text = this.text.substring(0, this.cursor) + this.text.substring(this.cursor + 1);
            }
         }
      }

      @Override
      public void left(boolean shift) {
         if (!shift && this.hasSel()) {
            this.cursor = this.selStart();
         } else if (this.cursor > 0) {
            this.cursor--;
         }

         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void right(boolean shift) {
         if (!shift && this.hasSel()) {
            this.cursor = this.selEnd();
         } else if (this.cursor < this.text.length()) {
            this.cursor++;
         }

         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void up(boolean shift) {
         int line = this.lineOf(this.cursor);
         if (line > 0) {
            int col = this.cursor - this.lineStart(line);
            int ps = this.lineStart(line - 1);
            int pe = this.lineStart(line) - 1;
            this.cursor = ps + Math.min(col, Math.max(0, pe - ps));
         }

         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void down(boolean shift) {
         int line = this.lineOf(this.cursor);
         int total = this.lines().length;
         if (line < total - 1) {
            int col = this.cursor - this.lineStart(line);
            int ns = this.lineStart(line + 1);
            int ne = this.lineEndFrom(ns);
            this.cursor = ns + Math.min(col, ne - ns);
         }

         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void home(boolean shift) {
         this.cursor = this.lineStart(this.lineOf(this.cursor));
         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void end(boolean shift) {
         this.cursor = this.lineEndFrom(this.lineStart(this.lineOf(this.cursor)));
         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void selectAll() {
         this.sel = 0;
         this.cursor = this.text.length();
      }

      @Override
      public void clearAll() {
         this.text = "";
         this.cursor = 0;
         this.sel = 0;
      }
   }

   private record Attr(Holder<Attribute> holder, String name, String id) {
   }

   private interface Edit {
      void insert(String text);

      void backspace();

      void delete();

      void left(boolean select);

      void right(boolean select);

      void up(boolean select);

      void down(boolean select);

      void home(boolean select);

      void end(boolean select);

      void selectAll();

      boolean hasSel();

      String selected();

      void deleteSel();

      void clearAll();
   }

   private record Eff(Holder<MobEffect> holder, String name, String id) {
   }

   private record Ench(Holder<Enchantment> holder, String name, String id, int max, boolean fits) {
   }

   private static final class Field implements Edit {
      String text = "";
      int cursor = 0;
      int sel = 0;
      float scroll = 0.0F;
      final int maxLen;
      final boolean digits;

      Field(int maxLen, boolean digits) {
         this.maxLen = maxLen;
         this.digits = digits;
      }

      void set(String t) {
         this.text = t == null ? "" : t;
         this.cursor = Math.min(this.cursor, this.text.length());
         this.sel = this.cursor;
      }

      @Override
      public boolean hasSel() {
         return this.sel != this.cursor;
      }

      int selStart() {
         return Math.min(this.sel, this.cursor);
      }

      int selEnd() {
         return Math.max(this.sel, this.cursor);
      }

      @Override
      public String selected() {
         return this.text.substring(this.selStart(), this.selEnd());
      }

      @Override
      public void deleteSel() {
         int s = this.selStart();
         int e = this.selEnd();
         this.text = this.text.substring(0, s) + this.text.substring(e);
         this.cursor = s;
         this.sel = s;
      }

      @Override
      public void insert(String in) {
         if (in == null || in.isEmpty()) return;

         if (this.digits) {
            in = in.replaceAll("[^0-9]", "");
         }

         in = in.replace("\n", "");
         if (this.hasSel()) {
            this.deleteSel();
         }

         if (this.maxLen > 0 && this.text.length() + in.length() > this.maxLen) {
            int room = this.maxLen - this.text.length();
            if (room <= 0) {
               return;
            }

            in = in.substring(0, room);
         }

         this.text = this.text.substring(0, this.cursor) + in + this.text.substring(this.cursor);
         this.cursor = this.cursor + in.length();
         this.sel = this.cursor;
      }

      @Override
      public void backspace() {
         if (this.hasSel()) {
            this.deleteSel();
         } else {
            if (this.cursor > 0) {
               this.text = this.text.substring(0, this.cursor - 1) + this.text.substring(this.cursor);
               this.cursor--;
               this.sel = this.cursor;
            }
         }
      }

      @Override
      public void delete() {
         if (this.hasSel()) {
            this.deleteSel();
         } else {
            if (this.cursor < this.text.length()) {
               this.text = this.text.substring(0, this.cursor) + this.text.substring(this.cursor + 1);
            }
         }
      }

      @Override
      public void left(boolean shift) {
         if (!shift && this.hasSel()) {
            this.cursor = this.selStart();
         } else if (this.cursor > 0) {
            this.cursor--;
         }

         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void right(boolean shift) {
         if (!shift && this.hasSel()) {
            this.cursor = this.selEnd();
         } else if (this.cursor < this.text.length()) {
            this.cursor++;
         }

         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void up(boolean shift) {
         this.home(shift);
      }

      @Override
      public void down(boolean shift) {
         this.end(shift);
      }

      @Override
      public void home(boolean shift) {
         this.cursor = 0;
         if (!shift) {
            this.sel = 0;
         }
      }

      @Override
      public void end(boolean shift) {
         this.cursor = this.text.length();
         if (!shift) {
            this.sel = this.cursor;
         }
      }

      @Override
      public void selectAll() {
         this.sel = 0;
         this.cursor = this.text.length();
      }

      @Override
      public void clearAll() {
         this.text = "";
         this.cursor = 0;
         this.sel = 0;
      }
   }

   private record Row(String name, boolean fits, String value, boolean active, Runnable dec, Runnable inc, Runnable toggle) {
   }
}
