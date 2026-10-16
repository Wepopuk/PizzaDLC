package pizza.dlc.api.neuro;

public final class NeuroSchema {
   public static final int VERSION=1, LENGTH=16, CHANNELS=24;
   public static final String[] NAMES={"dt_ms","rel_forward","rel_right","rel_up","distance","target_width","target_height","error_yaw","error_pitch",
      "player_forward_bps","player_right_bps","player_up_bps","target_forward_bps","target_right_bps","target_up_bps","radial_bps","relative_strafe_bps",
      "yaw_sin","yaw_cos","pitch","last_dyaw","last_dpitch","prev_dyaw","prev_dpitch","visible","blocked","attack_cooldown","since_attack_s",
      "previous_attack","previous_local_hit","previous_miss","target_changed","ray_u","ray_v","ray_depth","on_ground","sprint"};
   public static final float[] SCALES={50,12,12,6,12,2,3,180,90,10,10,10,10,10,10,15,15,1,1,90,30,30,30,30,1,1,1,5,1,1,1,1,1,1,1,1,1};
   private NeuroSchema() {}
   public static float wrap(float angle) { angle%=360;return angle>=180?angle-360:angle< -180?angle+360:angle; }
   public static float pitch(float angle) { return Math.clamp(angle,-90,90); }
   public static float[] normalize(float[] input) {
      if(input.length!=NAMES.length) throw new IllegalArgumentException("Feature schema mismatch");
      var out=input.clone();for(int i=0;i<out.length;i++) { if(!Float.isFinite(out[i]))throw new IllegalArgumentException("Non-finite feature "+NAMES[i]);out[i]=Math.clamp(out[i]/SCALES[i],-10,10); }return out;
   }
}
