package pizza.dlc.api.util.render.streamproof;

import net.minecraft.util.Mth;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;
import pizza.dlc.api.rotation.Angle;

public final class CleanRenderScope implements AutoCloseable {
   private static final ThreadLocal<CleanRenderScope> CURRENT = new ThreadLocal<>();
   private static final Set<AbstractWidget> CLIENT_WIDGETS = Collections.newSetFromMap(new WeakHashMap<>());
   private static final Set<AbstractWidget> VANILLA_MENU_WIDGETS = Collections.newSetFromMap(new WeakHashMap<>());
   private static final Set<AbstractWidget> CLIENT_MENU_WIDGETS = Collections.newSetFromMap(new WeakHashMap<>());
   private final CleanRenderScope previous;
   private final Angle previousRotation;
   private final Angle rotation;

   public CleanRenderScope(Angle previousRotation, Angle rotation) {
      this.previous = CURRENT.get();
      this.previousRotation = previousRotation;
      this.rotation = rotation;
      CURRENT.set(this);
   }

   public static boolean isActive() {
      return CURRENT.get() != null;
   }

   public static void hideWidget(AbstractWidget widget) {
      CLIENT_WIDGETS.add(widget);
   }

   public static boolean hidesWidget(AbstractWidget widget) {
      if (VANILLA_MENU_WIDGETS.contains(widget)) return !isActive();
      if (CLIENT_MENU_WIDGETS.contains(widget)) return isActive();
      return (isActive() || StreamProofRenderer.isVanillaGameplayActive()) && isClientWidget(widget);
   }

   public static void vanillaMenuWidget(AbstractWidget widget) { VANILLA_MENU_WIDGETS.add(widget); }

   public static void clientMenuWidget(AbstractWidget widget) {
      CLIENT_MENU_WIDGETS.add(widget);
      hideWidget(widget);
   }

   public static boolean hidesInputWidget(AbstractWidget widget) {
      return VANILLA_MENU_WIDGETS.contains(widget) ? !StreamProofRenderer.isVanillaGameplayActive()
         : isClientWidget(widget) && StreamProofRenderer.isVanillaGameplayActive();
   }

   public static boolean isClientWidget(AbstractWidget widget) {
      return CLIENT_WIDGETS.contains(widget);
   }

   public static float yaw(float original, float partialTick) {
      CleanRenderScope scope = CURRENT.get();
      return scope == null || scope.rotation == null ? original
         : Mth.rotLerp(partialTick, scope.previousRotation.getYaw(), scope.rotation.getYaw());
   }

   public static float pitch(float original, float partialTick) {
      CleanRenderScope scope = CURRENT.get();
      return scope == null || scope.rotation == null ? original
         : Mth.lerp(partialTick, scope.previousRotation.getPitch(), scope.rotation.getPitch());
   }

   @Override
   public void close() {
      if (this.previous == null) CURRENT.remove();
      else CURRENT.set(this.previous);
   }
}
