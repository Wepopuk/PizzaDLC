package pizza.dlc.api.theme;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import pizza.dlc.PizzaDLC;

public final class ThemeManager {
   private static final File FILE = new File(PizzaDLC.DIRECTORY, "themes.json");
   private static final List<Theme> THEMES = new ArrayList<>();
   private static String activeName;
   private static boolean loaded;

   private ThemeManager() {
   }

   private static Theme[] defaults() {
      return new Theme[]{
         new Theme("Калцит", 0xFF8E8BFF, 0xFFC98BFF),
         new Theme("Розовая", 0xFFEE5577, 0xFFC026D3),
         new Theme("Синяя", 0xFF4060FF, 0xFF8C64FF),
         new Theme("Фиолетовая", 0xFFC026D3, 0xFF2D5BFF),
         new Theme("Бирюзовая", 0xFF1FD3C0, 0xFF2D9BFF),
         new Theme("Зелёная", 0xFF35D07A, 0xFF1FA85F),
         new Theme("Оранжевая", 0xFFFF8A3D, 0xFFFF4D4D),
         new Theme("Красная", 0xFFFF3B3B, 0xFFFF7A3D),
         new Theme("Аврора", 0xFF00F5A0, 0xFF7A04EB),
         new Theme("Каберне", 0xFFF4B860, 0xFF7B2D6B),
         new Theme("Сумерки", 0xFFFF9E80, 0xFF5D3FD3)
      };
   }

   private static void fillDefaults() {
      for (Theme t : defaults()) {
         THEMES.add(t);
      }
   }

   public static List<Theme> themes() {
      ensureLoaded();
      return THEMES;
   }

   public static String[] names() {
      ensureLoaded();
      return THEMES.stream().map(Theme::name).toArray(String[]::new);
   }

   public static Theme current() {
      ensureLoaded();

      for (Theme t : THEMES) {
         if (!t.name().equalsIgnoreCase(activeName)) continue;

         return t;
      }

      return THEMES.get(0);
   }

   public static boolean isActive(Theme t) {
      ensureLoaded();
      return t != null && t.name().equalsIgnoreCase(activeName);
   }

   public static void setActive(String name) {
      ensureLoaded();

      for (Theme t : THEMES) {
         if (!t.name().equalsIgnoreCase(name)) continue;

         activeName = t.name();
         save();
         return;
      }
   }

   public static Theme addTheme() {
      ensureLoaded();
      String name = "Тема";

      for (int i = 2; exists(name); i++) {
         name = "Тема " + i;
      }

      Theme t = new Theme(name, 0xFFEE5577, 0xFFC026D3);
      THEMES.add(t);
      save();
      return t;
   }

   public static void removeTheme(Theme t) {
      ensureLoaded();
      if (t != null && THEMES.size() > 1) {
         boolean wasActive = t.name().equalsIgnoreCase(activeName);
         THEMES.remove(t);
         if (wasActive) {
            activeName = THEMES.get(0).name();
         }

         save();
      }
   }

   private static boolean exists(String name) {
      for (Theme t : THEMES) {
         if (!t.name().equalsIgnoreCase(name)) continue;

         return true;
      }

      return false;
   }

   public static int accent() {
      return current().accent();
   }

   public static int secondary() {
      return current().secondary();
   }

   public static int accentAlpha(int alpha) {
      return accent() & 0xFFFFFF | (alpha & 0xFF) << 24;
   }

   public static int accentRGB() {
      return accent() & 0xFFFFFF;
   }

   public static void save() {
      ensureLoaded();

      try {
         JsonObject root = new JsonObject();
         root.addProperty("active", activeName);
         JsonArray arr = new JsonArray();

         for (Theme t : THEMES) {
            JsonObject o = new JsonObject();
            o.addProperty("name", t.name());
            o.addProperty("accent", t.accent());
            o.addProperty("secondary", t.secondary());
            arr.add(o);
         }

         root.add("themes", arr);
         File parent = FILE.getParentFile();
         if (parent != null) {
            parent.mkdirs();
         }

         try (Writer w = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
         }
      } catch (Exception ignored) {
      }
   }

   private static void ensureLoaded() {
      if (loaded) return;

      loaded = true;

      try {
         if (FILE.exists()) {
            try (Reader r = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
               JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
               JsonArray arr = root.has("themes") ? root.getAsJsonArray("themes") : null;
               if (arr != null) {
                  for (JsonElement el : arr) {
                     JsonObject o = el.getAsJsonObject();
                     THEMES.add(new Theme(o.get("name").getAsString(), o.get("accent").getAsInt(), o.get("secondary").getAsInt()));
                  }
               }

               if (root.has("active")) {
                  activeName = root.get("active").getAsString();
               }
            }
         }
      } catch (Exception ignored) {
         THEMES.clear();
      }

      if (THEMES.isEmpty()) {
         fillDefaults();
      } else {
         for (Theme d : defaults()) {
            if (exists(d.name())) continue;

            THEMES.add(d);
         }
      }

      if (activeName == null || !exists(activeName)) {
         activeName = THEMES.get(0).name();
      }
   }
}
