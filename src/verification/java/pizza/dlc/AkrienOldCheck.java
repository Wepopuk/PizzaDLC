package pizza.dlc;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.util.render.AkrienOldRenderType;
import pizza.dlc.api.util.render.AkrienOldScope;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.ModuleManager;
import pizza.dlc.client.modules.impl.visuals.Esp;

public final class AkrienOldCheck {
   private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
   private static Object allocate(Class<?> type) throws Exception {
      var unsafe = Class.forName("sun.misc.Unsafe"); var field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
      return unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
   }
   private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
      var field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
   }
   private static Object get(Object target, String name) throws Exception {
      var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
   }
   public static void main(String[] args) throws Exception {
      net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap();
      var mc = (Minecraft)allocate(Minecraft.class); set(Minecraft.class, null, "instance", mc);
      var pizzadlc = new PizzaDLC(); set(PizzaDLC.class, null, "instance", pizzadlc);
      pizzadlc.getBus().registerLambdaFactory("pizza.dlc", (lookup, type) -> MethodHandles.privateLookupIn(type, MethodHandles.lookup()));
      var esp = new Esp(); var draw = (MultiModeSetting)get(esp, "draw"); var targets = (MultiModeSetting)get(esp, "targets");
      var player = (LocalPlayer)allocate(LocalPlayer.class); mc.player = player;
      var other = (LocalPlayer)allocate(LocalPlayer.class);
      check(!draw.get("Akrien OLD").isEnabled(), "New draw mode unexpectedly enabled by default");
      esp.setEnabled(true); draw.get("Akrien OLD").setEnabled(true);
      check(esp.acceptsAkrienOld(other), "Other player was excluded");
      check(!esp.acceptsAkrienOld((ItemEntity)allocate(ItemEntity.class)), "Dropped item was treated as a player");
      targets.get("Игроки").setEnabled(false); check(!esp.acceptsAkrienOld(other), "Player target filter ignored"); targets.get("Игроки").setEnabled(true);
      var options = (Options)allocate(Options.class); set(Minecraft.class, mc, "options", options);
      Field camera = Arrays.stream(Options.class.getDeclaredFields()).filter(field -> field.getType() == CameraType.class).findFirst().orElseThrow();
      camera.setAccessible(true); camera.set(options, CameraType.FIRST_PERSON);
      targets.get("Себя").setEnabled(true); check(!esp.acceptsAkrienOld(player), "First-person self was included");
      camera.set(options, CameraType.THIRD_PERSON_BACK); check(esp.acceptsAkrienOld(player), "Selected third-person self was excluded");
      var saved = draw.save(); draw.get("Akrien OLD").setEnabled(false); draw.load(saved); check(draw.get("Akrien OLD").isEnabled(), "Mode was not persisted");
      var client = new PizzaDLCClient(); set(PizzaDLCClient.class, null, "instance", client);
      var modules = new ModuleManager(); set(PizzaDLCClient.class, client, "moduleManager", modules);
      modules.registerModules(esp);
      check(Esp.shouldRenderAkrienOld(other), "Extraction did not enable selected player");
      try (var clean = new CleanRenderScope(null, null)) { check(!Esp.shouldRenderAkrienOld(other), "Model ESP leaked into clean capture"); }
      esp.setEnabled(false); check(!Esp.shouldRenderAkrienOld(other), "Disabled ESP retained through-wall pass");
      try (var outer = new AkrienOldScope(true)) {
         try (var nested = new AkrienOldScope(false)) { check(!AkrienOldScope.active(), "Nested normal entity inherited through-wall rendering"); }
         check(AkrienOldScope.active(), "Nested scope did not restore player rendering");
      }
      check(!AkrienOldScope.active(), "Render scope leaked into following entities");
      try { try (var failed = new AkrienOldScope(true)) { throw new IllegalStateException(); } } catch (IllegalStateException expected) { }
      check(!AkrienOldScope.active(), "Render failure leaked through-wall mode");
      int pipelines = 0;
      for (var field : RenderPipelines.class.getDeclaredFields()) {
         if (field.getType() != RenderPipeline.class || !(field.getName().contains("ENTITY") || field.getName().contains("ARMOR") || field.getName().contains("GLINT"))) continue;
         field.setAccessible(true); var original = (RenderPipeline)field.get(null);
         var copy = AkrienOldRenderType.copyPipeline(original, Identifier.fromNamespaceAndPath("pizzadlc", "test/" + pipelines++));
         check(copy.getDepthStencilState() == null, "Through-wall pipeline retains world depth");
         check(copy.getVertexShader().equals(original.getVertexShader()) && copy.getFragmentShader().equals(original.getFragmentShader()), "Original texture shader changed");
         check(copy.getShaderDefines().equals(original.getShaderDefines()), "Shader alpha/lighting definitions changed");
         check(copy.getBindGroupLayouts().equals(original.getBindGroupLayouts()), "Lightmap/overlay bindings changed");
         check(Arrays.equals(copy.getColorTargetStates(), original.getColorTargetStates()), "Blending/color masks changed");
         check(Arrays.equals(copy.getVertexFormatBindings(), original.getVertexFormatBindings()), "Vertex layout changed");
         check(copy.getPrimitiveTopology() == original.getPrimitiveTopology() && copy.isCull() == original.isCull() && copy.getPolygonMode() == original.getPolygonMode(), "Geometry state changed");
      }
      check(pipelines >= 10, "Too few native player/armor/glint pipelines checked");
      System.out.println("Akrien OLD: player/self filters, saved setting, clean capture isolation, scoped exception recovery and " + pipelines + " original texture/armor/glint pipelines passed.");
   }
}
