package pizza.dlc.api.render.msdf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import pizza.dlc.PizzaDLC;

public final class GlyphReplacementManager {
   private static final String FILE_NAME = "glyph_replacements.json";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final File file = new File(PizzaDLC.DIRECTORY, FILE_NAME);
   private volatile long lastModified = -1L;
   private volatile long lastCheckedAtMs = 0L;
   private volatile Map<String, Map<Integer, Replacement>> rulesByHost = Collections.emptyMap();

   public Replacement getReplacement(String serverHost, int codePoint) {
      return this.rulesFor(serverHost).get(codePoint);
   }

   public RuleSet rulesFor(String serverHost) {
      this.reloadIfNeeded();
      Map<String, Map<Integer, Replacement>> rules = this.rulesByHost;
      Map<Integer, Replacement> host = null;
      if (serverHost != null && !serverHost.isEmpty()) {
         String h = serverHost.toLowerCase(Locale.ROOT);
         int colon = h.indexOf(58);
         if (colon > 0) {
            h = h.substring(0, colon);
         }

         host = rules.get(h);
         if (host == null) {
            String base = baseDomain(h);

            for (Entry<String, Map<Integer, Replacement>> e : rules.entrySet()) {
               if ("*".equals(e.getKey()) || !baseDomain(e.getKey()).equals(base)) continue;

               host = e.getValue();
               break;
            }
         }
      }

      return new RuleSet(host, rules.get("*"));
   }

   private static String baseDomain(String h) {
      if (h != null && !h.isEmpty()) {
         int lastDot = h.lastIndexOf(46);
         if (lastDot <= 0) {
            return h;
         }

         int prevDot = h.lastIndexOf(46, lastDot - 1);
         return prevDot < 0 ? h : h.substring(prevDot + 1);
      }

      return "";
   }

   private void reloadIfNeeded() {
      long now = System.currentTimeMillis();
      if (now - this.lastCheckedAtMs >= 1000L) {
         this.lastCheckedAtMs = now;

         try {
            if (!this.file.exists()) {
               this.writeDefaultFile();
            }

            long lm = this.file.lastModified();
            if (lm == this.lastModified) {
               return;
            }

            this.lastModified = lm;
            Map<String, Map<Integer, Replacement>> loaded = this.readFile();
            this.rulesByHost = loaded != null ? loaded : Collections.emptyMap();
         } catch (Exception ignored) {
         }
      }
   }

