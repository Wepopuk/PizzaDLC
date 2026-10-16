package pizza.dlc.client.modules.impl.visuals.animatedhands;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.client.modules.impl.visuals.animatedhands.lua.LuaEasings;
import pizza.dlc.client.modules.impl.visuals.animatedhands.lua.LuaItem;
import pizza.dlc.client.modules.impl.visuals.animatedhands.lua.LuaMath;
import pizza.dlc.client.modules.impl.visuals.animatedhands.lua.LuaMisc;
import pizza.dlc.client.modules.impl.visuals.animatedhands.lua.LuaParticles;
import pizza.dlc.client.modules.impl.visuals.animatedhands.lua.LuaPlayer;
import pizza.dlc.client.modules.impl.visuals.animatedhands.lua.LuaRegistry;

public final class HmiRuntime {
   public static final Map<String, Object> REGISTRY = new HashMap<>();
   public static final Map<String, Object> RENDER_AS_BLOCK = new HashMap<>();
   public static final Map<String, Object> TRANSLATE_ITEM = new HashMap<>();
   public static final Map<String, Object> APPLY_BLOCK_ROTATION = new HashMap<>();
   public static final Map<String, Object> ITEM_SWING_SPEED = new HashMap<>();
   public static final Map<String, Object> USE_DURATION = new HashMap<>();
   public static final Map<String, Object> USING_ITEM = new HashMap<>();
   public static ItemStack mainHandOverride = ItemStack.EMPTY;
   public static ItemStack offHandOverride = ItemStack.EMPTY;
   static final HmiContext CONTEXT = new HmiContext();
   private static final int DEFAULT_SWING_TICKS = 10;
   private static final float MAX_DELTA = 0.05F;
   private static final Pattern GLOBAL_DECL = Pattern.compile("global\\.(\\w+)\\s*=\\s*([^;]+)\\s*;");
   private static final Pattern TEMPLATE = Pattern.compile("\\$\\{(\\w+)\\}");
   private static final String[] SCRIPTS = {"hand_pose", "hand_relative_pose", "item_pose"};
   private static final Script[] LOADED = new Script[SCRIPTS.length];
   private static String loadedKey;
   private static float deltaTime;
   private static long lastFrameNanos;

   private HmiRuntime() {
   }

   public static float deltaTime() {
      return deltaTime;
   }

   static void beginFrame() {
      long now = System.nanoTime();
      float dt = lastFrameNanos == 0L ? 0.0F : (now - lastFrameNanos) / 1.0E9F;
      lastFrameNanos = now;
      deltaTime = Minecraft.getInstance().isPaused() ? 0.0F : Math.min(MAX_DELTA, dt);
   }

   static boolean ensureLoaded(String key, Map<String, String> vars) {
      if (key.equals(loadedKey)) return LOADED[0] != null;
      loadedKey = key;
      reload(vars);
      return LOADED[0] != null;
   }

   static void invalidate() {
      loadedKey = null;
   }

   static String loadedKey() {
      return loadedKey;
   }

   static boolean hasScripts() {
      return LOADED[0] != null;
   }

   static void runHand() {
      run(0);
   }

   static void runHandRelative() {
      run(1);
   }

   static void runItem() {
      run(2);
   }

   private static void run(int index) {
      Script script = LOADED[index];
      if (script != null) script.run();
   }

   public static boolean flag(Map<String, Object> map, String key, boolean fallback) {
      Object value = map.get(key);
      return value instanceof Boolean b ? b : fallback;
   }

   public static int swingTicks(ItemStack stack) {
      Object value = ITEM_SWING_SPEED.get(stack.getItem().toString());
      return value instanceof Number n ? Math.max(1, n.intValue()) : DEFAULT_SWING_TICKS;
   }

   public static Float useDuration(ItemStack stack) {
      Object value = USE_DURATION.get(stack.getItem().toString());
      return value instanceof Number n ? n.floatValue() : null;
   }

   public static Boolean usingItem(ItemStack stack) {
      Object value = USING_ITEM.get(stack.getItem().toString());
      return value instanceof Boolean b ? b : null;
   }

   private static void reload(Map<String, String> vars) {
      REGISTRY.clear();
      RENDER_AS_BLOCK.clear();
      TRANSLATE_ITEM.clear();
      APPLY_BLOCK_ROTATION.clear();
      USE_DURATION.clear();
      USING_ITEM.clear();
      Globals globals = createGlobals();
      for (int i = 0; i < SCRIPTS.length; i++) {
         LOADED[i] = null;
         String source = readScript(SCRIPTS[i]);
         if (source == null) {
            PizzaDLC.LOGGER.warn("[AnimatedHands] script {} not found", SCRIPTS[i]);
            continue;
         }
         try {
            LOADED[i] = new Script(SCRIPTS[i], globals.load(preprocess(source, vars), "@" + SCRIPTS[i] + ".lua"));
         } catch (RuntimeException ex) {
            PizzaDLC.LOGGER.error("[AnimatedHands] failed to compile {}", SCRIPTS[i], ex);
         }
      }
   }

