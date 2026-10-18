package pizza.dlc.client.modules.impl.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
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
      try {
         StreamerMode m = active();
         if (m == null || s == null || s.isEmpty()) {
            return s;
         }
         List<String> names = m.names();
         if (names.isEmpty()) {
            return s;
         }
         return StreamerNameFilter.replace(s, names, m.repl());
      } catch (Throwable ignored) {
         return s;
      }
   }

   public static Component replace(Component c) {
      try {
         StreamerMode m = active();
         if (m == null || c == null) {
            return c;
         }
         List<String> names = m.names();
         if (names.isEmpty()) {
            return c;
         }
         List<int[]> norms = StreamerNameFilter.nameNorms(names);
         if (norms.isEmpty()) {
            return c;
         }
         return replaceComponent(c, norms, m.repl());
      } catch (Throwable ignored) {
         return c;
      }
   }

   public static FormattedCharSequence replace(FormattedCharSequence seq) {
      try {
         StreamerMode m = active();
         if (m == null || seq == null) {
            return seq;
         }
         List<String> names = m.names();
         if (names.isEmpty()) {
            return seq;
         }
         List<int[]> norms = StreamerNameFilter.nameNorms(names);
         if (norms.isEmpty()) {
            return seq;
         }
         return replaceSequence(seq, norms, m.repl());
      } catch (Throwable ignored) {
         return seq;
      }
   }

   /** Same as {@link #replace(Component)} but with precomputed norms (avoids recompute). */
   static Component replaceComponent(Component c, List<int[]> norms, String repl) {
      List<Style> styles = new ArrayList<>();
      List<String> texts = new ArrayList<>();
      StringBuilder plain = new StringBuilder();
      c.visit((style, str) -> {
         if (str != null && !str.isEmpty()) {
            styles.add(style);
            texts.add(str);
            plain.append(str);
         }
         return Optional.empty();
      }, Style.EMPTY);
      String full = plain.toString();
      if (full.isEmpty()) {
         return c;
      }
      List<int[]> matches = StreamerNameFilter.findMatches(full, norms);
      if (matches.isEmpty()) {
         return c;
      }
      int[] starts = new int[texts.size()];
      int acc = 0;
      for (int i = 0; i < texts.size(); i++) {
         starts[i] = acc;
         acc += texts.get(i).length();
      }
      MutableComponent out = Component.empty();
      boolean[] done = new boolean[matches.size()];
      for (int r = 0; r < texts.size(); r++) {
         String t = texts.get(r);
         Style st = styles.get(r);
         int rs = starts[r];
         int re = rs + t.length();
         int p = rs;
         for (int k = 0; k < matches.size(); k++) {
            int ms = matches.get(k)[0];
            int me = matches.get(k)[1];
            if (me <= rs || ms >= re) {
               continue;
            }
            int cs = Math.max(ms, rs);
            int ce = Math.min(me, re);
            if (cs > p) {
               out.append(Component.literal(t.substring(p - rs, cs - rs)).withStyle(st));
            }
            if (!done[k] && ms >= rs && ms < re) {
               out.append(Component.literal(repl).withStyle(st));
               done[k] = true;
            }
            if (ce > p) {
               p = ce;
            }
         }
         if (p < re) {
            out.append(Component.literal(t.substring(p - rs)).withStyle(st));
         }
      }
      return out;
   }

   private static FormattedCharSequence replaceSequence(FormattedCharSequence seq, List<int[]> norms, String repl) {
      List<Style> styles = new ArrayList<>();
      List<StringBuilder> texts = new ArrayList<>();
      StringBuilder plain = new StringBuilder();
      boolean[] any = new boolean[1];
      seq.accept((idx, style, cp) -> {
         int n = texts.size();
         if (n > 0 && styles.get(n - 1).equals(style)) {
            texts.get(n - 1).appendCodePoint(cp);
         } else {
            styles.add(style);
            StringBuilder sb = new StringBuilder();
            sb.appendCodePoint(cp);
            texts.add(sb);
         }
         plain.appendCodePoint(cp);
         any[0] = true;
         return true;
      });
      if (!any[0]) {
         return seq;
      }
      String full = plain.toString();
      List<int[]> matches = StreamerNameFilter.findMatches(full, norms);
      if (matches.isEmpty()) {
         return seq;
      }
      int[] starts = new int[texts.size()];
      int acc = 0;
      for (int i = 0; i < texts.size(); i++) {
         starts[i] = acc;
         acc += texts.get(i).length();
      }
      List<FormattedCharSequence> parts = new ArrayList<>();
      boolean[] done = new boolean[matches.size()];
      for (int r = 0; r < texts.size(); r++) {
         String t = texts.get(r).toString();
         Style st = styles.get(r);
         int rs = starts[r];
         int re = rs + t.length();
         int p = rs;
         for (int k = 0; k < matches.size(); k++) {
            int ms = matches.get(k)[0];
            int me = matches.get(k)[1];
            if (me <= rs || ms >= re) {
               continue;
            }
            int cs = Math.max(ms, rs);
            int ce = Math.min(me, re);
            if (cs > p) {
               parts.add(FormattedCharSequence.forward(t.substring(p - rs, cs - rs), st));
            }
            if (!done[k] && ms >= rs && ms < re) {
               parts.add(FormattedCharSequence.forward(repl, st));
               done[k] = true;
            }
            if (ce > p) {
               p = ce;
            }
         }
         if (p < re) {
            parts.add(FormattedCharSequence.forward(t.substring(p - rs), st));
         }
      }
      if (parts.isEmpty()) {
         return seq;
      }
      return parts.size() == 1 ? parts.get(0) : FormattedCharSequence.composite(parts);
   }

   private List<String> names() {
      List<String> out = new ArrayList<>();
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         String name = mc.player.getGameProfile().name();
         if (name != null && !name.isEmpty()) {
            out.add(name);
         }
      }

      if (this.hideFriends.isEnabled()) {
         try {
            for (String f : PizzaDLC.getInstance().getFriendManager().getItems()) {
               if (f != null && !f.isEmpty() && !out.contains(f)) {
                  out.add(f);
               }
            }
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
