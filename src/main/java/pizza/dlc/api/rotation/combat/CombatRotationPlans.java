package pizza.dlc.api.rotation.combat;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;
import pizza.dlc.api.rotation.combat.impl.BmwAim;
import pizza.dlc.api.rotation.combat.impl.FuntimeSnapAim;
import pizza.dlc.api.rotation.combat.impl.HumanAim;
import pizza.dlc.api.rotation.combat.impl.HvhAim;
import pizza.dlc.api.rotation.combat.impl.LegitAim;
import pizza.dlc.api.rotation.combat.impl.NeuroAim;
import pizza.dlc.api.rotation.combat.impl.SlothAim;
import pizza.dlc.api.rotation.combat.impl.SmoothAim;
import pizza.dlc.api.rotation.combat.impl.SnapAim;

import java.util.ArrayList;
import lombok.Generated;

public class CombatRotationPlans extends ArrayList<CombatRotationPlan> {
   private final SmoothAim smooth = new SmoothAim(null);
   private final SnapAim snap = new SnapAim(null);
   private final BmwAim bmw = new BmwAim(null);
   private final FuntimeSnapAim funtimeSnap = new FuntimeSnapAim(null);
   private final LegitAim legit = new LegitAim(null);
   private final HvhAim hvh = new HvhAim(null);
   private final SlothAim sloth = new SlothAim(null);
   private final HumanAim human = new HumanAim(null);
   private final NeuroAim neuro = new NeuroAim(null);
   private CombatRotationPlan active = null;
   private CombatRotationPlan previous = null;
   private int priority = 0;
   private int duration = 0;
   private int elapsedTicks = 0;
   private boolean expired = false;

   @Generated
   public HumanAim human() {
      return this.human;
   }

   @Generated
   public int getElapsedTicks() {
      return this.elapsedTicks;
   }

   public boolean setActive(CombatRotationPlan var1) {
      if (this.priority > 0) {
         return false;
      }

      this.active = var1;
      return true;
   }

   @Generated
   public BmwAim bmw() {
      return this.bmw;
   }

   public boolean clear(CombatRotationPlan var1) {
      if (this.active != var1 && this.previous != var1) {
         return false;
      }

      this.active = null;
      this.previous = null;
      this.priority = 0;
      this.duration = 0;
      this.elapsedTicks = 0;
      this.expired = false;
      return true;
   }

   @Generated
   public NeuroAim neuro() {
      return this.neuro;
   }

   @Generated
   public boolean isExpired() {
      return this.expired;
   }

   public void tick() {
      if (this.previous != null && !this.expired && this.active == this.previous) {
         if (++this.elapsedTicks > this.duration) {
            this.active = null;
            this.expired = true;
         }
      }
   }

   @Generated
   public SlothAim sloth() {
      return this.sloth;
   }

   @Generated
   public HvhAim hvh() {
      return this.hvh;
   }

   @Generated
   public SnapAim snap() {
      return this.snap;
   }

   @Generated
   public LegitAim legit() {
      return this.legit;
   }

   public boolean finishExpired(CombatRotationPlan var1) {
      if (!this.hasExpired(var1)) {
         return false;
      }

      this.previous = null;
      this.priority = 0;
      this.duration = 0;
      this.elapsedTicks = 0;
      this.expired = false;
      return true;
   }

   @Generated
   public int getDuration() {
      return this.duration;
   }

   public boolean hasExpired(CombatRotationPlan var1) {
      return this.expired && this.previous == var1;
   }

   public CombatRotationPlans() {
      this.add(this.smooth);
      this.add(this.snap);
      this.add(this.sloth);
      this.add(this.bmw);
      this.add(this.funtimeSnap);
      this.add(this.legit);
      this.add(this.human);
      this.add(this.hvh);
      this.add(this.neuro);
   }

   public boolean expire(CombatRotationPlan var1) {
      if (this.active == var1 && this.previous == var1) {
         this.active = null;
         this.expired = true;
         return true;
      } else {
         return false;
      }
   }

   @Generated
   public FuntimeSnapAim funtimeSnap() {
      return this.funtimeSnap;
   }

   public boolean schedule(CombatRotationPlan var1, int var2, int var3) {
      if (this.priority > var3) {
         return false;
      }

      this.active = var1;
      this.previous = var1;
      this.priority = var3;
      this.duration = var2;
      this.elapsedTicks = -1;
      this.expired = false;
      return true;
   }

   @Generated
   public SmoothAim smooth() {
      return this.smooth;
   }

   @Generated
   public int getPriority() {
      return this.priority;
   }

   @Generated
   public CombatRotationPlan getActive() {
      return this.active;
   }

   @Generated
   public CombatRotationPlan getPrevious() {
      return this.previous;
   }
}
