package pizza.dlc.api.ui.account;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class NickGenerator {
   private static final String[] ADJECTIVES = {
      "Dark", "Silent", "Toxic", "Frozen", "Crazy", "Lazy", "Swift", "Lucky", "Wild", "Shadow", "Ghost", "Neon", "Iron", "Golden", "Cosmic",
      "Angry", "Sleepy", "Rapid", "Savage", "Mystic", "Cyber", "Lunar", "Solar", "Red", "Blue", "Frost", "Storm", "Night", "Pixel", "Turbo"
   };
   private static final String[] NOUNS = {
      "Wolf", "Fox", "Bear", "Tiger", "Hawk", "Raven", "Viper", "Blade", "Knight", "Hunter", "Sniper", "Ninja", "Pirate", "Dragon", "Panda",
      "Cat", "Owl", "Shark", "Golem", "Creeper", "Miner", "Archer", "Rogue", "Wizard", "Spirit", "Comet", "Rocket", "Phoenix", "Lynx", "Cobra"
   };
   private static final String[] NAMES = {
      "Kirill", "Vanya", "Sanya", "Dima", "Maks", "Artem", "Egor", "Nikita", "Misha", "Lesha", "Pasha", "Vlad", "Danya", "Tema", "Kostya",
      "Roma", "Seva", "Gosha", "Yarik", "Stas"
   };
   private static final String[] TAGS = {"Pro", "Top", "YT", "Play", "Gg", "Off", "Prime", "Max", "Real", "Zz"};
   private static final int ATTEMPTS = 40;
   private static final int MIN_LENGTH = 3;
   private static final int MAX_LENGTH = 16;

   private NickGenerator() {
   }

   public static String generate(Predicate<String> taken) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      for (int i = 0; i < ATTEMPTS; i++) {
         String name = switch (random.nextInt(5)) {
            case 0 -> pick(ADJECTIVES) + pick(NOUNS);
            case 1 -> pick(ADJECTIVES) + pick(NOUNS) + random.nextInt(10, 100);
            case 2 -> pair(random).toLowerCase(Locale.ROOT);
            case 3 -> pick(NAMES) + (random.nextBoolean() ? "_" + pick(TAGS) : String.valueOf(random.nextInt(2005, 2015)));
            default -> pick(NAMES) + "_" + random.nextInt(100, 1000);
         };
         if (name.length() >= MIN_LENGTH && name.length() <= MAX_LENGTH && !taken.test(name)) return name;
      }

      return "Player" + random.nextInt(1000, 10000);
   }

   private static String pair(ThreadLocalRandom random) {
      int first = random.nextInt(NOUNS.length);
      int second = (first + 1 + random.nextInt(NOUNS.length - 1)) % NOUNS.length;
      return NOUNS[first] + "_" + NOUNS[second];
   }

   private static String pick(String[] words) {
      return words[ThreadLocalRandom.current().nextInt(words.length)];
   }
}
