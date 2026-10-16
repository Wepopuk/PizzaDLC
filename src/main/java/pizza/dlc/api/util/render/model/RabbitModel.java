package pizza.dlc.api.util.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public final class RabbitModel {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("pizzadlc", "models/rabbit.png");
   private static final float ARM_TILT = 0.0873f;

   private static final Map<Integer, RabbitModel> MODELS = new HashMap<>();

   private final ModelPart root;
   private final ModelPart body;
   private final ModelPart head;
   private final ModelPart leftArm;
   private final ModelPart rightArm;
   private final ModelPart leftLeg;
   private final ModelPart rightLeg;

   private RabbitModel() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition data = mesh.getRoot();
      PartDefinition rabbit = data.addOrReplaceChild("rabbit",
         box(28, 45, -5.0f, -13.0f, -5.0f, 10.0f, 11.0f, 8.0f),
         PartPose.offset(0.0f, 24.0f, 0.0f));
      rabbit.addOrReplaceChild("right_leg", box(0, 32, -2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f),
         PartPose.offset(-3.0f, -2.0f, -1.0f));
      rabbit.addOrReplaceChild("left_leg", box(0, 32, -2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f),
         PartPose.offset(3.0f, -2.0f, -1.0f));
      rabbit.addOrReplaceChild("left_arm", box(24, 16, 0.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f),
         PartPose.offsetAndRotation(5.0f, -13.0f, -1.0f, 0.0f, 0.0f, -ARM_TILT));
      rabbit.addOrReplaceChild("right_arm", box(24, 16, -2.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f),
         PartPose.offsetAndRotation(-5.0f, -13.0f, -1.0f, 0.0f, 0.0f, ARM_TILT));
      rabbit.addOrReplaceChild("head", CubeListBuilder.create()
         .texOffs(0, 0).addBox(-3.0f, 0.0f, -4.0f, 6.0f, 1.0f, 6.0f)
         .texOffs(0, 45).addBox(-4.0f, -11.0f, -4.0f, 8.0f, 11.0f, 8.0f)
         .texOffs(56, 0).addBox(-5.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f)
         .texOffs(56, 0).addBox(3.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f)
         .texOffs(46, 0).addBox(1.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f)
         .texOffs(46, 0).addBox(-4.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f),
         PartPose.offset(0.0f, -14.0f, -1.0f));

      this.root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
      this.body = this.root.getChild("rabbit");
      this.head = this.body.getChild("head");
      this.leftArm = this.body.getChild("left_arm");
      this.rightArm = this.body.getChild("right_arm");
      this.leftLeg = this.body.getChild("left_leg");
      this.rightLeg = this.body.getChild("right_leg");
   }

   public static RabbitModel of(int entityId) {
      if (MODELS.size() > 128) {
         MODELS.clear();
      }
      return MODELS.computeIfAbsent(Integer.valueOf(entityId), ignored -> new RabbitModel());
   }

   public void submit(PlayerModel vanilla, AvatarRenderState state, PoseStack pose,
      SubmitNodeCollector collector, int light, int overlay, int color, int outlineColor) {
      vanilla.setupAnim(state);
      copyAngles(vanilla);

      pose.pushPose();
      pose.scale(1.25f, 1.25f, 1.25f);
      pose.translate(0.0f, -0.3f, 0.0f);
      RenderType layer = RenderTypes.entityTranslucent(TEXTURE);
      collector.submitModelPart(this.root, pose, layer, light, overlay, (TextureAtlasSprite) null, color,
         (ModelFeatureRenderer.CrumblingOverlay) null, outlineColor);
      pose.popPose();
   }

   private void copyAngles(PlayerModel vanilla) {
      this.head.xRot = vanilla.head.xRot;
      this.head.yRot = vanilla.head.yRot;
      this.head.zRot = vanilla.head.zRot;

      this.body.xRot = 0.0f;
      this.body.yRot = 0.0f;
      this.body.zRot = 0.0f;

      this.leftArm.xRot = vanilla.leftArm.xRot;
      this.leftArm.yRot = vanilla.leftArm.yRot;
      this.leftArm.zRot = vanilla.leftArm.zRot - ARM_TILT;

      this.rightArm.xRot = vanilla.rightArm.xRot;
      this.rightArm.yRot = vanilla.rightArm.yRot;
      this.rightArm.zRot = vanilla.rightArm.zRot + ARM_TILT;

      this.leftLeg.xRot = vanilla.leftLeg.xRot;
      this.leftLeg.yRot = vanilla.leftLeg.yRot;
      this.leftLeg.zRot = vanilla.leftLeg.zRot;

      this.rightLeg.xRot = vanilla.rightLeg.xRot;
      this.rightLeg.yRot = vanilla.rightLeg.yRot;
      this.rightLeg.zRot = vanilla.rightLeg.zRot;
   }

   private static CubeListBuilder box(int u, int v, float x, float y, float z, float width, float height, float depth) {
      return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth);
   }
}
