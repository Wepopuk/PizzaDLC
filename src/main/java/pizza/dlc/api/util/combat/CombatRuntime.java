package pizza.dlc.api.util.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.Event;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.combat.*;
import pizza.dlc.api.events.impl.game.EventInputMove;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.events.impl.render.EventRender2D;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.util.render.combat.CombatHudText;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.impl.combat.AttackAura;
import pizza.dlc.client.modules.impl.combat.ElytraTarget;
import pizza.dlc.client.modules.impl.movement.ElytraBooster;
import pizza.dlc.client.modules.impl.movement.ElytraMotion;

/** Minecraft hooks publish native Orbit events; modules use ordinary Module subscriptions. */
public final class CombatRuntime {
    public static int worldTicks;
    private static boolean initialized;
    private static long previousFrame;
    private static double mediumDebt, fastDebt;
    private static int sentSlot = -1;

    public static void initialize() {
        if (initialized) return;
        PizzaDLC.getInstance().getBus().subscribe(CombatRotations.state());
        PizzaDLC.getInstance().getBus().subscribe(new CombatRuntime());
        initialized = true;
    }
    public static boolean gameplay() {
        var mc = Minecraft.getInstance();
        return initialized && mc != null && mc.player != null && mc.level != null
            && !CleanRenderScope.isActive() && !StreamProofRenderer.isVanillaGameplayActive();
    }
    public static void post(Event event) { if (gameplay()) PizzaDLC.getInstance().getBus().post(event); }
    public static boolean moduleEnabled(String name) {
        return PizzaDLCClient.getInstance().getModuleManager().getModules().stream()
            .anyMatch(module -> module.getName().replace(" ", "").equalsIgnoreCase(name) && module.isEnabled());
    }
    public static void enabled(Module module) {
        if (!(module instanceof AttackAura)) return;
        if (initialized && Minecraft.getInstance().player != null) {
            var state = CombatRotations.state(); var player = Minecraft.getInstance().player;
            state.getRotation().set(player.getYRot(), player.getXRot());
            state.getClientRotation().set(state.getRotation());
        }
    }
    public static void disabled(Module module) {
        if (!(module instanceof AttackAura) || !initialized) return;
        CombatRotations.state().resetForClient();
        for (var plan : CombatRotations.plans()) { CombatRotations.plans().clear(plan); plan.reset(); }
    }
    public static void preTick() { if (gameplay()) { worldTicks++; post(new CombatTickEvent()); } }
    public static void postTick() {
        if (gameplay() && Minecraft.getInstance().player.connection.hasClientLoaded()) post(new CombatTickEvent.PostTick());
    }
    public static void frame() {
        if (pizza.dlc.api.neuro.NeuroSystem.get()!=null) pizza.dlc.api.neuro.NeuroSystem.get().frame();
        long now = System.nanoTime();
        if (!gameplay() || Minecraft.getInstance().isPaused()) { previousFrame=now; mediumDebt=fastDebt=0; return; }
        double elapsed = previousFrame == 0 ? 0 : Math.min(.05, (now-previousFrame)/1e9);
        previousFrame = now; mediumDebt += elapsed*60; fastDebt += elapsed*240;
        while (mediumDebt >= 1) { mediumDebt--; post(new MediumAimEvent()); }
        while (fastDebt >= 1) { fastDebt--; post(new FastAimEvent()); }
    }
    public static int sentSlot() { return sentSlot; }
    public static boolean rocketModulesActive(LivingEntity entity) {
        if (!gameplay() || entity != Minecraft.getInstance().player || !entity.isFallFlying()) return false;
        var modules = PizzaDLCClient.getInstance().getModuleManager();
        return modules.get(ElytraBooster.class).isEnabled() || modules.get(ElytraTarget.class).isEnabled();
    }
    public static Vec3 rocketVelocity(LivingEntity entity) {
        var look=entity.getLookAngle(); var velocity=entity.getDeltaMovement();
        var event=new RocketBoostEvent(entity,1.5F,1.5F,1.5F); post(event);
        return velocity.add(look.x*.1+(look.x*event.getXPower()-velocity.x)*.5,
            look.y*.1+(look.y*event.getYPower()-velocity.y)*.5, look.z*.1+(look.z*event.getZPower()-velocity.z)*.5);
    }
    public static Vec3 glidingVelocity(Vec3 velocity) {
        if (!gameplay()) return velocity;
        var event=new GlidingVelocityEvent(velocity); post(event); return event.getVelocity();
    }
    public static CombatCameraEvent camera(float yaw, float pitch) {
        if (pizza.dlc.api.neuro.NeuroSystem.managed()) {
            var p=Minecraft.getInstance().player;
            return new CombatCameraEvent(p.getYRot(),p.getXRot());
        }
        var event=new CombatCameraEvent(yaw,pitch); post(event); return event;
    }
    public static Angle cleanCameraRotation(boolean showRotations) {
        if (!gameplay()) return null;
        var state=CombatRotations.state(); var plan=state.getActivePlan();
        if (plan == null || !plan.hasMovementCorrection()) return null;
        var player=Minecraft.getInstance().player;
        return showRotations ? new Angle(player.getYRot(),player.getXRot())
            : new Angle(state.getClientRotation().x,state.getClientRotation().y);
    }
    public static boolean mouseTurn(double x, double y) {
        if (!gameplay()) return false;
        var event=new CombatMouseEvent((float)x,(float)y); post(event); return event.isCancelled();
    }
    @EventHandler private void input(EventInputMove event) {
        if (!gameplay()) return;
        var input=new CombatInputEvent(event.getForward(),event.getStrafe(),event.getForward()>0,event.getForward()<0,
            event.getStrafe()>0,event.getStrafe()<0,event.isJump(),event.isSneaking(),event.isSprint());
        post(input);
        var plan=CombatRotations.plans().getActive();
        if (!input.isCancelled() && plan!=null && plan.hasMovementCorrection()) CombatMovement.correctInput(input);
        event.setForward(input.isCancelled()?0:input.getForward()); event.setStrafe(input.isCancelled()?0:input.getStrafe());
        event.setJump(input.isJumping()); event.setSneaking(input.isSneaking()); event.setSprint(input.isSprinting());
    }
    @EventHandler private void key(EventKey event) { post(new CombatKeyEvent(event.getKey(),event.getAction())); }
    @EventHandler private void world(EventWorldLoad event) {
        var manager=PizzaDLCClient.getInstance().getModuleManager();
        manager.get(AttackAura.class).resetState(); manager.get(ElytraTarget.class).resetState();
        manager.get(ElytraMotion.class).resetState(); manager.get(ElytraBooster.class).resetState();
        disabled(manager.get(AttackAura.class)); previousFrame=0; mediumDebt=fastDebt=0; sentSlot=-1;
    }
    @EventHandler private void packet(PacketEvent.Send event) {
        if (event.getPacket() instanceof net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket slot) sentSlot=slot.getSlot();
        post(new CombatPacketEvent(event.getPacket()));
    }
    @EventHandler private void hud(EventRender2D event) {
        if (!gameplay()) return;
        var window=Minecraft.getInstance().getWindow(); CombatHudText.begin(event.getContext());
        try { post(new CombatHudEvent(window.getScreenWidth(),window.getScreenHeight(),event.getContext())); }
        finally { CombatHudText.end(); }
    }
}
