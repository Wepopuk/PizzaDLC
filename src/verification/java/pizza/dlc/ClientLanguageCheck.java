package pizza.dlc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import pizza.dlc.api.translation.ClientLanguage;
import pizza.dlc.api.translation.TranslationManager;
import pizza.dlc.api.ui.language.LanguagePickerLayout;
import pizza.dlc.api.ui.clickgui.ModuleSearch;
import pizza.dlc.client.modules.impl.misc.Sounds;

/** Persistence, bundled translations, glyph coverage, and transformed picker hit areas. */
public final class ClientLanguageCheck {
   public static void main(String[] args) throws Exception {
      Path folder = Files.createTempDirectory(Path.of("."), "language-");
      Path preference = folder.resolve("pizzadlc/language.json");
      TranslationManager manager = new TranslationManager(preference);
      check(manager.needsSelection() && manager.language() == ClientLanguage.RUSSIAN, "Fresh client must offer language selection");
      check(!Files.exists(preference), "Loading must not confirm a language before selection");
      check(manager.getAvailableLanguages().equals(java.util.List.of("en", "ru", "uk", "pl", "de")), "All five languages must be available");
      Sounds sounds = new Sounds();
      sounds.mode.setCurrentMode("Chaos");
      for (String query : new String[]{"toggle", "SOUNDS", "звук", "громкость", "Lautstärke", "glosnosc", "sounds toggle"}) {
         check(ModuleSearch.matches(sounds, query, manager), "Sounds not found by setting/mode: " + query);
      }
      check(!ModuleSearch.matches(sounds, "no-such-setting-123", manager), "Unrelated search matched Sounds");
      check(ModuleSearch.matches(sounds, "   ", manager), "Whitespace search should show modules");
      String moduleState = sounds.save().toString();
      for (ClientLanguage language : ClientLanguage.values()) {
         check(manager.setLanguage(language.code()), "Could not save " + language.code());
         TranslationManager restarted = new TranslationManager(preference);
         check(restarted.language() == language && !restarted.needsSelection(), "Restart lost language " + language.code());
         check(!restarted.translate("language.choose").equals("language.choose"), "Missing picker translation");
         check(restarted.translate("search.empty", "CustomQuery_123").contains("CustomQuery_123"), "Search text changed");
         check(restarted.text("UserDefined_123").equals("UserDefined_123"), "Unknown user content must remain intact");
         check(!Files.exists(preference.resolveSibling("language.json.tmp")), "Atomic save left temporary file");
         check(ModuleSearch.matches(sounds, restarted.text("Sounds"), restarted), "Translated module name not searchable");
         check(moduleState.equals(sounds.save().toString()) && sounds.getName().equals("Sounds") && sounds.mode.get().equals("Chaos"), "Translation changed module IDs/config/mode");
         check(language.minecraftCode().matches("en_us|ru_ru|uk_ua|pl_pl|de_de"), "Invalid vanilla locale");
      }
      String before = Files.readString(preference);
      check(!manager.setLanguage("invalid") && before.equals(Files.readString(preference)), "Invalid code changed preference");
      check(manager.setLanguage("ua") && manager.language() == ClientLanguage.UKRAINIAN, "UA alias must resolve to Ukrainian");
      check(Files.readString(preference).contains("\"uk\""), "Persist canonical language code");
      Files.writeString(preference, "{\"language\":\"unknown\"}");
      check(new TranslationManager(preference).needsSelection(), "Unknown saved language must prompt selection");
      Files.writeString(preference, "{broken");
      TranslationManager repaired = new TranslationManager(preference);
      check(repaired.needsSelection() && repaired.setLanguage("pl"), "Corrupt preference must be repairable");
      Path blocked = folder.resolve("file-instead-of-directory");
      Files.writeString(blocked, "blocker");
      TranslationManager failed = new TranslationManager(blocked.resolve("language.json"));
      check(!failed.setLanguage("de") && failed.needsSelection() && failed.language() == ClientLanguage.RUSSIAN,
         "Failed save must not mark language confirmed");

      Set<String> messages = null, labels = null;
      Set<Integer> characters = new HashSet<>();
      for (ClientLanguage language : ClientLanguage.values()) {
         JsonObject data = json("lang/client/" + language.code() + ".json");
         JsonObject m = data.getAsJsonObject("messages"), l = data.getAsJsonObject("labels");
         if (messages == null) { messages = m.keySet(); labels = l.keySet(); }
         check(messages.equals(m.keySet()) && labels.equals(l.keySet()), "Catalog keys differ: " + language.code());
         for (JsonObject section : new JsonObject[]{m, l}) section.entrySet().forEach(entry -> {
            String value = entry.getValue().getAsString();
            check(!value.isBlank(), "Empty translation: " + entry.getKey());
            value.codePoints().filter(c -> !Character.isWhitespace(c)).forEach(characters::add);
         });
         language.nativeName().codePoints().forEach(characters::add);
         try (var stream = resource("textures/gui/languages/" + language.flag() + ".png")) {
            var image = ImageIO.read(stream);
            check(image != null && image.getWidth() == 96 && image.getHeight() == 64, "Missing/bad flag " + language.flag());
         }
      }
      for (String name : new String[]{"nexa_regular", "nexa_book", "sfpro", "regular"}) {
         JsonObject data = json("fonts/msdf/" + name + ".json");
         Set<Integer> glyphs = new HashSet<>();
         data.getAsJsonArray("glyphs").forEach(g -> check(glyphs.add(g.getAsJsonObject().get("unicode").getAsInt()), "Duplicate glyph in " + name));
         Set<Integer> missing = new HashSet<>(characters);
         missing.removeAll(glyphs);
         check(missing.isEmpty(), "Missing translated glyphs in " + name + ": " + missing);
         try (var stream = resource("fonts/msdf/" + name + ".png")) {
            var image = ImageIO.read(stream);
            JsonObject atlas = data.getAsJsonObject("atlas");
            check(image.getWidth() == atlas.get("width").getAsInt() && image.getHeight() == atlas.get("height").getAsInt(), "Font atlas dimensions differ");
         }
      }
      checkLabelCoverage(labels);
      for (int[] size : new int[][]{{160, 90}, {320, 180}, {854, 480}, {1920, 1080}}) {
         for (float opening : new float[]{.94F, .975F, 1F}) {
            LanguagePickerLayout layout = LanguagePickerLayout.fit(size[0], size[1], opening);
            check(layout.scale() > 0 && 392 * layout.scale() <= size[0] - 24.001F + .01F
               && 374 * layout.scale() <= size[1] - 24.001F + .01F, "Picker does not fit window");
            for (int row = 0; row < 5; row++) {
               double x = layout.centerX() + (100 - 196) * layout.scale();
               double y = layout.centerY() + (LanguagePickerLayout.rowY(row) + 19 - 187) * layout.scale();
               check(LanguagePickerLayout.rowAt(layout.localX(x), layout.localY(y)) == row, "Animated/scaled hit area differs from rendering");
               check(LanguagePickerLayout.rowAt(100, LanguagePickerLayout.rowY(row) + 40) == -1, "Card gap is clickable");
            }
         }
      }
      check(LanguagePickerLayout.rowAt(21, 100) == -1 && LanguagePickerLayout.rowAt(370, 100) == -1
         && LanguagePickerLayout.rowAt(100, 91) == -1 && LanguagePickerLayout.rowAt(100, 324) == -1, "Outside cards is clickable");
      System.out.println("Verified five languages, first-launch persistence, failure recovery, fonts/flags and scaled selection.");
   }

