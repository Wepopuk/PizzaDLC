package pizza.dlc.api.neuro;

/** Render interpolation that conserves learned yaw AND pitch when predictions overlap. */
public final class NeuroCameraMotion {
   private double yaw,pitch,seconds;
   public record Step(float yaw,float pitch) {}
   public void enqueue(float nextYaw,float nextPitch) {
      if(!Float.isFinite(nextYaw)||!Float.isFinite(nextPitch))throw new IllegalArgumentException("Non-finite camera step");
      // A new prediction must not discard the unfinished part of the previous one.
      yaw+=nextYaw;pitch+=nextPitch;seconds=.05;
   }
   public Step advance(double elapsed) {
      if(!Double.isFinite(elapsed)||elapsed<0)throw new IllegalArgumentException("Invalid frame time");
      if(seconds<=0||elapsed==0)return new Step(0,0);
      double used=Math.min(elapsed,seconds),ratio=used/seconds;
      float y=(float)(yaw*ratio),p=(float)(pitch*ratio);
      yaw-=y;pitch-=p;seconds-=used;
      if(seconds<1e-9)reset();
      return new Step(y,p);
   }
   public boolean pending(){return seconds>0;}
   public void reset(){yaw=pitch=seconds=0;}
}
