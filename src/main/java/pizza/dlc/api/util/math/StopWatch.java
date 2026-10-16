package pizza.dlc.api.util.math;

public class StopWatch {
   private long lastMS = System.currentTimeMillis();

   public void reset() {
      this.lastMS = System.currentTimeMillis();
   }

   public boolean hasTimeElapsed(long time) {
      return System.currentTimeMillis() - this.lastMS > time;
   }

   public long getTime() {
      return System.currentTimeMillis() - this.lastMS;
   }
}