   private static java.io.InputStream resource(String path) {
      var stream = ClientLanguageCheck.class.getResourceAsStream("/assets/pizzadlc/" + path);
      check(stream != null, "Missing bundled resource " + path);
      return stream;
   }
   private static void checkLabelCoverage(Set<String> labels) throws Exception {
      Path root = Path.of("").toAbsolutePath().getParent().getParent().resolve("src/main/java/pizza/dlc");
      String token = "\"(?:[^\"\\\\]|\\\\.)*\"";
      var strings = java.util.regex.Pattern.compile(token);
      var names = java.util.regex.Pattern.compile("\\b(?:name|description)\\s*=\\s*(" + token + ")");
      var constructors = java.util.regex.Pattern.compile("new\\s+\\w*Setting\\s*\\(");
      var groups = java.util.regex.Pattern.compile("\\.(?:section|group)\\(\\s*(" + token + ")|\\bstatic\\s+final\\s+String\\s+\\w+\\s*=\\s*(" + token + ")");
      try (var paths = Files.walk(root.resolve("client/modules"))) {
         for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
            String source = Files.readString(path);
            var annotations = names.matcher(source);
            while (annotations.find()) requireLabel(labels, annotations.group(1), path);
            var constants = groups.matcher(source);
            while (constants.find()) {
               String value = constants.group(1) == null ? constants.group(2) : constants.group(1);
               if (value.matches(".*[А-Яа-яЁё].*")) requireLabel(labels, value, path);
            }
            var settings = constructors.matcher(source);
            while (settings.find()) {
               int cursor = settings.end(), depth = 1;
               while (cursor < source.length() && depth > 0) {
                  if (source.charAt(cursor) == '"') {
                     var literal = strings.matcher(source).region(cursor, source.length());
                     check(literal.lookingAt(), "Invalid string in " + path);
                     requireLabel(labels, literal.group(), path);
                     cursor = literal.end();
                  } else {
                     if (source.charAt(cursor) == '(') depth++;
                     if (source.charAt(cursor) == ')') depth--;
                     cursor++;
                  }
               }
            }
         }
      }
      try (var paths = Files.walk(root.resolve("api/ui"))) {
         for (Path path : paths.filter(p -> p.toString().endsWith(".java") && !p.getFileName().toString().equals("LanguageSelectScreen.java")).toList()) {
            var literal = strings.matcher(Files.readString(path));
            while (literal.find()) if (literal.group().matches(".*[А-Яа-яЁё].*")) requireLabel(labels, literal.group(), path);
         }
      }
   }
   private static void requireLabel(Set<String> labels, String literal, Path path) {
      String value = JsonParser.parseString(literal).getAsString();
      check(value.isBlank() || labels.contains(value), "Untranslated client label in " + path.getFileName() + ": " + value);
   }
   private static JsonObject json(String path) throws Exception {
      try (var stream = resource(path)) {
         return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
      }
   }
   private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
