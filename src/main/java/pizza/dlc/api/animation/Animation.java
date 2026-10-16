package pizza.dlc.api.animation;

public class Animation {
   private Easing easing;
   private long duration;
   private long startTime;
   private float startValue;
   private float targetValue;
   private float value;

   public Animation(Easing easing, long duration) {
      this.easing = easing;
      this.duration = duration;
      this.startTime = System.currentTimeMillis();
      this.value = 0.0F;
      this.startValue = 0.0F;
      this.targetValue = 0.0F;
   }

   public Animation(Easing easing, long duration, float initialValue) {
      this(easing, duration);
      this.setValue(initialValue);
   }

   public float run(float destination) {
      if (this.targetValue != destination) {
         this.targetValue = destination;
         this.startValue = this.value;
         this.startTime = System.currentTimeMillis();
      }

      if (this.isFinished()) {
         this.value = destination;
         return this.value;
      }

      double progress = this.getProgress();
      double easedProgress = this.easing.getFunction().apply(progress);
      this.value = (float)(this.startValue + (this.targetValue - this.startValue) * easedProgress);
      return this.value;
   }

   public double getProgress() {
      long elapsed = System.currentTimeMillis() - this.startTime;
      if (this.duration == 0L) {
         return 1.0;
      }

      double progress = (double)elapsed / this.duration;
      return Math.min(1.0, Math.max(0.0, progress));
   }

   public boolean isFinished() {
      return this.getProgress() >= 1.0;
   }

   public void setValue(float value) {
      this.value = value;
      this.startValue = value;
      this.targetValue = value;
      this.startTime = 0L;
   }

   public Easing getEasing() {
      return this.easing;
   }

   public long getDuration() {
      return this.duration;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public float getStartValue() {
      return this.startValue;
   }

   public float getTargetValue() {
      return this.targetValue;
   }

   public float getValue() {
      return this.value;
   }

   public void setEasing(Easing easing) {
      this.easing = easing;
   }

   public void setDuration(long duration) {
      this.duration = duration;
   }

   public void setStartTime(long startTime) {
      this.startTime = startTime;
   }

   public void setStartValue(float startValue) {
      this.startValue = startValue;
   }

   public void setTargetValue(float targetValue) {
      this.targetValue = targetValue;
   }
}
