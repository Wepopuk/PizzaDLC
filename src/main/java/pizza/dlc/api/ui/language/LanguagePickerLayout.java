package pizza.dlc.api.ui.language;

/** Shared drawing/input geometry, including the opening transform. */
public record LanguagePickerLayout(float scale, float centerX, float centerY) {
   public static final int WIDTH = 392, HEIGHT = 374;
   public static LanguagePickerLayout fit(int width, int height, float openingScale) {
      float fit = Math.min(1, Math.min(Math.max(1, width - 24) / (float)WIDTH, Math.max(1, height - 24) / (float)HEIGHT));
      return new LanguagePickerLayout(fit * openingScale, width / 2F, height / 2F);
   }
   public float localX(double screenX) { return (float)((screenX - centerX) / scale + WIDTH / 2F); }
   public float localY(double screenY) { return (float)((screenY - centerY) / scale + HEIGHT / 2F); }
   public static int rowY(int index) { return 92 + index * 44; }
   public static int rowAt(float x, float y) {
      if (x < 22 || x >= WIDTH - 22) return -1;
      int index = (int)Math.floor((y - 92) / 44);
      return index >= 0 && index < 5 && y < rowY(index) + 38 ? index : -1;
   }
}
