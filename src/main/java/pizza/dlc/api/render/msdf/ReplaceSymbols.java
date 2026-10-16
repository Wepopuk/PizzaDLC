package pizza.dlc.api.render.msdf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ReplaceSymbols {
   private static final Map<Integer, String> REPLACEMENTS = new LinkedHashMap<>();
   private static final Map<Integer, Integer> RANK_COLORS = new LinkedHashMap<>();
   private static final GlyphReplacementManager USER_RULES = new GlyphReplacementManager();

   private ReplaceSymbols() {
   }

   private static int argb(int r, int g, int b) {
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public static String replacement(int cp) {
      GlyphReplacementManager.Replacement r = USER_RULES.getReplacement(currentHost(), cp);
      return r != null ? r.text() : REPLACEMENTS.get(cp);
   }

   public static int rankColor(int cp) {
      GlyphReplacementManager.Replacement r = USER_RULES.getReplacement(currentHost(), cp);
      return r != null && r.color() != 0 ? r.color() : RANK_COLORS.getOrDefault(cp, 0);
   }

   public static RankMatch recoverRanksFromText(String decoded) {
      if (decoded != null && !decoded.isEmpty()) {
         GlyphReplacementManager.RuleSet rules = USER_RULES.rulesFor(currentHost());
         Set<Integer> cps = new LinkedHashSet<>(REPLACEMENTS.keySet());
         if (rules.host() != null) {
            cps.addAll(rules.host().keySet());
         }

         if (rules.global() != null) {
            cps.addAll(rules.global().keySet());
         }

         Map<String, Integer> wordColor = new LinkedHashMap<>();

         for (int cp : cps) {
            GlyphReplacementManager.Replacement r = rules.get(cp);
            String text = r != null ? r.text() : REPLACEMENTS.get(cp);
            if (text != null && text.length() > 1) {
               int color = r != null && r.color() != 0 ? r.color() : RANK_COLORS.getOrDefault(cp, 0);
               wordColor.putIfAbsent(text.toUpperCase(Locale.ROOT), color);
            }
         }

         if (wordColor.isEmpty()) {
            return null;
         }

         List<String> words = new ArrayList<>(wordColor.keySet());
         words.sort((a, b) -> b.length() - a.length());
         String up = decoded.toUpperCase(Locale.ROOT);
         boolean[] used = new boolean[up.length()];
         List<int[]> hits = new ArrayList<>();

         for (int wi = 0; wi < words.size(); wi++) {
            String w = words.get(wi);
            if (!w.isEmpty()) {
               for (int idx = up.indexOf(w); idx >= 0; idx = up.indexOf(w, idx + 1)) {
                  boolean free = true;

                  for (int k = idx; k < idx + w.length(); k++) {
                     if (!used[k]) continue;

                     free = false;
                     break;
                  }

                  if (free) {
                     for (int kx = idx; kx < idx + w.length(); kx++) {
                        used[kx] = true;
                     }

                     hits.add(new int[]{idx, wi});
                  }
               }
            }
         }

         if (hits.isEmpty()) {
            return null;
         }

         hits.sort((a, b) -> Integer.compare(a[0], b[0]));
         StringBuilder sb = new StringBuilder();

         for (int[] h : hits) {
            if (sb.length() > 0) {
               sb.append(' ');
            }

            sb.append(words.get(h[1]));
         }

         int color = wordColor.getOrDefault(words.get(hits.get(0)[1]), 0);
         return new RankMatch(sb.toString(), color);
      }

      return null;
   }

   public static String normalize(String s) {
      if (s != null && !s.isEmpty()) {
         GlyphReplacementManager.RuleSet rules = USER_RULES.rulesFor(currentHost());
         boolean any = false;
         int i = 0;

         while (i < s.length()) {
            int cp = s.codePointAt(i);
            if (REPLACEMENTS.containsKey(cp) || rules.get(cp) != null) {
               any = true;
               break;
            }

            i += Character.charCount(cp);
         }

         if (!any) {
            return s;
         }

         StringBuilder out = new StringBuilder(s.length());
         int ix = 0;

         while (ix < s.length()) {
            int cp = s.codePointAt(ix);
            GlyphReplacementManager.Replacement r = rules.get(cp);
            String repl = r != null ? r.text() : REPLACEMENTS.get(cp);
            if (repl != null) {
               out.append(repl);
            } else {
               out.appendCodePoint(cp);
            }

            ix += Character.charCount(cp);
         }

         return out.toString();
      }

      return s;
   }

   private static String currentHost() {
      try {
         Minecraft mc = Minecraft.getInstance();
         ServerData sd = mc.getCurrentServer();
         if (sd == null && mc.getConnection() != null) {
            sd = mc.getConnection().getServerData();
         }

         return sd != null ? sd.ip : null;
      } catch (Exception ignored) {
         return null;
      }
   }

   static Map<Integer, GlyphReplacementManager.Replacement> builtinDefaults() {
      Map<Integer, GlyphReplacementManager.Replacement> defaults = new LinkedHashMap<>();

      for (Entry<Integer, String> e : REPLACEMENTS.entrySet()) {
         int color = RANK_COLORS.getOrDefault(e.getKey(), 0);
         defaults.put(e.getKey(), new GlyphReplacementManager.Replacement(e.getValue(), color));
      }

      return defaults;
   }

   static {
      REPLACEMENTS.put(9889, "");
      REPLACEMENTS.put(9733, "");
      REPLACEMENTS.put(42240, "PLAYER");
      REPLACEMENTS.put(42244, "HERO");
      REPLACEMENTS.put(42248, "TITAN");
      REPLACEMENTS.put(42258, "AVENGER");
      REPLACEMENTS.put(42262, "OVERLORD");
      REPLACEMENTS.put(42272, "MAGISTER");
      REPLACEMENTS.put(42276, "IMPERATOR");
      REPLACEMENTS.put(42280, "DRAGON");
      REPLACEMENTS.put(42336, "D.HELPER");
      REPLACEMENTS.put(42290, "BULL");
      REPLACEMENTS.put(42294, "TIGER");
      REPLACEMENTS.put(42308, "DRACULA");
      REPLACEMENTS.put(42326, "BUNNY");
      REPLACEMENTS.put(42312, "COBRA");
      REPLACEMENTS.put(42304, "HYDRA");
      REPLACEMENTS.put(42322, "RABBIT");
      REPLACEMENTS.put(42249, "HELPER");
      REPLACEMENTS.put(42259, "ML.MODER");
      REPLACEMENTS.put(42263, "MODER");
      REPLACEMENTS.put(42273, "MODER+");
      REPLACEMENTS.put(42277, "ST.MODER");
      REPLACEMENTS.put(42281, "GL.MODER");
      REPLACEMENTS.put(42291, "ML.ADMIN");
      REPLACEMENTS.put(42295, "ADMIN");
      REPLACEMENTS.put(42241, "MEDIA");
      REPLACEMENTS.put(42245, "YT");
      REPLACEMENTS.put(42313, "PEGAS");
      REPLACEMENTS.put(42242, "GOD");
      REPLACEMENTS.put(42246, "VAMPIRE");
      REPLACEMENTS.put(9679, "●");
      REPLACEMENTS.put(7424, "A");
      REPLACEMENTS.put(665, "B");
      REPLACEMENTS.put(7428, "C");
      REPLACEMENTS.put(7429, "D");
      REPLACEMENTS.put(7431, "E");
      REPLACEMENTS.put(42800, "F");
      REPLACEMENTS.put(610, "G");
      REPLACEMENTS.put(668, "H");
      REPLACEMENTS.put(618, "I");
      REPLACEMENTS.put(7434, "J");
      REPLACEMENTS.put(7435, "K");
      REPLACEMENTS.put(671, "L");
      REPLACEMENTS.put(7437, "M");
      REPLACEMENTS.put(628, "N");
      REPLACEMENTS.put(7439, "O");
      REPLACEMENTS.put(7448, "P");
      REPLACEMENTS.put(491, "Q");
      REPLACEMENTS.put(640, "R");
      REPLACEMENTS.put(42801, "S");
      REPLACEMENTS.put(7451, "T");
      REPLACEMENTS.put(7452, "U");
      REPLACEMENTS.put(7456, "V");
      REPLACEMENTS.put(7457, "W");
      REPLACEMENTS.put(7521, "X");
      REPLACEMENTS.put(655, "Y");
      REPLACEMENTS.put(7458, "Z");
      REPLACEMENTS.put(1110, "i");
      REPLACEMENTS.put(1030, "I");
      RANK_COLORS.put(42240, argb(120, 120, 120));
      RANK_COLORS.put(42244, argb(100, 113, 251));
      RANK_COLORS.put(42248, argb(214, 200, 42));
      RANK_COLORS.put(42258, argb(101, 189, 56));
      RANK_COLORS.put(42262, argb(64, 151, 214));
      RANK_COLORS.put(42272, argb(202, 130, 60));
      RANK_COLORS.put(42276, argb(202, 60, 60));
      RANK_COLORS.put(42280, argb(245, 51, 238));
      RANK_COLORS.put(42336, argb(214, 200, 42));
      RANK_COLORS.put(42290, argb(121, 81, 202));
      RANK_COLORS.put(42294, argb(202, 130, 60));
      RANK_COLORS.put(42308, argb(202, 60, 60));
      RANK_COLORS.put(42326, argb(68, 65, 66));
      RANK_COLORS.put(42312, argb(127, 214, 86));
      RANK_COLORS.put(42304, argb(92, 120, 7));
      RANK_COLORS.put(42322, argb(120, 120, 120));
      RANK_COLORS.put(42249, argb(214, 200, 42));
      RANK_COLORS.put(42259, argb(100, 113, 251));
      RANK_COLORS.put(42263, argb(100, 113, 251));
      RANK_COLORS.put(42273, argb(121, 81, 202));
      RANK_COLORS.put(42277, argb(100, 113, 251));
      RANK_COLORS.put(42281, argb(121, 81, 202));
      RANK_COLORS.put(42291, argb(64, 151, 214));
      RANK_COLORS.put(42295, argb(202, 60, 60));
      RANK_COLORS.put(42241, argb(121, 81, 202));
      RANK_COLORS.put(42245, argb(255, 255, 255));
      RANK_COLORS.put(42313, argb(255, 204, 0));
      RANK_COLORS.put(42305, argb(255, 140, 0));
      RANK_COLORS.put(42246, argb(255, 140, 0));
   }

   public record RankMatch(String text, int color) {
   }
}
