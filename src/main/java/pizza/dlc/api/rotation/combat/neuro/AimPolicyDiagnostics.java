package pizza.dlc.api.rotation.combat.neuro;
import pizza.dlc.api.rotation.combat.neuro.AimPolicy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class AimPolicyDiagnostics {

   private static final String[] f_31480 = new String[]{
      "sample_dt_ms",
      "world_tick_changed",
      "world_tick_phase",
      "rel_pos_right",
      "rel_pos_up",
      "rel_pos_forward",
      "distance",
      "rel_vel_right",
      "rel_vel_up",
      "rel_vel_forward",
      "aim_error_yaw",
      "aim_error_pitch",
      "yaw_velocity",
      "pitch_velocity",
      "target_relative_yaw_sin",
      "target_relative_yaw_cos",
      "target_on_ground",
      "target_sprinting",
      "target_sneaking",
      "target_hurt_time",
      "player_on_ground",
      "player_sprinting",
      "player_sneaking",
      "input_forward",
      "input_back",
      "input_left",
      "input_right",
      "input_jump",
      "input_attack",
      "exact_crosshair"
   };

   public static void m_8779(AimPolicy var0, int var1) {
      if (!Arrays.equals(f_31480, var0.m_3599())) {
         throw new IllegalArgumentException("policy.bin has a different raw feature schema");
      }

      if (var1 != 30) {
         throw new IllegalArgumentException(var1 + "");
      }
   }

   public static Map<String, Float> m_8780(float[] var0) {
      if (var0 != null && var0.length == 30) {
         HashMap var1 = new HashMap(64);

         for (int var2 = 0; var2 < f_31480.length; var2++) {
            if (!Float.isFinite(var0[var2])) {
               throw new IllegalArgumentException(f_31480[var2] + "");
            }

            var1.put(f_31480[var2], var0[var2]);
         }

         return var1;
      } else {
         throw new IllegalArgumentException("Expected 30 raw features from Aim60FeatureExtractor");
      }
   }

   private AimPolicyDiagnostics() {
   }
}
