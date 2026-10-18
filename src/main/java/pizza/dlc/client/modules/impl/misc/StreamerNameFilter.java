package pizza.dlc.client.modules.impl.misc;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java fuzzy nickname matching for StreamerMode. No Minecraft dependencies,
 * so it can be unit-tested with plain javac/java.
 *
 * Catches bypass attempts:
 * - invisible characters inside the name (zero-width spaces, joiners, variation
 *   selectors, combining marks, soft hyphen, braille blank, bidi controls, etc.)
 * - Minecraft formatting codes (§a, &a, &#RRGGBB) mixed into the name
 * - homoglyphs: 0/O/О, 1/l/I/І, Cyrillic/Greek lookalikes (а/a, с/c, е/e, ...),
 *   fullwidth characters, leet digits (3/e, 5/s, ...), case differences
 *
 * Known limitation: one-to-many transliteration (я->ya, ю->yu, ш->sh, ...) only
 * matches when the rendered length stays the same; fully re-typed transliterated
 * nicknames ("Ярик" instead of "Yarik") are not matched.
 */
public final class StreamerNameFilter {
   private StreamerNameFilter() {
   }

   /** Replaces every fuzzy occurrence of any name with {@code repl}. Never returns null. */
   public static String replace(String text, List<String> names, String repl) {
      if (text == null || text.isEmpty() || names == null || names.isEmpty() || repl == null) {
         return text;
      }
      List<int[]> norms = nameNorms(names);
      if (norms.isEmpty()) {
         return text;
      }
      List<int[]> matches = findMatches(text, norms);
      if (matches.isEmpty()) {
         return text;
      }
      StringBuilder sb = new StringBuilder(text.length() + 16);
      int cur = 0;
      for (int[] m : matches) {
         if (m[0] < cur) {
            continue;
         }
         sb.append(text, cur, m[0]).append(repl);
         cur = m[1];
      }
      sb.append(text, cur, text.length());
      return sb.toString();
   }

   /** Normalized (canonical) form of each usable name. */
   public static List<int[]> nameNorms(List<String> names) {
      List<int[]> out = new ArrayList<>(names.size());
      for (String n : names) {
         if (n == null || n.isEmpty()) {
            continue;
         }
         int[] nn = normalizeName(n);
         if (nn.length > 0) {
            out.add(nn);
         }
      }
      return out;
   }

   /**
    * Finds non-overlapping [start, end) char ranges in {@code text} matching any
    * of the precomputed {@code norms}. Ranges already include adjacent invisible
    * characters, so replacing the range removes the whole obfuscated fragment.
    */
   public static List<int[]> findMatches(String text, List<int[]> norms) {
      List<int[]> out = new ArrayList<>();
      if (text == null || text.isEmpty() || norms == null || norms.isEmpty()) {
         return out;
      }
      Src src = normalizeSrc(text);
      if (src.kept == 0) {
         return out;
      }
      int pos = 0;
      while (pos < src.kept) {
         int bestS = -1;
         int bestE = -1;
         int bestAfter = -1;
         for (int[] nn : norms) {
            int len = nn.length;
            if (len == 0 || pos + len > src.kept) {
               continue;
            }
            if (pos > 0 && isNameChar(src.prim[pos - 1])) {
               continue;
            }
            boolean ok = true;
            for (int k = 0; k < len; k++) {
               if (!altContains(src.alt[pos + k], nn[k])) {
                  ok = false;
                  break;
               }
            }
            if (!ok) {
               continue;
            }
            int after = pos + len;
            if (after < src.kept && isNameChar(src.prim[after])) {
               continue;
            }
            int s = expandLeft(text, src.orig[pos]);
            int lastOrig = src.orig[after - 1];
            int e = expandRight(text, lastOrig + Character.charCount(text.codePointAt(lastOrig)));
            if (bestS < 0 || e - s > bestE - bestS) {
               bestS = s;
               bestE = e;
               bestAfter = after;
            }
         }
         if (bestAfter >= 0) {
            out.add(new int[]{bestS, bestE});
            pos = bestAfter;
         } else {
            pos++;
         }
      }
      return out;
   }

   // ------------------------------------------------------------------ internals

   private static final class Src {
      final int[] prim;
      final int[][] alt;
      final int[] orig;
      final int kept;

      Src(int[] prim, int[][] alt, int[] orig, int kept) {
         this.prim = prim;
         this.alt = alt;
         this.orig = orig;
         this.kept = kept;
      }
   }

   private static int[] normalizeName(String name) {
      int len = name.length();
      int[] tmp = new int[name.codePointCount(0, len)];
      int n = 0;
      for (int i = 0; i < len;) {
         int cp = name.codePointAt(i);
         int cl = Character.charCount(cp);
         if (cp == 0x00A7) {
            i += cl;
            if (i < len) {
               i += Character.charCount(name.codePointAt(i));
            }
            continue;
         }
         if (cp == '&' && i + cl < len) {
            int next = name.codePointAt(i + cl);
            if (next == '#') {
               int hexEnd = skipHexColor(name, i + cl + 1);
               if (hexEnd > 0) {
                  i = hexEnd;
                  continue;
               }
            } else if (isFormatCode(next)) {
               i += cl + Character.charCount(next);
               continue;
            }
         }
         if (isSkipped(cp)) {
            i += cl;
            continue;
         }
         tmp[n++] = alts(cp)[0];
         i += cl;
      }
      int[] out = new int[n];
      System.arraycopy(tmp, 0, out, 0, n);
      return out;
   }

   private static Src normalizeSrc(String s) {
      int len = s.length();
      int[] prim = new int[len];
      int[][] alt = new int[len][];
      int[] orig = new int[len];
      int n = 0;
      for (int i = 0; i < len;) {
         int cp = s.codePointAt(i);
         int cl = Character.charCount(cp);
         if (cp == 0x00A7) {
            i += cl;
            if (i < len) {
               i += Character.charCount(s.codePointAt(i));
            }
            continue;
         }
         if (cp == '&' && i + cl < len) {
            int next = s.codePointAt(i + cl);
            if (next == '#') {
               int hexEnd = skipHexColor(s, i + cl + 1);
               if (hexEnd > 0) {
                  i = hexEnd;
                  continue;
               }
            } else if (isFormatCode(next)) {
               i += cl + Character.charCount(next);
               continue;
            }
         }
         if (isSkipped(cp)) {
            i += cl;
            continue;
         }
         int[] a = alts(cp);
         prim[n] = a[0];
         alt[n] = a;
         orig[n] = i;
         n++;
         i += cl;
      }
      return new Src(prim, alt, orig, n);
   }

   /** Skips "&#RRGGBB". Returns index after the color or -1 when it is not a hex color. */
   private static int skipHexColor(String s, int from) {
      int i = from;
      for (int k = 0; k < 6; k++) {
         if (i >= s.length()) {
            return -1;
         }
         int cp = s.codePointAt(i);
         if (!isHex(cp)) {
            return -1;
         }
         i += Character.charCount(cp);
      }
      return i;
   }

   private static boolean isHex(int cp) {
      return cp >= '0' && cp <= '9' || cp >= 'a' && cp <= 'f' || cp >= 'A' && cp <= 'F';
   }

   private static boolean isFormatCode(int cp) {
      return cp >= '0' && cp <= '9'
         || cp >= 'a' && cp <= 'f'
         || cp >= 'k' && cp <= 'o'
         || cp == 'r'
         || cp >= 'A' && cp <= 'F'
         || cp >= 'K' && cp <= 'O'
         || cp == 'R'
         || cp == 'x' || cp == 'X';
   }

   private static int expandLeft(String s, int from) {
      int i = from;
      while (i > 0) {
         int cp = s.codePointBefore(i);
         if (!isSkipped(cp)) {
            break;
         }
         i -= Character.charCount(cp);
      }
      return i;
   }

   private static int expandRight(String s, int from) {
      int i = from;
      int len = s.length();
      while (i < len) {
         int cp = s.codePointAt(i);
         if (!isSkipped(cp)) {
            break;
         }
         i += Character.charCount(cp);
      }
      return i;
   }

   private static boolean altContains(int[] set, int v) {
      for (int c : set) {
         if (c == v) {
            return true;
         }
      }
      return false;
   }

   private static boolean isNameChar(int canon) {
      return canon == '_' || Character.isLetterOrDigit(canon);
   }

   /** Invisible / zero-width / formatting characters that are skipped while matching. */
   private static boolean isSkipped(int cp) {
      if (cp < 0x20 || cp >= 0x7F && cp <= 0x9F) {
         return true;
      }
      if (cp == 0x00AD || cp == 0x034F || cp == 0x061C
         || cp == 0x115F || cp == 0x1160 || cp == 0x17B4 || cp == 0x17B5
         || cp == 0xFEFF || cp == 0x3164 || cp == 0xFFA0 || cp == 0x2800) {
         return true;
      }
      if (cp >= 0x180B && cp <= 0x180F) {
         return true;
      }
      if (cp >= 0x200B && cp <= 0x200F) {
         return true;
      }
      if (cp >= 0x2028 && cp <= 0x202E) {
         return true;
      }
      if (cp >= 0x2060 && cp <= 0x206F) {
         return true;
      }
      if (cp >= 0x2FF0 && cp <= 0x2FFB) {
         return true;
      }
      if (cp >= 0xFE00 && cp <= 0xFE0F) {
         return true;
      }
      if (cp >= 0xE0000 && cp <= 0xE0FFF) {
         return true;
      }
      if (cp >= 0x0300 && cp <= 0x036F) {
         return true;
      }
      if (cp >= 0x0483 && cp <= 0x0489) {
         return true;
      }
      if (cp >= 0x1AB0 && cp <= 0x1AFF) {
         return true;
      }
      if (cp >= 0x1DC0 && cp <= 0x1DFF) {
         return true;
      }
      if (cp >= 0x20D0 && cp <= 0x20FF) {
         return true;
      }
      return cp >= 0xFE20 && cp <= 0xFE2F;
   }

   // ------------------------------------------------------- confusable mapping

   private static final int[][] ASCII_ALTS = new int[128][];

   static {
      for (int i = 0; i < 128; i++) {
         ASCII_ALTS[i] = new int[]{Character.toLowerCase(i)};
      }
      ASCII_ALTS['0'] = new int[]{'o'};
      ASCII_ALTS['1'] = new int[]{'i'};
      ASCII_ALTS['2'] = new int[]{'z'};
      ASCII_ALTS['3'] = new int[]{'e'};
      ASCII_ALTS['4'] = new int[]{'a'};
      ASCII_ALTS['5'] = new int[]{'s'};
      ASCII_ALTS['6'] = new int[]{'b'};
      ASCII_ALTS['7'] = new int[]{'t'};
      ASCII_ALTS['8'] = new int[]{'b'};
      ASCII_ALTS['9'] = new int[]{'g'};
      ASCII_ALTS['I'] = new int[]{'i'};
      ASCII_ALTS['L'] = new int[]{'i'};
      ASCII_ALTS['l'] = new int[]{'i'};
      ASCII_ALTS['$'] = new int[]{'s'};
      ASCII_ALTS['|'] = new int[]{'|', 'i'};
      ASCII_ALTS['_'] = new int[]{'_'};
   }

   /**
    * Candidate canonical codepoints for {@code cp}. Element [0] is the primary
    * one used for names; the whole set is used for rendered text, so one source
    * character can match several name characters (e.g. 'п' matches both 'p'
    * by transliteration and 'n' visually).
    */
   private static int[] alts(int cp) {
      if (cp < 128) {
         return ASCII_ALTS[cp];
      }
      if (cp >= 0xFF10 && cp <= 0xFF19) {
         return ASCII_ALTS['0' + (cp - 0xFF10)];
      }
      if (cp >= 0xFF21 && cp <= 0xFF3A) {
         return ASCII_ALTS[Character.toLowerCase((char) ('A' + (cp - 0xFF21)))];
      }
      if (cp >= 0xFF41 && cp <= 0xFF5A) {
         return ASCII_ALTS['a' + (cp - 0xFF41)];
      }
      if (cp == 0xFF3F || cp == 0x02CD || cp == 0xFE33 || cp == 0xFE34) {
         return ASCII_ALTS['_'];
      }
      // Roman numerals that are a single letter.
      if (cp >= 0x2160 && cp <= 0x217F) {
         switch (cp) {
            case 0x2160:
            case 0x2170:
               return ASCII_ALTS['i'];
            case 0x2164:
            case 0x2174:
               return ASCII_ALTS['v'];
            case 0x2169:
            case 0x2179:
               return ASCII_ALTS['x'];
            case 0x216C:
            case 0x217C:
               return ASCII_ALTS['i'];
            case 0x216D:
            case 0x217D:
               return ASCII_ALTS['c'];
            case 0x216E:
            case 0x217E:
               return ASCII_ALTS['d'];
            case 0x216F:
            case 0x217F:
               return ASCII_ALTS['m'];
            default:
               break;
         }
      }
      // Encircled digits 1-9.
      if (cp >= 0x2460 && cp <= 0x2468 || cp >= 0x2474 && cp <= 0x247C
         || cp >= 0x2488 && cp <= 0x2490 || cp >= 0x2780 && cp <= 0x2788) {
         int d;
         if (cp <= 0x2468) {
            d = cp - 0x2460 + 1;
         } else if (cp <= 0x247C) {
            d = cp - 0x2474 + 1;
         } else if (cp <= 0x2490) {
            d = cp - 0x2488 + 1;
         } else {
            d = cp - 0x2780 + 1;
         }
         return ASCII_ALTS['0' + d];
      }
      // Encircled latin letters.
      if (cp >= 0x24B6 && cp <= 0x24CF) {
         return ASCII_ALTS[Character.toLowerCase((char) ('A' + (cp - 0x24B6)))];
      }
      if (cp >= 0x24D0 && cp <= 0x24E9) {
         return ASCII_ALTS['a' + (cp - 0x24D0)];
      }
      // Super/subscript digits 0-9.
      switch (cp) {
         case 0x2070:
         case 0x2080:
            return ASCII_ALTS['0'];
         case 0x00B9:
         case 0x2081:
            return ASCII_ALTS['1'];
         case 0x00B2:
         case 0x2082:
            return ASCII_ALTS['2'];
         case 0x00B3:
         case 0x2083:
            return ASCII_ALTS['3'];
         case 0x2074:
         case 0x2084:
            return ASCII_ALTS['4'];
         case 0x2075:
         case 0x2085:
            return ASCII_ALTS['5'];
         case 0x2076:
         case 0x2086:
            return ASCII_ALTS['6'];
         case 0x2077:
         case 0x2087:
            return ASCII_ALTS['7'];
         case 0x2078:
         case 0x2088:
            return ASCII_ALTS['8'];
         case 0x2079:
         case 0x2089:
            return ASCII_ALTS['9'];
         case 0x00AA:
            return ASCII_ALTS['a'];
         case 0x00BA:
            return ASCII_ALTS['o'];
         default:
            break;
      }
      switch (Character.toLowerCase(cp)) {
         // Cyrillic + Greek + Latin lookalikes, grouped by ASCII they can stand for.
         // а
         case 'а':
         case 'α':
            return ASCII_ALTS['a'];
         // в / β
         case 'в':
            return new int[]{'v', 'b'};
         case 'β':
            return new int[]{'b', 'v'};
         // с / σ / ς
         case 'с':
         case 'σ':
            return new int[]{'s', 'c'};
         case 'ς':
            return new int[]{'c', 's'};
         // е
         case 'е':
         case 'ё':
         case 'ε':
         case 'э':
         case 'є':
            return ASCII_ALTS['e'];
         // н
         case 'н':
            return new int[]{'n', 'h'};
         case 'һ':
            return ASCII_ALTS['h'];
         // к
         case 'к':
         case 'κ':
            return ASCII_ALTS['k'];
         // м
         case 'м':
            return ASCII_ALTS['m'];
         case 'μ':
            // U+039C (capital Mu, looks like M) and U+03BC (looks like u)
            // both lowercase to 'μ', so accept both readings.
            return new int[]{'m', 'u'};
         // о
         case 'о':
         case 'ο':
         case 'ө':
         case 'ӧ':
            return ASCII_ALTS['o'];
         // р / ρ
         case 'р':
         case 'ρ':
            return new int[]{'r', 'p'};
         // т
         case 'т':
         case 'τ':
            return ASCII_ALTS['t'];
         // х
         case 'х':
            return new int[]{'x', 'h'};
         case 'χ':
         case 'ξ':
            return ASCII_ALTS['x'];
         // у / υ
         case 'у':
         case 'υ':
            return new int[]{'u', 'y'};
         // б
         case 'б':
            return new int[]{'b', '6'};
         // д / г
         case 'д':
            return new int[]{'d', 'g'};
         case 'г':
         case 'ґ':
            return new int[]{'g', 'r'};
         // з
         case 'з':
            return new int[]{'z', 'e'};
         // и / й
         case 'и':
            return new int[]{'i', 'n'};
         case 'й':
            return new int[]{'y', 'i'};
         case 'і':
         case 'ї':
            return ASCII_ALTS['i'];
         // л
         case 'л':
            return new int[]{'i', 'n'};
         // п / π
         case 'п':
         case 'π':
            return new int[]{'p', 'n'};
         // ф
         case 'ф':
         case 'φ':
            return ASCII_ALTS['f'];
         // ц / ч
         case 'ц':
         case 'ч':
            return ASCII_ALTS['c'];
         // ш / щ
         case 'ш':
         case 'щ':
            return new int[]{'s', 'w'};
         // ж
         case 'ж':
            return ASCII_ALTS['z'];
         // ы
         case 'ы':
            return ASCII_ALTS['y'];
         // ю / я
         case 'ю':
            return new int[]{'y', 'u'};
         case 'я':
            return new int[]{'y', 'a'};
         // Greek leftovers
         case 'η':
            return new int[]{'n', 'i'};
         case 'θ':
            return ASCII_ALTS['o'];
         case 'ν':
            return new int[]{'n', 'v'};
         case 'ω':
            return new int[]{'o', 'w'};
         case 'λ':
            return ASCII_ALTS['i'];
         case 'γ':
            return new int[]{'g', 'y'};
         case 'δ':
            return ASCII_ALTS['d'];
         case 'ζ':
            return ASCII_ALTS['z'];
         case 'ι':
            return ASCII_ALTS['i'];
         // Latin extras without canonical decomposition
         case 'ø':
            return ASCII_ALTS['o'];
         case 'æ':
            return new int[]{'a', 'e'};
         case 'œ':
            return new int[]{'o', 'e'};
         case 'ß':
            return new int[]{'s', 'b'};
         case 'ł':
         case 'ĺ':
         case 'ļ':
         case 'ľ':
            return ASCII_ALTS['i'];
         case 'đ':
         case 'ď':
         case 'ð':
            return ASCII_ALTS['d'];
         case 'þ':
            return new int[]{'t', 'p'};
         case 'ı':
            return ASCII_ALTS['i'];
         case 'ŋ':
            return ASCII_ALTS['n'];
         case 'ƒ':
            return ASCII_ALTS['f'];
         case 'ğ':
         case 'ĝ':
         case 'ģ':
         case 'ġ':
            return ASCII_ALTS['g'];
         case 'ħ':
            return ASCII_ALTS['h'];
         case 'ĵ':
            return ASCII_ALTS['j'];
         case 'ķ':
            return ASCII_ALTS['k'];
         case 'ŵ':
            return ASCII_ALTS['w'];
         // Cyrillic letters missed by transliteration groups.
         case 'ѕ':
            return ASCII_ALTS['s'];
         case 'ј':
            return ASCII_ALTS['j'];
         case 'ќ':
            return ASCII_ALTS['k'];
         case 'ӏ':
            return ASCII_ALTS['i'];
         case 'ſ':
            return new int[]{'s', 'f'};
         // Lunate Greek letters (look exactly like latin ones).
         case 'ϲ':
            return ASCII_ALTS['c'];
         case 'ϳ':
            return ASCII_ALTS['j'];
         case 'ϵ':
            return ASCII_ALTS['e'];
         // Greek symbol variants.
         case 'ϐ':
            return new int[]{'b', 'v'};
         case 'ϑ':
            return ASCII_ALTS['o'];
         case 'ϕ':
            return ASCII_ALTS['f'];
         case 'ϖ':
            return new int[]{'p', 'n'};
         case 'ϰ':
            return ASCII_ALTS['k'];
         case 'ϱ':
            return new int[]{'r', 'p'};
         // Hook/diacritic latin letters (visually the base letter).
         case 'ɓ':
            return ASCII_ALTS['b'];
         case 'ƈ':
            return ASCII_ALTS['c'];
         case 'ɗ':
         case 'ɖ':
            return ASCII_ALTS['d'];
         case 'ɠ':
            return ASCII_ALTS['g'];
         case 'ƙ':
            return ASCII_ALTS['k'];
         case 'ɲ':
            return ASCII_ALTS['n'];
         case 'ƥ':
            return ASCII_ALTS['p'];
         case 'ʈ':
         case 'ƭ':
            return ASCII_ALTS['t'];
         case 'ʋ':
            return ASCII_ALTS['v'];
         case 'ƴ':
            return ASCII_ALTS['y'];
         case 'ʒ':
            return new int[]{'z', 'e'};
         case 'ƶ':
            return ASCII_ALTS['z'];
         // Turned/small-cap latin letters.
         case 'ɐ':
            return ASCII_ALTS['a'];
         case 'ǝ':
            return ASCII_ALTS['e'];
         case 'ɩ':
            return ASCII_ALTS['i'];
         case 'ɔ':
            return ASCII_ALTS['o'];
         case 'ʌ':
            return ASCII_ALTS['v'];
         case 'ɯ':
            return new int[]{'u', 'w'};
         case 'ɹ':
            return ASCII_ALTS['r'];
         case 'ᴀ':
            return ASCII_ALTS['a'];
         case 'ʙ':
            return ASCII_ALTS['b'];
         case 'ᴄ':
            return ASCII_ALTS['c'];
         case 'ᴅ':
            return ASCII_ALTS['d'];
         case 'ᴇ':
            return ASCII_ALTS['e'];
         case 'ᴊ':
            return ASCII_ALTS['j'];
         case 'ᴋ':
            return ASCII_ALTS['k'];
         case 'ʟ':
            return ASCII_ALTS['i'];
         case 'ᴍ':
            return ASCII_ALTS['m'];
         case 'ɴ':
            return ASCII_ALTS['n'];
         case 'ᴏ':
            return ASCII_ALTS['o'];
         case 'ᴘ':
            return ASCII_ALTS['p'];
         case 'ʀ':
            return ASCII_ALTS['r'];
         case 'ᴛ':
            return ASCII_ALTS['t'];
         case 'ᴜ':
            return ASCII_ALTS['u'];
         case 'ᴠ':
            return ASCII_ALTS['v'];
         case 'ᴡ':
            return ASCII_ALTS['w'];
         case 'ʏ':
            return ASCII_ALTS['y'];
         case 'ᴢ':
            return ASCII_ALTS['z'];
         case 'ɢ':
            return ASCII_ALTS['g'];
         case 'ɸ':
            return ASCII_ALTS['f'];
         case 'ĸ':
            return new int[]{'k', 'q'};
         // Armenian lookalikes.
         case 'օ':
            return ASCII_ALTS['o'];
         case 'ա':
            return new int[]{'a', 'w'};
         // Common symbols used as letters.
         case '€':
            return new int[]{'e', 'c'};
         case '£':
            return ASCII_ALTS['i'];
         case '¥':
            return ASCII_ALTS['y'];
         case '¢':
            return ASCII_ALTS['c'];
         case '×':
            return ASCII_ALTS['x'];
         case '∞':
            return ASCII_ALTS['b'];
         case '∑':
            return ASCII_ALTS['s'];
         case '∏':
            return new int[]{'p', 'n'};
         case '∆':
         case '∂':
            return ASCII_ALTS['d'];
         case '©':
            return ASCII_ALTS['c'];
         case '®':
            return ASCII_ALTS['r'];
         default:
            break;
      }
      // Accented Latin (é, ü, ñ, ...) via canonical decomposition: base letter + marks.
      if (cp >= 0xC0) {
         String decomposed = Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFD);
         if (!decomposed.isEmpty()) {
            int base = decomposed.codePointAt(0);
            if (base != cp) {
               return alts(base);
            }
         }
      }
      return new int[]{Character.toLowerCase(cp)};
   }
}
