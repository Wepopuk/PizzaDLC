package pizza.dlc.client.modules.api;

public enum ModuleCategory {
   COMBAT("Combat"),
   MOVEMENT("Movement"),
   PLAYER("Player"),
   VISUALS("Visuals"),
   OTHER("Other"),
   ENVIRONMENT("Environment"),
   THEMES("Themes");

   final String displayName;

   public String getDisplayName() {
      return this.displayName;
   }

   private ModuleCategory(final String displayName) {
      this.displayName = displayName;
   }
}
