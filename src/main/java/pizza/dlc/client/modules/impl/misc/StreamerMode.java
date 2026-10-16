package pizza.dlc.client.modules.impl.misc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.StringSetting;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "StreamerMode",
   description = "Прячет твой ник везде (таб/чат/скорборд/тайтлы)",
   category = ModuleCategory.OTHER
)
public class StreamerMode extends Module {
   public final StringSetting replacement = new StringSetting("Замена", "На что менять ник", "Protected", 32, this);
   public final BooleanSetting hideFriends = new BooleanSetting("Прятать друзей", "Прятать и ники из списка друзей", this);

   public StreamerMode() {
      this.hideFriends.setEnabled(true);
   }

   public static String replace(String s) {
      StreamerMode m = active();
      if (m != null && s != null && !s.isEmpty()) {
         List<String> names = m.names();
         if (!containsAny(s, names)) {
            return s;
         }

         String r = m.repl();

         for (String n : names) {
            s = replaceWord(s, n, r);
         }

         return s;
      }

      return s;
   }

   public static Component replace(Component c) {
      StreamerMode m = active();
      if (m != null && c != null) {
         List<String> names = m.names();
         return !containsAny(c.getString(), names) ? c : rebuild(c, names, m.repl());
      }

      return c;
   }

   private static Component rebuild(Component c, List<String> names, String repl) {
      ComponentContents contents = c.getContents();
      MutableComponent out;
      if (contents instanceof PlainTextContents ptc) {
         String t = ptc.text();

         for (String n : names) {
            t = replaceWord(t, n, repl);
         }

         out = Component.literal(t);
      } else if (contents instanceof TranslatableContents tc) {
         Object[] args = tc.getArgs();
         Object[] newArgs = new Object[args.length];

         for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a instanceof Component ac) {
               newArgs[i] = rebuild(ac, names, repl);
            } else if (!(a instanceof String s)) {
               newArgs[i] = a;
            } else {
               for (String n : names) {
                  s = replaceWord(s, n, repl);
               }

               newArgs[i] = s;
            }
         }

         out = MutableComponent.create(new TranslatableContents(tc.getKey(), tc.getFallback(), newArgs));
      } else {
         out = MutableComponent.create(contents);
      }

      out.setStyle(c.getStyle());

      for (Component sib : c.getSiblings()) {
         out.append(rebuild(sib, names, repl));
      }

      return out;
   }

   private static String replaceWord(String s, String word, String repl) {
      if (word != null && !word.isEmpty()) {
         int i = 0;

         while ((i = s.indexOf(word, i)) >= 0) {
            int end = i + word.length();
            boolean leftOk = i == 0 || !isNameChar(s.charAt(i - 1));
            boolean rightOk = end >= s.length() || !isNameChar(s.charAt(end));
            if (leftOk && rightOk) {
               s = s.substring(0, i) + repl + s.substring(end);
               i += repl.length();
            } else {
               i = end;
            }
         }

         return s;
      }

      return s;
   }

   private static boolean isNameChar(char c) {
      return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_';
   }

   private static boolean containsAny(String s, List<String> names) {
      for (String n : names) {
         if (!containsWord(s, n)) continue;

         return true;
      }

      return false;
   }

   private static boolean containsWord(String s, String word) {
      if (word != null && !word.isEmpty()) {
         int i = 0;

         while ((i = s.indexOf(word, i)) >= 0) {
            int end = i + word.length();
            boolean leftOk = i == 0 || !isNameChar(s.charAt(i - 1));
            boolean rightOk = end >= s.length() || !isNameChar(s.charAt(end));
            if (leftOk && rightOk) {
               return true;
            }

            i = end;
         }

         return false;
      }

      return false;
   }

   private List<String> names() {
      List<String> out = new ArrayList<>();
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         out.add(mc.player.getGameProfile().name());
      }

      if (this.hideFriends.isEnabled()) {
         try {
            out.addAll(PizzaDLC.getInstance().getFriendManager().getItems());
         } catch (Exception ignored) {
         }
      }

      return out;
   }

   private String repl() {
      String t = this.replacement.getText();
      return t != null && !t.isEmpty() ? t : "Protected";
   }

   private static StreamerMode active() {
      StreamerMode m = get();
      return m != null && m.isEnabled() && Minecraft.getInstance().player != null ? m : null;
   }

   public static boolean isActive() {
      return active() != null;
   }

   private static StreamerMode get() {
      PizzaDLCClient client = PizzaDLCClient.getInstance();
      return client != null && client.getModuleManager() != null ? client.getModuleManager().get(StreamerMode.class) : null;
   }
}
