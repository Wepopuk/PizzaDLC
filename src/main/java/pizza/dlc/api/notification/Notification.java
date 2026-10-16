package pizza.dlc.api.notification;

import net.minecraft.world.item.ItemStack;
import pizza.dlc.api.animation.Animation;
import pizza.dlc.api.animation.Easing;
import pizza.dlc.client.modules.api.ModuleCategory;

public class Notification {
   public static final int NO_TOGGLE = -1;
   private final String title;
   private String message;
   private NotificationType type;
   private final ItemStack icon;
   private final boolean themeAccent;
   private final ModuleCategory category;
   private int toggle;
   private final Animation animation;
   private final long duration;
   private long startTime;
   float renderY;
   boolean placed;
   float toggleAnim;

   public Notification(String title, String message, NotificationType type, long duration) {
      this(title, message, type, duration, null, false);
   }

   public Notification(String title, String message, NotificationType type, long duration, ItemStack icon) {
      this(title, message, type, duration, icon, false);
   }

   public Notification(String title, String message, NotificationType type, long duration, ItemStack icon, boolean themeAccent) {
      this(title, message, type, duration, icon, themeAccent, null, NO_TOGGLE);
   }

   public Notification(
      String title, String message, NotificationType type, long duration, ItemStack icon, boolean themeAccent, ModuleCategory category, int toggle
   ) {
      this.title = title;
      this.message = message;
      this.type = type;
      this.duration = duration;
      this.icon = icon;
      this.themeAccent = themeAccent;
      this.category = category;
      this.toggle = toggle;
      this.toggleAnim = toggle == 1 ? 0.0F : 1.0F;
      this.startTime = System.currentTimeMillis();
      this.animation = new Animation(Easing.EASE_OUT_EXPO, 500L);
   }

   public boolean isExpired() {
      return System.currentTimeMillis() - this.startTime > this.duration;
   }

   public boolean isToggle() {
      return this.toggle != NO_TOGGLE;
   }

   void retoggle(String message, NotificationType type, int toggle) {
      this.message = message;
      this.type = type;
      this.toggle = toggle;
      this.startTime = System.currentTimeMillis();
   }

   public String getTitle() {
      return this.title;
   }

   public String getMessage() {
      return this.message;
   }

   public NotificationType getType() {
      return this.type;
   }

   public ItemStack getIcon() {
      return this.icon;
   }

   public boolean isThemeAccent() {
      return this.themeAccent;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public int getToggle() {
      return this.toggle;
   }

   public Animation getAnimation() {
      return this.animation;
   }

   public long getDuration() {
      return this.duration;
   }

   public long getStartTime() {
      return this.startTime;
   }
}
