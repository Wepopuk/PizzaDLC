package pizza.dlc;
import pizza.dlc.api.events.impl.combat.RocketBoostEvent;
import pizza.dlc.api.interfaces.ResolvedRotationAccess;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.util.render.combat.CombatHudText;


import com.google.gson.JsonObject;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.ModuleManager;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.client.modules.impl.combat.AttackAura;
import pizza.dlc.client.modules.impl.combat.ElytraTarget;
import pizza.dlc.client.modules.impl.movement.ElytraMotion;
import pizza.dlc.client.modules.impl.movement.ElytraBooster;
import pizza.dlc.mixin.client.LivingEntityGravityAccessor;
/** Executes the imported configuration and flight logic against controllable players without a GPU or connection. */
public final class CombatModulesCheck {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void near(Vec3 actual, Vec3 expected, String message) { check(actual.distanceTo(expected) < 1e-6, message + ": " + actual + " vs " + expected); }
    private static Object allocate(Class<?> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe"); singleton.setAccessible(true);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type);
    }
    private static void set(Class<?> owner, Object instance, String fieldName, Object value) throws Exception {
        Field field = owner.getDeclaredField(fieldName); field.setAccessible(true); field.set(instance, value);
    }
    private static Setting setting(pizza.dlc.client.modules.Module module, String name) {
        return module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow(() -> new AssertionError(name));
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var minecraft = (Minecraft) allocate(Minecraft.class);
        set(Minecraft.class, null, "instance", minecraft);
        set(Minecraft.class, minecraft, "gameDirectory", Path.of("build/combat-check").toFile());
        var player = (FlightPlayer) allocate(FlightPlayer.class);
        player.place(Vec3.ZERO); player.velocity = Vec3.ZERO; player.flying = true;
        minecraft.player = player;
        minecraft.level = (ClientLevel) allocate(ClientLevel.class);
        var pizzadlc = new PizzaDLC();
        set(PizzaDLC.class, null, "instance", pizzadlc);
        pizzadlc.getBus().registerLambdaFactory("pizza.dlc", (lookup, type) -> MethodHandles.privateLookupIn(type, MethodHandles.lookup()));
        var client = new PizzaDLCClient();
        set(PizzaDLCClient.class, null, "instance", client);
        var manager = new ModuleManager();
        set(PizzaDLCClient.class, client, "moduleManager", manager);
        var aura = new AttackAura(); var target = new ElytraTarget(); var motion = new ElytraMotion(); var booster = new ElytraBooster();
        manager.registerModules(aura, target, motion, booster);
        CombatRuntime.initialize();
        check(aura.getName().equals("Attack Aura"), "AttackAura registration name");
        var managerCode = new ClassNode();
        new ClassReader(ModuleManager.class.getName()).accept(managerCode, 0);
        long auraRegistrations = managerCode.methods.stream().filter(method -> method.name.equals("init"))
            .flatMap(method -> Arrays.stream(method.instructions.toArray()))
            .filter(instruction -> instruction instanceof TypeInsnNode type && type.getOpcode() == org.objectweb.asm.Opcodes.NEW
                && type.desc.equals(AttackAura.class.getName().replace('.', '/'))).count();
        check(auraRegistrations == 1, "Attack Aura must be registered exactly once");
        var configs = new JsonObject();
        var removed = new JsonObject(); removed.addProperty("key", 42); removed.addProperty("enabled", false);
        var removedSettings = new JsonObject(); removedSettings.addProperty("Поворот", "Polar"); removed.add("Settings", removedSettings);
        configs.add("Attack Aura", removed);
        var imported = aura.save(); configs.add("KillAura", imported);
        check(pizza.dlc.api.config.Config.moduleState(configs, aura.getName()) == imported, "Imported aura config lost on rename");
        var current = aura.save(); configs.add("Attack Aura", current);
        check(pizza.dlc.api.config.Config.moduleState(configs, aura.getName()) == current, "Current aura config must win over legacy name");
        configs.remove("KillAura"); configs.add("Attack Aura", removed);
        var migrated = pizza.dlc.api.config.Config.moduleState(configs, aura.getName());
        check(migrated.get("key").getAsInt() == 42 && !migrated.has("Settings"), "Removed aura bind or settings migration incorrect");
        check(removed.has("Settings"), "Config migration mutated the input");

        check(!aura.isEnabled() && !target.isEnabled() && !motion.isEnabled() && !booster.isEnabled(), "Imported modules auto-enabled");
        check(aura.getRotationMode().getModes().equals(List.of("Smooth", "Snap", "Sloth", "FuntimeSnap", "BMW", "Legit", "HvH", "Neuro")), "Rotation modes lost");
        for (var module : List.of(aura, target, motion, booster)) {
            var keys = new HashSet<String>();
            for (var option : module.getSettings()) {
                check(option.getParent() == module, "Setting attached to wrong module");
                check(keys.add(option.configKey()), "Duplicate persisted setting: " + module.getName() + "/" + option.configKey());
                check(option instanceof CombatSliderSetting || option instanceof BooleanSetting || option instanceof ModeSetting
                    || option instanceof pizza.dlc.api.settings.impl.MultiModeSetting
                    || option instanceof pizza.dlc.api.settings.impl.ColorSetting
                    || option instanceof pizza.dlc.api.settings.impl.StringSetting, "Unsupported GUI setting " + option.getClass());
            }
            var saved = module.save();
            module.load(saved);
            var restored = module.save();
            for (String key : saved.keySet()) check(saved.get(key).equals(restored.get(key)), "Config round trip changed " + module.getName() + "/" + key + ": " + saved.get(key) + " -> " + restored.get(key));
        }
        var rotations = CombatRotations.plans();
        rotations.schedule(rotations.smooth(), 1, 6);
        rotations.getActive().setMovementCorrection(true);
        CombatRotations.state().getClientRotation().set(30,15);
        player.setYRot(80); player.setXRot(45);
        check(CombatRuntime.cleanCameraRotation(false).getYaw() == 30, "Clean camera exposes combat rotation");
        check(CombatRuntime.cleanCameraRotation(true).getYaw() == 80, "Clean camera rotation option ignored");
        CombatRuntime.disabled(aura);
        check(rotations.getActive() == null, "Disabling before first rotation left a pending plan");

        // All four server profiles must produce finite and periodic coefficients, including every angle boundary.
        var profile = (ModeSetting)setting(booster, "Мод");
        int combinations = 0;
        for (String mode : profile.getModes()) {
            profile.setCurrentMode(mode);
            for (float yaw : new float[]{-180,-135,-90,-45,-1,0,1,5,10,20,30,40,45,60,80,90,135,180,360}) {
                for (float pitch : new float[]{-90,-45,-1,0,1,5,10,20,30,40,45,60,80,90}) {
                    player.setYRot(yaw); player.setXRot(pitch);
                    var event = new RocketBoostEvent(player, 1.5F, 1.5F, 1.5F);
                    booster.onRocketBoost(event);
                    check(Float.isFinite(event.getXPower()) && event.getXPower() > 0 && event.getXPower() < 20, "Invalid horizontal boost " + mode);
                    check(Float.isFinite(event.getYPower()) && event.getYPower() > 0 && event.getYPower() < 20, "Invalid vertical boost " + mode);
                    check(event.getXPower() == event.getZPower(), "Horizontal axes disagree");
                    player.setYRot(yaw + 360);
                    var repeated = new RocketBoostEvent(player, 1.5F, 1.5F, 1.5F);
                    booster.onRocketBoost(repeated);
                    check(Math.abs(event.getXPower() - repeated.getXPower()) < 1e-5, "Yaw wrap changes boost");
                    combinations++;
                }
            }
        }
        check(profile.getModes().size() == 4, "Server profiles lost");
        booster.setEnabled(true);
        player.velocity = new Vec3(.4,-.2,.6);
        near(CombatRuntime.rocketVelocity(player), expectedRocket(booster, player), "Rocket hook coefficients");
        booster.setEnabled(false);
        check(!CombatRuntime.rocketModulesActive(player), "Disabled booster changes rocket");

        var victim = (FlightPlayer) allocate(FlightPlayer.class);
        victim.place(new Vec3(0,0,1)); victim.velocity = new Vec3(0,0,1); victim.flying = true;
        victim.setYRot(90); victim.setXRot(0); victim.zo = 0;
        set(AttackAura.class, aura, "target", victim);
        target.setEnabled(true);
        target.getOvertake().setEnabled(true);
        target.getPredictionStrength().setCurrentValue(3);
        var prediction = (ModeSetting) setting(target, "Режим предикта");
        prediction.setCurrentMode("От ротации");
        near(target.predictTarget(victim, victim.position), victim.position.add(-3,0,0), "Overtake by resolved rotation");
        prediction.setCurrentMode("От скорости");
        Vec3 velocityPrediction = target.predictTarget(victim, victim.position).subtract(victim.position);
        check(velocityPrediction.length() > 0 && Double.isFinite(velocityPrediction.length()) && velocityPrediction.z > 0, "Eight-tick velocity prediction");
        victim.zo = 1;
        near(target.predictTarget(victim, victim.position), victim.position, "BPS threshold ignored");
        target.getOvertake().setEnabled(false);
        victim.zo = 0;
        near(target.predictTarget(victim, victim.position), victim.position, "Disabled overtake still predicts");
        target.setEnabled(false);
        motion.setEnabled(true);
        Vec3 originalVelocity = new Vec3(.4,.1,.8);
        near(CombatRuntime.glidingVelocity(originalVelocity), Vec3.ZERO, "Motion did not stop near target");
        victim.place(new Vec3(0,0,10));
        near(CombatRuntime.glidingVelocity(new Vec3(.1,0,.1)), originalVelocity, "Motion did not restore saved velocity");
        near(CombatRuntime.glidingVelocity(new Vec3(.1,0,.1)), new Vec3(.1,0,.1), "Motion restored velocity twice");
        victim.place(new Vec3(0,0,1));
        CombatRuntime.glidingVelocity(originalVelocity);
        motion.setEnabled(false);
        motion.setEnabled(true);
        victim.place(new Vec3(0,0,10));
        near(CombatRuntime.glidingVelocity(new Vec3(.1,0,.1)), new Vec3(.1,0,.1), "Disable retained saved speed");
        try (var clean = new CleanRenderScope(null, null)) {
            check(!CombatRuntime.gameplay(), "Clean capture runs imported gameplay");
            near(CombatRuntime.glidingVelocity(originalVelocity), originalVelocity, "Clean capture changed movement");
        }
        motion.setEnabled(false);
        checkHooks();
        var assets = com.google.gson.JsonParser.parseString(Files.readString(Path.of("src/main/resources/combat-assets.json"))).getAsJsonArray();
        for (var entry : assets) {
            var resource = entry.getAsJsonObject();
            byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/assets/pizzadlc").resolve(resource.get("path").getAsString()));
            check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)).equals(resource.get("sha256").getAsString()), "Imported effect asset modified or missing: " + resource.get("path"));
        }
        System.out.println("Native combat modules: 4 modules, 8 rotation modes, native config round trips, " + combinations
            + " boost angles, both overtake modes, stop/restore flight, clean capture and required Minecraft hooks passed.");
    }

    private static Vec3 expectedRocket(ElytraBooster booster, FlightPlayer player) {
        var coefficients = new RocketBoostEvent(player,1.5F,1.5F,1.5F);
        booster.onRocketBoost(coefficients);
        Vec3 look = player.getLookAngle(), v = player.velocity;
        return v.add(look.x*.1+(look.x*coefficients.getXPower()-v.x)*.5,
            look.y*.1+(look.y*coefficients.getYPower()-v.y)*.5,look.z*.1+(look.z*coefficients.getZPower()-v.z)*.5);
    }

    private static ClassNode bytecode(String name) throws Exception {
        try (var stream = CombatModulesCheck.class.getResourceAsStream("/" + name.replace('.','/') + ".class")) {
            check(stream != null, "Missing class " + name);
            var node = new ClassNode(); new ClassReader(stream).accept(node, 0); return node;
        }
    }
    private static void checkHooks() throws Exception {
        String prefix = "pizza.dlc.mixin.client.";
        var config = com.google.gson.JsonParser.parseString(Files.readString(Path.of("src/main/resources/pizzadlc.client.mixins.json"))).getAsJsonObject();
        for (var element : config.getAsJsonArray("client")) {
            if (!element.getAsString().startsWith("Combat") && !element.getAsString().equals("LivingEntityGravityAccessor") && !element.getAsString().equals("WebBlockMixin")) continue;
            ClassNode mixin = bytecode(prefix + element.getAsString());
            var declaration = mixin.invisibleAnnotations.stream().filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
            check(Boolean.FALSE.equals(value(declaration, "remap")), "Combat mixin still enables remap: " + element);
            String target = mixin.invisibleAnnotations.stream().filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                .map(a -> (List<org.objectweb.asm.Type>) a.values.get(a.values.indexOf("value")+1)).findFirst().orElseThrow().getFirst().getClassName();
            ClassNode original = bytecode(target);
            for (var field : mixin.fields) if (has(field.visibleAnnotations,"Shadow") || has(field.invisibleAnnotations,"Shadow")) {
                check(original.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)), "Missing shadow " + target + "/" + field.name);
            }
            for (var method : mixin.methods) {
                var annotations = new ArrayList<AnnotationNode>();
                if(method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
                if(method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
                for(var annotation : annotations) {
                    if(annotation.desc.contains("Accessor;")) {
                        String field = (String) value(annotation,"value");
                        check(original.fields.stream().anyMatch(f -> f.name.equals(field)), "Missing accessor " + target + "/" + field);
                    } else if(annotation.desc.contains("Invoker;")) {
                        String name = (String) value(annotation,"value");
                        check(original.methods.stream().anyMatch(m -> m.name.equals(name)), "Missing invoker " + target + "/" + name);
                    } else if(annotation.desc.contains("Inject;") || annotation.desc.contains("ModifyReturnValue;")) {
                        for(String selector : (List<String>) value(annotation,"method")) {
                            String name = selector.contains("(") ? selector.substring(0,selector.indexOf('(')) : selector;
                            var originalMethod = original.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow(() -> new AssertionError("Missing hook " + target + "/" + name));
                            if (annotation.desc.contains("Inject;")) {
                                var callbackArgs = org.objectweb.asm.Type.getArgumentTypes(method.desc);
                                int callbackIndex = 0;
                                while (callbackIndex < callbackArgs.length && !callbackArgs[callbackIndex].getClassName().startsWith("org.spongepowered.asm.mixin.injection.callback.CallbackInfo")) callbackIndex++;
                                if (callbackIndex > 0) {
                                    var targetArgs = org.objectweb.asm.Type.getArgumentTypes(originalMethod.desc);
                                    check(callbackIndex == targetArgs.length, "Wrong injection argument count: " + mixin.name + "/" + method.name);
                                    for (int i=0;i<callbackIndex;i++) check(callbackArgs[i].equals(targetArgs[i]), "Wrong injection argument type: " + mixin.name + "/" + method.name);
                                }
                            }
                            Object at = value(annotation,"at");
                            List<AnnotationNode> positions = at instanceof List<?> list ? (List<AnnotationNode>) list : List.of((AnnotationNode)at);
                            for(var position : positions) {
                                String call = (String) value(position,"target");
                                if(call == null || !"INVOKE".equals(value(position,"value"))) continue;
                                boolean found=false;
                                for(var instruction : originalMethod.instructions) if(instruction instanceof MethodInsnNode invocation
                                    && call.equals("L"+invocation.owner+";"+invocation.name+invocation.desc)) found=true;
                                check(found, "Missing injection call " + name + " -> " + call);
                            }
                        }
                    }
                }
            }
        }
    }
    private static boolean has(List<AnnotationNode> annotations,String ending) { return annotations != null && annotations.stream().anyMatch(a -> a.desc.endsWith("/"+ending+";")); }
    private static Object value(AnnotationNode annotation,String key) { if(annotation.values==null) return null; for(int i=0;i<annotation.values.size();i+=2) if(annotation.values.get(i).equals(key)) return annotation.values.get(i+1); return null; }

    private static final class FlightPlayer extends LocalPlayer implements ResolvedRotationAccess, LivingEntityGravityAccessor {
        Vec3 position, velocity;
        boolean flying;
        private FlightPlayer() { super(null,null,null,null,null,null,false,null); }
        @Override public boolean isFallFlying() { return flying; }
        @Override public Vec3 position() { return position; }
        @Override public Vec3 getDeltaMovement() { return velocity; }
        @Override protected double getDefaultGravity() { return .08; }
        @Override public boolean isNoGravity() { return false; }
        void place(Vec3 location) throws Exception {
            position = location;
            set(net.minecraft.world.entity.Entity.class, this, "position", location);
            set(net.minecraft.world.entity.Entity.class, this, "eyeHeight", 1.6F);
            set(net.minecraft.world.entity.Entity.class, this, "bb", new AABB(location.x-.3,location.y,location.z-.3,location.x+.3,location.y+1.8,location.z+.3));
        }
        @Override public boolean hasEffect(Holder<MobEffect> effect) { return false; }
        @Override public double pizzadlc$getEffectiveGravity() { return .08; }
        @Override public float getResolvedYaw() { return getYRot(); }
        @Override public float getResolvedPitch() { return getXRot(); }
        @Override public Vec3 getResolvedPos() { return position; }
        @Override public void setResolvedYaw(float yaw) { setYRot(yaw); }
        @Override public void setResolvedPitch(float pitch) { setXRot(pitch); }
        @Override public void setResolvedPos(Vec3 position) { this.position=position; }
        @Override public Vec3 getResolvedForward() { return Vec3.directionFromRotation(getXRot(),getYRot()); }
    }
}
