package pizza.dlc.api.theme;

public final class Theme {
   private final String name;
   private int accent;
   private int secondary;

   public Theme(String name, int accent, int secondary) {
      this.name = name;
      this.accent = accent;
      this.secondary = secondary;
   }

   public String name() {
      return this.name;
   }

   public int accent() {
      return this.accent;
   }

   public int secondary() {
      return this.secondary;
   }

   public void setAccent(int color) {
      this.accent = color;
   }

   public void setSecondary(int color) {
      this.secondary = color;
   }
}
