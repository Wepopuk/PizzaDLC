package pizza.dlc.api.rotation.ml;

public final class SegmentTracker {
   public static final int NO_TARGET = Integer.MIN_VALUE;
   private int segment = -1;
   private int tick = 0;
   private int lastTargetId = Integer.MIN_VALUE;
   private boolean open = false;

   public boolean advance(int targetId) {
      if (targetId == Integer.MIN_VALUE) {
         this.open = false;
         this.lastTargetId = Integer.MIN_VALUE;
         return false;
      }

      if (this.open && targetId == this.lastTargetId) {
         this.tick++;
      } else {
         this.segment++;
         this.tick = 0;
         this.open = true;
      }

      this.lastTargetId = targetId;
      return true;
   }

   public int segment() {
      return this.segment;
   }

   public int tick() {
      return this.tick;
   }

   public void reset() {
      this.open = false;
      this.lastTargetId = Integer.MIN_VALUE;
   }
}
