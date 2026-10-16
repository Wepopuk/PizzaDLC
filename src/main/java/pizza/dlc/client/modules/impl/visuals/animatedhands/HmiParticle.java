package pizza.dlc.client.modules.impl.visuals.animatedhands;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pizza.dlc.api.util.render.AdditiveTextureType;

public final class HmiParticle {
   private static final double HALF_SIZE = 0.25;
   private static final double TIME_SCALE = 30.0;
   private static final double DRAG = 0.9;
   private static final double GRAVITY = 0.01;
   private final boolean gravity;
   private final String renderType;
   private final Consumer<HmiParticle> ticker;
   private final List<Identifier> keyframes;
   private final Iterator<Identifier> keyframeIterator;
   private Identifier texture;
   private double lifetime;
   private double alpha;
   private double scale;
   private double age;
   private boolean birth = true;
   private int prevAge;
   public String lifecycleType;
   public String space;
   public InteractionHand hand;
   public boolean dead;
   public double x;
   public double y;
   public double z;
   public double dx;
   public double dy;
   public double dz;
   public double rx;
   public double ry;
   public double rz;
   public double drx;
   public double dry;
   public double drz;
   public double maxScale;
   public PoseStack p_matrices;

   HmiParticle(
      boolean gravity,
      double[] motion,
      double maxScale,
      Identifier texture,
      List<Identifier> keyframes,
      String space,
      InteractionHand hand,
      String lifecycleType,
      String renderType,
      double lifetime,
      double alpha,
      Consumer<HmiParticle> ticker,
      PoseStack matrices
   ) {
      this.gravity = gravity;
      this.x = motion[0];
      this.y = motion[1];
      this.z = motion[2];
      this.dx = motion[3];
      this.dy = motion[4];
      this.dz = motion[5];
      this.rx = motion[6];
      this.ry = motion[7];
      this.rz = motion[8];
      this.drx = motion[9];
      this.dry = motion[10];
      this.drz = motion[11];
      this.maxScale = maxScale;
      this.texture = texture;
      this.keyframes = keyframes == null ? List.of() : keyframes;
      this.keyframeIterator = this.keyframes.iterator();
      this.space = space;
      this.hand = hand;
      this.lifecycleType = lifecycleType;
      this.renderType = renderType;
      this.lifetime = lifetime;
      this.alpha = alpha;
      this.ticker = ticker;
      this.p_matrices = matrices;
   }

   void tick() {
      if (this.dead) return;
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.dead = true;
         return;
      }

      double dt = HmiRuntime.deltaTime();
      double step = dt * TIME_SCALE;
      this.lifetime = Math.clamp(this.lifetime - 0.1 * step, 0.0, 9999.0);
      this.age += 0.1 * step;
      this.x += this.dx * step;
      this.y += this.dy * step;
      this.z += this.dz * step;
      double drag = Math.pow(DRAG, step);
      this.dx *= drag;
      this.dy *= drag;
      this.dz *= drag;
      if (this.gravity) this.dy -= GRAVITY * step;
      this.rx += this.drx * step;
      this.ry += this.dry * step;
      this.rz += this.drz * step;
      if (!this.keyframes.isEmpty() && this.keyframeIterator.hasNext() && player.tickCount % 4 == 0 && player.tickCount != this.prevAge) {
         this.texture = this.keyframeIterator.next();
      }

      switch (this.lifecycleType) {
         case "SCALE" -> {
            if (!this.birth && this.lifetime == 0.0) {
               this.scale -= 0.03 * step;
               if (this.scale <= 0.0) this.dead = true;
            } else {
               this.scale += 0.07 * step;
               if (this.scale >= this.maxScale) {
                  this.scale = this.maxScale;
                  this.birth = false;
               }
            }
         }
         case "OPACITY" -> {
            if (!this.birth && this.lifetime == 0.0) {
               this.alpha -= 10.0 * step;
               if (this.alpha <= 0.0) {
                  this.alpha = 0.0;
                  this.dead = true;
               }
            } else {
               this.scale = this.maxScale;
               this.birth = false;
               this.alpha = 255.0;
            }
         }
         case "SPAWN" -> {
            if (!this.birth && this.lifetime == 0.0) {
               this.dead = true;
            } else {
               this.scale = this.maxScale;
               this.birth = false;
            }
         }
         case "KEYFRAME" -> {
            if (!this.birth && !this.keyframeIterator.hasNext()) {
               this.dead = true;
            } else {
               this.scale = this.maxScale;
               this.birth = false;
            }
         }
         default -> {
         }
      }

      if (this.ticker != null) this.ticker.accept(this);
      this.prevAge = player.tickCount;
   }

   void render(PoseStack pose, SubmitNodeCollector collector, int light) {
      if (this.dead || this.texture == null) return;
      pose.pushPose();
      pose.translate(this.x, this.y, this.z);
      pose.scale((float)this.scale, (float)this.scale, (float)this.scale);
      RenderType type = "ADDITIVE".equals(this.renderType) ? AdditiveTextureType.image(this.texture) : RenderTypes.breezeWind(this.texture, 0.0F, 0.0F);
      Matrix4f matrix = pose.last().pose();
      Vector3f translation = matrix.getTranslation(new Vector3f());
      Vector3f scaleVec = matrix.getScale(new Vector3f());
      Quaternionf cameraRotation = Minecraft.getInstance().gameRenderer.mainCamera().rotation();
      matrix.identity().translate(translation).scale(scaleVec).rotate(cameraRotation);
      matrix.rotate(new Quaternionf().rotateX((float)(this.rx * (float)(Math.PI / 180.0))));
      matrix.rotate(new Quaternionf().rotateY((float)(this.ry * (float)(Math.PI / 180.0))));
      matrix.rotate(new Quaternionf().rotateZ((float)(this.rz * (float)(Math.PI / 180.0))));
      Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F).mul(pose.last().normal());
      int vertexLight = "CUTOUT_L".equals(this.renderType) ? LightCoordsUtil.lightCoordsWithEmission(light, 15) : light;
      int a = (int)Math.clamp((long)(int)this.alpha, 0, 255);
      float h = (float)HALF_SIZE;
      collector.submitCustomGeometry(pose, type, (entry, vc) -> {
         vertex(vc, entry, h, h, 1.0F, 0.0F, a, vertexLight, normal);
         vertex(vc, entry, -h, h, 0.0F, 0.0F, a, vertexLight, normal);
         vertex(vc, entry, -h, -h, 0.0F, 1.0F, a, vertexLight, normal);
         vertex(vc, entry, h, -h, 1.0F, 1.0F, a, vertexLight, normal);
      });
      pose.popPose();
   }

   private static void vertex(VertexConsumer vc, PoseStack.Pose entry, float x, float y, float u, float v, int alpha, int light, Vector3f normal) {
      vc.addVertex(entry, x, y, 0.0F)
         .setColor(255, 255, 255, alpha)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(normal.x, normal.y, normal.z);
   }
}
