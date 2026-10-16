package pizza.dlc.api.util.combat;

import lombok.Generated;

public class CombatTimer {
   private long f_12394 = System.currentTimeMillis();
   private long f_12395;

   public boolean m_4457(float var1) {
      return (float)System.currentTimeMillis() - var1 >= (float)this.f_12395;
   }

   public boolean m_4458(double var1) {
      return System.currentTimeMillis() - this.f_12394 >= var1;
   }

   public void m_4459() {
      this.f_12394 = System.currentTimeMillis();
   }

   public long m_4460() {
      return System.currentTimeMillis() - this.f_12394;
   }

   public boolean m_4461(long var1, boolean var3) {
      boolean var4 = this.m_4462() >= var1;
      if (var4 && var3) {
         this.m_4459();
      }

      return var4;
   }

   public long m_4462() {
      return System.currentTimeMillis() - this.f_12394;
   }

   public boolean m_4463(long var1) {
      return this.m_4462() >= var1;
   }

   public boolean m_4464(long var1) {
      return System.currentTimeMillis() - this.f_12394 > var1;
   }

   @Generated
   public void m_4465(long var1) {
      this.f_12394 = var1;
   }

   public boolean m_4466() {
      return this.f_12394 < System.currentTimeMillis();
   }
}