   private void writeDefaultFile() {
      try {
         if (this.file.getParentFile() != null) {
            this.file.getParentFile().mkdirs();
         }

         JsonObject root = new JsonObject();
         root.addProperty(
            "_comment",
            "Карта подмены глифов: хост сервера -> { кодпоинт: замена }. Кодпоинт: 'U+A506' / '0xA506' / десятичный / сам символ. Замена: \"ТЕКСТ\" либо {\"text\":\"ТЕКСТ\",\"color\":\"#RRGGBB\"}; пустой \"\" убирает символ. Хост \"*\" — для всех серверов, конкретный (напр. mc.bravohvh.fun) перебивает \"*\". Это встроенные дефолты — правь/добавляй свои, файл перечитывается на лету."
         );
         JsonObject global = new JsonObject();

         for (Entry<Integer, Replacement> e : ReplaceSymbols.builtinDefaults().entrySet()) {
            String key = codePointKey(e.getKey());
            Replacement r = e.getValue();
            if (r.color() != 0) {
               JsonObject obj = new JsonObject();
               obj.addProperty("text", r.text());
               obj.addProperty("color", toHex(r.color()));
               global.add(key, obj);
            } else {
               global.addProperty(key, r.text());
            }
         }

         root.add("*", global);

         try (FileWriter writer = new FileWriter(this.file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
         }
      } catch (Exception ignored) {
      }
   }

   private static String codePointKey(int cp) {
      return String.format("U+%04X", cp);
   }

   private static String toHex(int argb) {
      int alpha = argb >>> 24 & 0xFF;
      return alpha == 255 ? String.format("#%06X", argb & 0xFFFFFF) : String.format("#%08X", argb);
   }

   private Map<String, Map<Integer, Replacement>> readFile() {
      try {
         try (BufferedReader reader = new BufferedReader(new FileReader(this.file, StandardCharsets.UTF_8))) {
            JsonElement rootEl = (JsonElement)GSON.fromJson(reader, JsonElement.class);
            if (rootEl == null || !rootEl.isJsonObject()) {
               return Collections.emptyMap();
            }

            JsonObject root = rootEl.getAsJsonObject();
            if (root.isEmpty()) {
               return Collections.emptyMap();
            }

            Map<String, Map<Integer, Replacement>> out = new HashMap<>();

            for (Entry<String, JsonElement> serverEntry : root.entrySet()) {
               String host = serverEntry.getKey();
               if (host != null) {
                  host = host.trim().toLowerCase(Locale.ROOT);
                  if (!host.isEmpty() && !host.startsWith("_")) {
                     JsonElement serverVal = serverEntry.getValue();
                     if (serverVal != null && serverVal.isJsonObject()) {
                        JsonObject mappings = serverVal.getAsJsonObject();
                        if (!mappings.isEmpty()) {
                           Map<Integer, Replacement> byCodePoint = new HashMap<>();

                           for (Entry<String, JsonElement> mapEntry : mappings.entrySet()) {
                              int cp = this.parseCodePointKey(mapEntry.getKey());
                              if (cp >= 0) {
                                 Replacement repl = this.parseReplacement(mapEntry.getValue());
                                 if (repl != null) {
                                    byCodePoint.put(cp, repl);
                                 }
                              }
                           }

                           if (!byCodePoint.isEmpty()) {
                              out.put(host, byCodePoint);
                           }
                        }
                     }
                  }
               }
            }

            return out;
         }
      } catch (Exception ignored) {
         return Collections.emptyMap();
      }
   }

   private Replacement parseReplacement(JsonElement replEl) {
      if (replEl == null) {
         return null;
      }

      if (replEl.isJsonPrimitive()) {
         String text = replEl.getAsString();
         return text == null ? null : new Replacement(text.trim(), 0);
      }

      if (!replEl.isJsonObject()) {
         return null;
      }

      JsonObject obj = replEl.getAsJsonObject();
      if (!obj.has("text")) {
         return null;
      }

      JsonElement textEl = obj.get("text");
      if (textEl != null && textEl.isJsonPrimitive()) {
         String text = textEl.getAsString();
         if (text == null) {
            return null;
         }

         text = text.trim();
         int color = 0;
         if (obj.has("color")) {
            JsonElement colorEl = obj.get("color");
            if (colorEl != null && colorEl.isJsonPrimitive()) {
               color = this.parseColor(colorEl.getAsString());
            }
         }

         return new Replacement(text, color);
      }

      return null;
   }

   private int parseColor(String s) {
      if (s == null) {
         return 0;
      }

      s = s.trim();
      if (s.isEmpty()) {
         return 0;
      }

      String up = s.toUpperCase(Locale.ROOT);

      try {
         if (up.startsWith("#")) {
            String hex = up.substring(1);
            long v = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
               return (int)(4278190080L | v);
            }

            if (hex.length() == 8) {
               return (int)v;
            }

            return 0;
         }

         if (up.startsWith("0X")) {
            long vx = Long.parseLong(up.substring(2), 16);
            if (up.length() <= 8) {
               return (int)(4278190080L | vx);
            }

            return (int)vx;
         }

         if (up.matches("^[0-9]+$")) {
            long vx = Long.parseLong(up);
            if (vx <= 0xFFFFFFL) {
               return (int)(4278190080L | vx);
            }

            return (int)vx;
         }
      } catch (Exception ignored) {
      }

      return 0;
   }

   private int parseCodePointKey(String key) {
      if (key == null) {
         return -1;
      }

      key = key.trim();
      if (key.isEmpty()) {
         return -1;
      }

      if (key.codePointCount(0, key.length()) == 1) {
         return key.codePointAt(0);
      }

      String k = key.toUpperCase(Locale.ROOT);

      try {
         if (k.startsWith("U+")) {
            return Integer.parseInt(k.substring(2), 16);
         }

         return k.startsWith("0X") ? Integer.parseInt(k.substring(2), 16) : Integer.parseInt(k);
      } catch (Exception ignored) {
         return -1;
      }
   }

   public record Replacement(String text, int color) {
   }

   public record RuleSet(Map<Integer, Replacement> host, Map<Integer, Replacement> global) {
      public Replacement get(int codePoint) {
         Replacement r = this.host != null ? this.host.get(codePoint) : null;
         if (r != null) {
            return r;
         }

         return this.global != null ? this.global.get(codePoint) : null;
      }
   }
}
