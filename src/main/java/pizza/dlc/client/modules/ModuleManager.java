package pizza.dlc.client.modules;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.render.combat.CombatHudText;
import pizza.dlc.client.modules.impl.combat.ElytraTarget;
import pizza.dlc.client.modules.impl.movement.ElytraBooster;
import pizza.dlc.client.modules.impl.movement.ElytraMotion;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import pizza.dlc.client.modules.impl.combat.AntiBot;
import pizza.dlc.client.modules.impl.combat.AttackAura;
import pizza.dlc.client.modules.impl.combat.AutoExplosion;
import pizza.dlc.client.modules.impl.combat.AutoPotion;
import pizza.dlc.client.modules.impl.combat.AutoSwap;
import pizza.dlc.client.modules.impl.combat.AutoTotem;
import pizza.dlc.client.modules.impl.combat.AutoWeb;
import pizza.dlc.client.modules.impl.combat.AnchorAura;
import pizza.dlc.client.modules.impl.combat.CrystalAura;
import pizza.dlc.client.modules.impl.combat.ElytraSample;
import pizza.dlc.client.modules.impl.combat.HitBox;
import pizza.dlc.client.modules.impl.combat.TargetPearl;
import pizza.dlc.client.modules.impl.combat.TriggerBot;
import pizza.dlc.client.modules.impl.combat.Velocity;
import pizza.dlc.client.modules.impl.misc.AntiCrash;
import pizza.dlc.client.modules.impl.misc.Assist;
import pizza.dlc.client.modules.impl.misc.Blink;
import pizza.dlc.client.modules.impl.misc.DevMode;
import pizza.dlc.client.modules.impl.misc.FixHP;
import pizza.dlc.client.modules.impl.misc.FreeCam;
import pizza.dlc.client.modules.impl.misc.ItemScroller;
import pizza.dlc.client.modules.impl.misc.NeuroRecorder;
import pizza.dlc.client.modules.impl.misc.Optimization;
import pizza.dlc.client.modules.impl.misc.ZakoOpt;
import pizza.dlc.client.modules.impl.misc.SRPSpoofer;
import pizza.dlc.client.modules.impl.misc.HitSound;
import pizza.dlc.client.modules.impl.misc.Sounds;
import pizza.dlc.client.modules.impl.misc.StreamerMode;
import pizza.dlc.client.modules.impl.misc.StreamProof;
import pizza.dlc.client.modules.impl.misc.Tweaks;
import pizza.dlc.client.modules.impl.misc.Zoom;
import pizza.dlc.client.modules.impl.movement.AirStuck;
import pizza.dlc.client.modules.impl.movement.AutoSprint;
import pizza.dlc.client.modules.impl.movement.ElytraJump;
import pizza.dlc.client.modules.impl.movement.Fly;
import pizza.dlc.client.modules.impl.movement.InventoryMove;
import pizza.dlc.client.modules.impl.movement.NoSlow;
import pizza.dlc.client.modules.impl.movement.NoWeb;
import pizza.dlc.client.modules.impl.movement.SuperFirework;
import pizza.dlc.client.modules.impl.other.ClickGui;
import pizza.dlc.client.modules.impl.player.AutoTool;
import pizza.dlc.client.modules.impl.player.ClickAction;
import pizza.dlc.client.modules.impl.player.ElytraHelper;
import pizza.dlc.client.modules.impl.player.FastBreak;
import pizza.dlc.client.modules.impl.player.ItemFinder;
import pizza.dlc.client.modules.impl.player.LoginFix;
import pizza.dlc.client.modules.impl.player.NoDelay;
import pizza.dlc.client.modules.impl.player.NoFall;
import pizza.dlc.client.modules.impl.player.NoPush;
import pizza.dlc.client.modules.impl.player.SwapSettings;
import pizza.dlc.client.modules.impl.themes.Themes;
import pizza.dlc.client.modules.impl.visuals.Ambience;
import pizza.dlc.client.modules.impl.visuals.ArmorDurability;
import pizza.dlc.client.modules.impl.visuals.Arrows;
import pizza.dlc.client.modules.impl.visuals.BlockEsp;
import pizza.dlc.client.modules.impl.visuals.Chams;
import pizza.dlc.client.modules.impl.visuals.ChorusRadius;
import pizza.dlc.client.modules.impl.visuals.CustomModels;
import pizza.dlc.client.modules.impl.visuals.CustomSky;
import pizza.dlc.client.modules.impl.visuals.Emotes;
import pizza.dlc.client.modules.impl.visuals.Esp;
import pizza.dlc.client.modules.impl.visuals.Highlight;
import pizza.dlc.client.modules.impl.visuals.Hud;
import pizza.dlc.client.modules.impl.visuals.JumpCircle;
import pizza.dlc.client.modules.impl.visuals.Particles;
import pizza.dlc.client.modules.impl.visuals.Predictions;
import pizza.dlc.client.modules.impl.visuals.Removals;
import pizza.dlc.client.modules.impl.visuals.Saturation;
import pizza.dlc.client.modules.impl.visuals.SeeInvisibles;
import pizza.dlc.client.modules.impl.visuals.ShaderESP;
import pizza.dlc.client.modules.impl.visuals.TargetEsp;
import pizza.dlc.client.modules.impl.visuals.Trails;
import pizza.dlc.client.modules.impl.visuals.ViewModel;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;

