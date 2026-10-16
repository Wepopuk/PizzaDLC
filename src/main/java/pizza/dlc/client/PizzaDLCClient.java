package pizza.dlc.client;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.render.combat.CombatHudText;


import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import com.google.gson.JsonObject;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.CollectSubmits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.account.AccountSwitcher;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.config.Config;
import pizza.dlc.api.config.ConfigManager;
import pizza.dlc.api.draggable.DraggableManager;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.input.EventInput;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.filemanager.impl.AltAccountManager;
import pizza.dlc.api.macros.Macros;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.util.RuntimeStats;
import pizza.dlc.api.util.WindowTheme;
import pizza.dlc.api.util.render.ProjectionUtil;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.api.waypoints.Waypoints;
import pizza.dlc.api.waypoints.WaypointsManager;
import pizza.dlc.client.command.CommandManager;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.ModuleManager;
import pizza.dlc.client.render.NotificationRenderer;

public class PizzaDLCClient implements ClientModInitializer {
   private static PizzaDLCClient instance;
   private DraggableManager draggableManager;
   private ModuleManager moduleManager;
   private ConfigManager configManager;
   private CommandManager commandManager;
   private Macros macros;
   private WaypointsManager waypointsManager;
   private pizza.dlc.api.waypoints.GpsNavigation gpsNavigation;
   private JsonObject defaultConfigSnapshot;
   private boolean configLoaded;

   public void onInitializeClient() {
      instance = this;
      pizza.dlc.api.translation.TranslationManager.getInstance();
      this.draggableManager = new DraggableManager();
      this.moduleManager = new ModuleManager().init();
      pizza.dlc.api.util.combat.CombatRuntime.initialize();
      new pizza.dlc.api.neuro.NeuroSystem();
      this.configManager = new ConfigManager();
      this.defaultConfigSnapshot = Config.captureState();
      this.macros = new Macros();
      this.waypointsManager = new WaypointsManager();
      this.waypointsManager.load();
      this.gpsNavigation = new pizza.dlc.api.waypoints.GpsNavigation();
      PizzaDLC.getInstance().getBus().subscribe(this.gpsNavigation);
      this.commandManager = new CommandManager().init();
      registerBuiltinPacks();
      ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(
         Identifier.fromNamespaceAndPath("pizzadlc", "armor_cit"), pizza.dlc.api.resource.ArmorCitRegistry.INSTANCE);
      RuntimeStats.flush();
      PizzaDLC.getInstance().getBus().subscribe(this);
      PizzaDLC.getInstance().getBus().subscribe(new NotificationRenderer());
      PizzaDLC.getInstance().getBus().subscribe(new RotationController());
      PizzaDLC.getInstance().getBus().subscribe(new Waypoints());
      LevelRenderEvents.COLLECT_SUBMITS.register((CollectSubmits)ctx -> {
         if (CleanRenderScope.isActive() || StreamProofRenderer.isVanillaGameplayActive()) return;
         CameraRenderState cam = ctx.levelState().cameraRenderState;
         ProjectionUtil.update(cam, Minecraft.getInstance().getWindow());
         PizzaDLC.getInstance().getBus().post(new EventRender3D(ctx.poseStack(), ctx.submitNodeCollector(), cam));
      });
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (this.configManager != null && this.configLoaded) {
            this.configManager.save();
         }

         if (this.macros != null) {
            this.macros.save();
         }

         if (this.waypointsManager != null) {
            this.waypointsManager.save();
         }
      }));
   }

   public void resetToDefaults() {
      if (this.defaultConfigSnapshot == null) return;

      Config.applyState(this.defaultConfigSnapshot);
      if (this.configManager != null) {
         this.configManager.clearActiveConfigName();
         this.configManager.saveConfig("autosave");
      }
   }

   @EventHandler
   public void onTick(EventTick event) {
      if (this.configLoaded) return;

      try {
         if (this.configManager != null) {
            this.configManager.loadConfig("autosave");
         }
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to load autosave config", ex);
      }

      this.configLoaded = true;
      WindowTheme.applyFromSystem();
      AltAccountManager.OfflineAlt sel = PizzaDLC.getInstance().getAltAccountManager().getSelected();
      if (sel != null && sel.premium()) {
         AccountSwitcher.loginPremium(sel.name()).whenCompleteAsync((s, err) -> {
            if (err != null) {
               NotificationManager.add("Alt Manager", AccountSwitcher.errorText(err), NotificationType.ERROR, 5000L);
            }
         }, Minecraft.getInstance());
      } else if (sel != null) {
         AccountSwitcher.applyOffline(sel.name());
      }
   }

   @EventHandler
   public void onInput(EventInput event) {
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      if (this.moduleManager != null) {
         for (Module module : this.moduleManager.getModules()) {
            if (!event.isPressed(module.getKey())) continue;

            module.toggle();
         }
      }
   }

   @EventHandler
   public void onKeyBind(EventKey event) {
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      if (this.moduleManager != null && Minecraft.getInstance().gui.screen() == null) {
         boolean press = event.getAction() != 0;

         for (Module module : this.moduleManager.getModules()) {
            for (Setting setting : module.getSettings()) {
               if (setting instanceof BooleanSetting b) {
                  this.applyCheckboxBind(b, event.getKey(), press);
               } else if (setting instanceof MultiModeSetting mm) {
                  for (BooleanSetting b : mm.getBoolSettings()) {
                     this.applyCheckboxBind(b, event.getKey(), press);
                  }
               }
            }
         }
      }
   }

   private void applyCheckboxBind(BooleanSetting b, int key, boolean press) {
      if (b.getKey() == -1 || b.getKey() != key) return;

      if (b.isHoldBind()) {
         b.setEnabled(press);
      } else if (press) {
         b.toggle();
      }
   }

   public DraggableManager getDraggableManager() {
      return this.draggableManager;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public Macros getMacros() {
      return this.macros;
   }

   public pizza.dlc.api.waypoints.GpsNavigation getGpsNavigation() { return this.gpsNavigation; }

   public WaypointsManager getWaypointsManager() {
      return this.waypointsManager;
   }

   public JsonObject getDefaultConfigSnapshot() {
      return this.defaultConfigSnapshot;
   }

   public boolean isConfigLoaded() {
      return this.configLoaded;
   }

   public static PizzaDLCClient getInstance() {
      return instance;
   }

   private static void registerBuiltinPacks() {
      FabricLoader.getInstance().getModContainer(PizzaDLC.MOD_ID).ifPresent(container -> ResourceLoader.registerBuiltinPack(
         Identifier.fromNamespaceAndPath(PizzaDLC.MOD_ID, "animatedhands_3d"), container, Component.literal("AnimatedHands: 3D-предметы"), PackActivationType.NORMAL
      ));
   }
}