   private static Globals createGlobals() {
      Globals g = new Globals();
      g.load(new JseBaseLib());
      g.load(new PackageLib());
      g.load(new Bit32Lib());
      g.load(new TableLib());
      g.load(new StringLib());
      g.load(new CoroutineLib());
      g.load(new JseMathLib());
      LoadState.install(g);
      LuaC.install(g);
      g.set("M", CoerceJavaToLua.coerce(new LuaMath()));
      g.set("I", CoerceJavaToLua.coerce(new LuaItem()));
      g.set("P", CoerceJavaToLua.coerce(new LuaPlayer()));
      g.set("S", CoerceJavaToLua.coerce(new LuaMisc.Sound()));
      g.set("C", CoerceJavaToLua.coerce(new LuaMisc.Camera()));
      g.set("KeyBindManager", CoerceJavaToLua.coerce(new LuaMisc.Keys()));
      g.set("debugger", CoerceJavaToLua.coerce(new LuaMisc.Debug()));
      g.set("Easings", CoerceJavaToLua.coerce(new LuaEasings()));
      g.set("Items", CoerceJavaToLua.coerce(new LuaRegistry.Items()));
      g.set("Tags", CoerceJavaToLua.coerce(new LuaRegistry.Tags()));
      g.set("Texture", CoerceJavaToLua.coerce(new LuaRegistry.Texture()));
      g.set("particleManager", CoerceJavaToLua.coerce(new LuaParticles()));
      g.set("swingSpeed", LuaValue.valueOf(9));
      g.set("registry", CoerceJavaToLua.coerce(REGISTRY));
      g.set("renderAsBlock", CoerceJavaToLua.coerce(RENDER_AS_BLOCK));
      g.set("translateItem", CoerceJavaToLua.coerce(TRANSLATE_ITEM));
      g.set("itemSwingSpeed", CoerceJavaToLua.coerce(ITEM_SWING_SPEED));
      g.set("applyBlockRotation", CoerceJavaToLua.coerce(APPLY_BLOCK_ROTATION));
      g.set("useDuration", CoerceJavaToLua.coerce(USE_DURATION));
      g.set("usingItem", CoerceJavaToLua.coerce(USING_ITEM));
      g.set("context", CoerceJavaToLua.coerce(CONTEXT));
      return g;
   }

   private static String readScript(String name) {
      Identifier id = Identifier.fromNamespaceAndPath("pizzadlc", "animatedhands/" + name + ".lua");
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
      if (resource.isEmpty()) return null;
      try (InputStream in = resource.get().open()) {
         return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("[AnimatedHands] failed to read {}", id, ex);
         return null;
      }
   }

   static String preprocess(String source, Map<String, String> vars) {
      Set<String> persisted = new LinkedHashSet<>();
      StringBuilder out = new StringBuilder(source.length() + 1024);
      for (String line : source.split("\\r?\\n")) {
         Matcher template = TEMPLATE.matcher(line);
         StringBuilder filled = new StringBuilder();
         while (template.find()) {
            template.appendReplacement(filled, Matcher.quoteReplacement(vars.getOrDefault(template.group(1), "0")));
         }
         template.appendTail(filled);
         Matcher decl = GLOBAL_DECL.matcher(filled);
         StringBuilder declared = new StringBuilder();
         while (decl.find()) {
            String name = decl.group(1);
            persisted.add(name);
            String replacement = "local " + name + " = registry:getOrDefault('" + name + "', " + decl.group(2).trim() + ")";
            decl.appendReplacement(declared, Matcher.quoteReplacement(replacement));
         }
         decl.appendTail(declared);
         out.append(declared).append('\n');
      }
      if (!persisted.isEmpty()) {
         out.append("\n-- PERSIST VARIABLES\n");
         for (String name : persisted) {
            out.append("registry:put('").append(name).append("', ").append(name).append(")\n");
         }
      }
      return out.toString();
   }

   private static final class Script {
      private final String name;
      private final LuaValue chunk;
      private boolean failed;

      Script(String name, LuaValue chunk) {
         this.name = name;
         this.chunk = chunk;
      }

      void run() {
         if (this.failed) return;
         try {
            this.chunk.call();
         } catch (RuntimeException ex) {
            this.failed = true;
            PizzaDLC.LOGGER.error("[AnimatedHands] runtime error in {}", this.name, ex);
            NotificationManager.add("AnimatedHands", "Ошибка в скрипте " + this.name + ": " + ex.getMessage(), NotificationType.ERROR, 5000L);
         }
      }
   }
}
