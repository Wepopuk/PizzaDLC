package pizza.dlc.api.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.theme.ThemeManager;

public final class ItemNbtViewer implements QuickImports {
   private static final int WINDOW = 10;
   private static final long AUTO_INTERVAL = 650000000L;
   private static final long AUTO_PAUSE = 2500000000L;
   private static final long COPY_FLASH = 1200000000L;
   private static int activeSig = Integer.MIN_VALUE;
   private static List<String> lines = List.of();
   private static List<String> logicalLines = List.of();
   private static String fullText = "";
   private static int cursor = 0;
   private static long lastInput = 0L;
   private static long lastAuto = 0L;
   private static long copiedAt = 0L;
   private static String copiedMsg = "";

   private ItemNbtViewer() {
   }

   public static boolean onArrowKey(int key) {
      List<String> snap = lines;
      List<String> logic = logicalLines;
      int n = snap.size();
      if (n == 0) {
         return false;
      }

      long now = System.nanoTime();
      if (key == 265) {
         cursor = Math.max(0, cursor - 1);
         lastInput = now;
         return true;
      }

      if (key == 264) {
         cursor = Math.min(n - 1, cursor + 1);
         lastInput = now;
         return true;
      }

      if (key == 263) {
         if (logic.isEmpty()) {
            return true;
         }

         cursor = Math.max(0, Math.min(cursor, n - 1));
         mc.keyboardHandler.setClipboard(logic.get(Math.min(cursor, logic.size() - 1)).strip());
         copiedMsg = "строка скопирована";
         copiedAt = now;
         lastInput = now;
         return true;
      }

      if (key == 262) {
         mc.keyboardHandler.setClipboard(fullText);
         copiedMsg = "NBT скопирован";
         copiedAt = now;
         lastInput = now;
         return true;
      }

      return false;
   }

   public static List<Component> appendNbt(ItemStack stack, List<Component> original) {
      if (stack != null && !stack.isEmpty() && mc.level != null && mc.gui.screen() != null) {
         if (!mc.isSameThread()) {
            return original;
         }

         int sig = signature(stack);
         if (sig != activeSig) {
            activeSig = sig;
            rebuild(stack);
            long now = System.nanoTime();
            cursor = 0;
            lastInput = now;
            lastAuto = now;
            copiedAt = 0L;
         }

         List<String> snap = lines;
         int n = snap.size();
         if (n == 0) {
            return original;
         }

         long now = System.nanoTime();
         if (now - lastInput > AUTO_PAUSE && now - lastAuto > AUTO_INTERVAL) {
            cursor = (cursor + 1) % n;
            lastAuto = now;
         }

         cursor = Math.max(0, Math.min(cursor, n - 1));
         int top = n <= WINDOW ? 0 : Math.max(0, Math.min(cursor - 5, n - WINDOW));
         int bot = Math.min(n, top + WINDOW);
         int accent = ThemeManager.accent() & 0xFFFFFF;
         boolean flash = now - copiedAt < COPY_FLASH;
         List<Component> out = new ArrayList<>(original);
         out.add(Component.empty());
         out.add(
            Component.literal("NBT  " + (cursor + 1) + " / " + n + "    " + (flash ? "✓ " + copiedMsg : "↑↓ листать   ← строка   → всё"))
               .withColor(flash ? 0xFF7BE0A0 : accent)
         );

         for (int i = top; i < bot; i++) {
            String line = snap.get(i);
            if (i == cursor) {
               MutableComponent comp = Component.literal("» ").withColor(accent);
               comp.append(Component.literal(line).withColor(-1));
               out.add(comp);
            } else {
               out.add(Component.literal("  " + line).withColor(0xFFB6BECE));
            }
         }

         return out;
      }

      return original;
   }

   private static void rebuild(ItemStack stack) {
      try {
         RegistryOps<Tag> ops = mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
         Tag tag = (Tag)ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);
         if (tag == null) {
            lines = logicalLines = List.of("<нет данных>");
            fullText = "";
            return;
         }

         fullText = new SnbtPrinterTagVisitor().visit(tag);
         List<String> disp = new ArrayList<>();
         List<String> logic = new ArrayList<>();

         for (String l : fullText.split("\n")) {
            wrap(disp, logic, l);
         }

         lines = disp;
         logicalLines = logic;
      } catch (Throwable ex) {
         lines = logicalLines = List.of("<ошибка: " + ex.getClass().getSimpleName() + ">");
         fullText = "";
      }
   }

   private static void wrap(List<String> disp, List<String> logic, String line) {
      if (line.length() <= 60) {
         disp.add(line);
         logic.add(line);
      } else {
         int indent = 0;

         while (indent < line.length() && line.charAt(indent) == ' ') {
            indent++;
         }

         String pad = " ".repeat(Math.min(indent + 2, 52));
         disp.add(line.substring(0, 60));
         logic.add(line);
         int i = 60;

         while (i < line.length()) {
            int end = Math.min(line.length(), i + (60 - pad.length()));
            disp.add(pad + line.substring(i, end));
            logic.add(line);
            i = end;
         }
      }
   }

   private static int signature(ItemStack stack) {
      return stack.getItem().hashCode() * 31 + stack.getComponentsPatch().hashCode() + stack.getCount();
   }
}
