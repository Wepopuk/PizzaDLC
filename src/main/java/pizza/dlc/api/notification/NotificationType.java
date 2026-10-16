package pizza.dlc.api.notification;

public enum NotificationType {
   INFO(-1),
   WARNING(0xFFEBCA21),
   ERROR(0xFFFF3232),
   SUCCESS(0xFF18FF00);

   private final int color;

   private NotificationType(int color) {
      this.color = color;
   }

   public int color() {
      return this.color;
   }
}