public class ModuleManager {
   private List<Module> modules = new ArrayList<>();
   private final Map<Class<? extends Module>, Module> moduleByClass = new HashMap<>();
   private final Map<String, Module> moduleByName = new HashMap<>();

   public ModuleManager init() {
      this.registerModules(
         new pizza.dlc.client.modules.impl.combat.ElytraTarget(),
         new pizza.dlc.client.modules.impl.movement.ElytraMotion(),
         new pizza.dlc.client.modules.impl.movement.ElytraBooster(),
         new AutoSprint(),
         new Fly(),
         new NoSlow(),
         new InventoryMove(),
         new AirStuck(),
         new NoWeb(),
         new Assist(),
         new Hud(),
         new Esp(),
         new ShaderESP(),
         new Chams(),
         new Saturation(),
         new TargetEsp(),
         new BlockEsp(),
         new ViewModel(),
         new AnimatedHands(),
         new Particles(),
         new JumpCircle(),
         new Trails(),
         new ChorusRadius(),
         new CustomModels(),
         new Emotes(),
         new CustomSky(),
         new Arrows(),
         new Ambience(),
         new Predictions(),
         new Highlight(),
         new SeeInvisibles(),
         new ArmorDurability(),
         new ClickGui(),
         new Themes(),
         new Sounds(),
         new HitSound(),
         new StreamerMode(),
         new StreamProof(),
         new Tweaks(),
         new SRPSpoofer(),
         new Zoom(),
         new FreeCam(),
         new Blink(),
         new NeuroRecorder(),
         new FixHP(),
         new DevMode(),
         new Optimization(),
         new ZakoOpt(),
         new ItemScroller(),
         new LoginFix(),
         new SwapSettings(),
         new ClickAction(),
         new NoDelay(),
         new Removals(),
         new AntiBot(),
         new TriggerBot(),
         new AntiCrash(),
         new AttackAura(),
         new AutoExplosion(),
         new AutoWeb(),
         new CrystalAura(),
         new AnchorAura(),
         new AutoSwap(),
         new AutoTotem(),
         new AutoPotion(),
         new TargetPearl(),
         new HitBox(),
         new Velocity(),
         new ElytraSample(),
         new ElytraHelper(),
         new ElytraJump(),
         new SuperFirework(),
         new NoPush(),
         new NoFall(),
         new AutoTool(),
         new FastBreak(),
         new ItemFinder()
      );
      return this;
   }

   public void registerModules(@NonNull Module... modules) {
      if (modules == null) {
         throw new NullPointerException("modules is marked non-null but is null");
      }

      this.modules.addAll(Arrays.asList(modules));

      for (Module module : modules) {
         this.moduleByClass.put((Class<? extends Module>)module.getClass(), module);
         this.moduleByName.put(module.getName().toLowerCase(), module);
         if (module instanceof AnimatedHands) this.moduleByName.put("animatedhands", module);
      }

      this.modules = Collections.unmodifiableList(this.modules);
   }

   public <T extends Module> T get(@NonNull Class<T> clazz) {
      Objects.requireNonNull(clazz, "Module class cannot be null");
      return (T)this.moduleByClass.get(clazz);
   }

   public <T extends Module> T get(@NonNull String name) {
      Objects.requireNonNull(name, "Module name cannot be null");
      return (T)this.moduleByName.get(name.toLowerCase());
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public Map<Class<? extends Module>, Module> getModuleByClass() {
      return this.moduleByClass;
   }

   public Map<String, Module> getModuleByName() {
      return this.moduleByName;
   }
}
