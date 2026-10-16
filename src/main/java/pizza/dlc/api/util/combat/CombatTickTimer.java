package pizza.dlc.api.util.combat;
import pizza.dlc.api.util.combat.CombatRuntime;

public class CombatTickTimer {
   private int f_25015 = 0;

   public void m_7442() {
      this.f_25015 = CombatRuntime.worldTicks;
   }

   public boolean m_7443(int var1) {
      return CombatRuntime.worldTicks - this.f_25015 >= var1;
   }
}
