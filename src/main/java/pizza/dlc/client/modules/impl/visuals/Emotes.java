package pizza.dlc.client.modules.impl.visuals;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import pizza.dlc.api.emote.EmoteDefinition;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.EmoteWheel;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.events.impl.render.EventRender2D;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Emotes",
   category = ModuleCategory.VISUALS,
   description = "Колесо эмоций на бинд"
)
public class Emotes extends Module {
   private static final String HOLD = "Удержание";
   private static final String PRESS = "Нажатие";
   private static final String BACK = "Сзади";
   private static final String FRONT = "Спереди";
   private static final String KEEP = "Не менять";
   private static final int KEY_ESCAPE = 256;
   private static Emotes instance;
   public final BindSetting wheelBind = new BindSetting("Бинд колеса", this, -1);
   private final ModeSetting bindMode = new ModeSetting("Режим бинда", this, HOLD, PRESS);
   private final ModeSetting camera = new ModeSetting("Камера", this, BACK, FRONT, KEEP);
   private final BooleanSetting stopOnMove = new BooleanSetting("Стоп при движении", this);
   private CameraType savedCamera;
   private CameraType forcedCamera;
   private boolean stopArmed;

   public Emotes() {
      instance = this;
      this.stopOnMove.setEnabled(true);
   }

   @Override
   public void onDisable() {
      EmoteWheel.cancel();
      if (mc.player != null) {
         EmoteEngine.INSTANCE.stop(mc.player.getUUID());
      }

      this.restoreCamera();
      super.onDisable();
   }

   private static boolean active() {
      return instance != null && instance.isEnabled();
   }

   // Esc closes the wheel without picking and without opening the pause menu.
   public static boolean consumeEscape(int key, int action) {
      if (key != KEY_ESCAPE || !active() || !EmoteWheel.isOpen()) return false;

      EmoteWheel.cancel();
      return true;
   }

   // While the wheel is open: LMB picks, RMB closes, other buttons are swallowed. The wheel bind itself still goes through EventKey.
   public static boolean onWheelMouseButton(int button, int action) {
      if (!active() || !EmoteWheel.isOpen() || -100 - button == instance.wheelBind.getKey()) return false;

      if (action == 1) {
         if (button == 0) {
            instance.pick(EmoteWheel.close());
         } else if (button == 1) {
            EmoteWheel.cancel();
         }
      }

      return true;
   }

   @EventHandler
   public void onKey(EventKey e) {
      int bind = this.wheelBind.getKey();
      if (bind == -1 || e.getKey() != bind) return;

      if (e.getAction() == 1) {
         if (EmoteWheel.isOpen()) {
            if (this.bindMode.is(PRESS)) {
               this.pick(EmoteWheel.close());
            }
         } else if (mc.player != null && mc.gui.screen() == null) {
            EmoteWheel.open();
         }

         e.setCancelled(true);
      } else if (e.getAction() == 0 && this.bindMode.is(HOLD) && EmoteWheel.isOpen()) {
         this.pick(EmoteWheel.close());
         e.setCancelled(true);
      }
   }

   private void pick(int index) {
      if (mc.player == null || index == EmoteWheel.CANCELLED) return;

      EmoteDefinition emote = EmoteWheel.entry(index);
      if (emote == null) {
         EmoteEngine.INSTANCE.stop(mc.player.getUUID());
         return;
      }

      EmoteEngine.INSTANCE.play(mc.player.getUUID(), emote);
      this.stopArmed = !this.moving();
      this.applyCamera();
   }

   @EventHandler
   public void onTick(EventTick e) {
      if (EmoteWheel.isOpen() && (mc.player == null || mc.gui.screen() != null)) {
         EmoteWheel.cancel();
      }

      if (mc.player == null) {
         this.restoreCamera();
         return;
      }

      boolean playing = EmoteEngine.INSTANCE.isPlaying(mc.player.getUUID());
      // Walking into the pick doesn't cancel it; the next fresh movement input does.
      if (playing && this.stopOnMove.isEnabled()) {
         boolean moving = this.moving();
         if (!this.stopArmed) {
            this.stopArmed = !moving;
         } else if (moving) {
            EmoteEngine.INSTANCE.stop(mc.player.getUUID());
            playing = false;
         }
      }

      if (this.savedCamera != null && mc.options.getCameraType() != this.forcedCamera) {
         this.savedCamera = null;
         this.forcedCamera = null;
      }

      if (!playing) {
         this.restoreCamera();
      }
   }

   @EventHandler
   public void onRender(EventRender2D e) {
      EmoteWheel.render(e.getContext());
   }

   private boolean moving() {
      return mc.player.input.getMoveVector().lengthSquared() > 1.0E-4F || mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
   }

   private void applyCamera() {
      if (this.camera.is(KEEP)) return;

      CameraType want = this.camera.is(FRONT) ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK;
      if (this.savedCamera == null) {
         this.savedCamera = mc.options.getCameraType();
      }

      this.forcedCamera = want;
      mc.options.setCameraType(want);
   }

   private void restoreCamera() {
      if (this.savedCamera == null) return;

      if (mc.options.getCameraType() == this.forcedCamera) {
         mc.options.setCameraType(this.savedCamera);
      }

      this.savedCamera = null;
      this.forcedCamera = null;
   }
}
