package pizza.dlc.api.util.render.combat;
import pizza.dlc.api.events.impl.combat.CombatWorldRenderEvent;
import pizza.dlc.api.util.render.combat.CombatColors;
import pizza.dlc.api.util.render.combat.CombatDistortionRenderer;
import pizza.dlc.api.util.render.combat.CombatGlowRenderer;
import pizza.dlc.api.util.render.combat.CombatPipelineBuilder;
import pizza.dlc.api.util.render.combat.CombatQuadGeometry;
import pizza.dlc.api.util.render.combat.CombatResources;
import pizza.dlc.api.util.render.combat.CombatTextureQuads;
import pizza.dlc.api.util.render.combat.CombatVertexAllocator;
import pizza.dlc.api.util.render.combat.OrbitGeometry;
import pizza.dlc.api.util.render.combat.TargetCubeRenderer;
import pizza.dlc.api.util.render.combat.TargetSurfaceLighting;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.math.Axis;
import pizza.dlc.mixin.client.CombatRenderPipelinesAccessor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class CombatTargetEffects implements pizza.dlc.api.interfaces.QuickImports {
   private static final Identifier f_26511 = Identifier.fromNamespaceAndPath("pizzadlc", "icons/world_render/target.png");
   private static final Identifier f_26512 = Identifier.fromNamespaceAndPath("pizzadlc", "icons/world_render/target1.png");
   private static final Identifier f_26513 = Identifier.fromNamespaceAndPath("pizzadlc", "icons/world_render/firepart2.png");
   private static final Identifier f_26514 = Identifier.fromNamespaceAndPath("pizzadlc", "textures/targetesp/zenith_ghost.png");
   private static final Identifier f_26515 = Identifier.fromNamespaceAndPath("pizzadlc", "icons/world_render/bloom.png");
   private static final Identifier f_26516 = Identifier.fromNamespaceAndPath("pizzadlc", "images/particles/bloom.png");
   private static final Identifier f_26517 = Identifier.fromNamespaceAndPath("pizzadlc", "images/particles/bloom.png");
   private static final Identifier f_26518 = Identifier.fromNamespaceAndPath("pizzadlc", "images/targetesp/glow.png");
   private static final Identifier f_26519 = Identifier.fromNamespaceAndPath("pizzadlc", "textures/targetesp/lightning_arc.png");
   private static final RenderPipeline f_26520 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp")
      .m_2734(CombatResources.m_9312("dead_chams"))
      .m_2740(CombatResources.m_9312("dead_chams"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2741("Sampler0")
      .m_2722();
   private static final RenderPipeline f_26521 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_ghost_no_depth")
      .m_2734(CombatResources.m_9312("zenith_target_esp"))
      .m_2740(CombatResources.m_9312("zenith_target_esp"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(true)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2741("Sampler0")
      .m_2722();
   private static final RenderPipeline f_26522 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_ghost_depth")
      .m_2734(CombatResources.m_9312("zenith_target_esp"))
      .m_2740(CombatResources.m_9312("zenith_target_esp"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(false)
      .m_2725(true)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2741("Sampler0")
      .m_2722();
   private static final RenderPipeline f_26523 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_crystal_color")
      .m_2734(CombatResources.m_9312("target_esp_crystal"))
      .m_2740(CombatResources.m_9312("target_esp_crystal"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(false)
      .m_2725(true)
      .m_2732(BlendFunction.TRANSLUCENT)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2722();
   private static final RenderPipeline f_26524 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_crystal_color_no_depth")
      .m_2734(CombatResources.m_9312("target_esp_crystal"))
      .m_2740(CombatResources.m_9312("target_esp_crystal"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(true)
      .m_2732(BlendFunction.TRANSLUCENT)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2722();
   private static final RenderPipeline f_26525 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_crystal_glow_depth")
      .m_2734(CombatResources.m_9312("dead_chams"))
      .m_2740(CombatResources.m_9312("dead_chams"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2741("Sampler0")
      .m_2722();
   private static final RenderPipeline f_26526 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_orbit_crystal_depth")
      .m_2734(CombatResources.m_9312("target_esp_crystal"))
      .m_2740(CombatResources.m_9312("target_esp_crystal"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.LIGHTNING)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
      .m_2722();
   private static final RenderPipeline f_26527 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_orbit_crystal_no_depth")
      .m_2734(CombatResources.m_9312("target_esp_crystal"))
      .m_2740(CombatResources.m_9312("target_esp_crystal"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.LIGHTNING)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
      .m_2722();
   private static final RenderPipeline f_26528 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_orbit_glow_depth")
      .m_2734(CombatResources.m_9312("target_esp_orbit_glow"))
      .m_2740(CombatResources.m_9312("target_esp_orbit_glow"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.LIGHTNING)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2741("Sampler0")
      .m_2722();
   private static final RenderPipeline f_26529 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_orbit_glow_no_depth")
      .m_2734(CombatResources.m_9312("target_esp_orbit_glow"))
      .m_2740(CombatResources.m_9312("target_esp_orbit_glow"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.LIGHTNING)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2741("Sampler0")
      .m_2722();
   private static final RenderPipeline f_26530 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_ring")
      .m_2734(CombatResources.m_9312("target_esp_ring"))
      .m_2740(CombatResources.m_9312("target_esp_ring"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2722();
   private static final RenderPipeline f_26531 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_hoop")
      .m_2734(CombatResources.m_9312("target_esp_ring"))
      .m_2740(CombatResources.m_9312("target_esp_ring"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2722();
   private static final RenderPipeline f_26532 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_ring_wave")
      .m_2734(CombatResources.m_9312("target_esp_cube"))
      .m_2740(CombatResources.m_9312("target_esp_cube"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.OVERLAY)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2722();
   private static final RenderPipeline f_26533 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_cube")
      .m_2734(CombatResources.m_9312("target_esp_cube"))
      .m_2740(CombatResources.m_9312("target_esp_cube"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2727()
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2722();
   private static final RenderPipeline f_26534 = CombatPipelineBuilder.m_2718()
      .m_2726("dimasik/target_esp_cube_composite")
      .m_2734(CombatResources.m_9312("hands_fullscreen"))
      .m_2740(CombatResources.m_9312("target_esp_cube_composite"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.TRANSLUCENT)
      .m_2739(CombatPipelineBuilder.f_7670, PrimitiveTopology.TRIANGLES)
      .m_2741("InSampler")
      .m_2722();
   private static final RenderPipeline f_26535 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_lightning_arc")
      .m_2721(1.0F, 1.0F)
      .m_2734(CombatResources.m_9312("dead_chams"))
      .m_2740(CombatResources.m_9312("dead_chams"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2741("Sampler0")
      .m_2722();
   private static final RenderPipeline f_26536 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_lightning_arc_depth")
      .m_2721(1.0F, 1.0F)
      .m_2734(CombatResources.m_9312("dead_chams"))
      .m_2740(CombatResources.m_9312("dead_chams"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(true)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2741("Sampler0")
      .m_2722();
   private static final RenderPipeline f_26537 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_lightning")
      .m_2734(CombatResources.m_9312("target_esp_lightning"))
      .m_2740(CombatResources.m_9312("target_esp_lightning"))
      .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2722();
   private static final RenderPipeline f_26538 = CombatPipelineBuilder.m_2718()
      .m_2726("dimasik/target_esp_lightning_core")
      .m_2734(CombatResources.m_9312("hands_fullscreen"))
      .m_2740(CombatResources.m_9312("target_esp_lightning_core"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.TRANSLUCENT)
      .m_2739(CombatPipelineBuilder.f_7670, PrimitiveTopology.TRIANGLES)
      .m_2741("InSampler")
      .m_2722();
   private static final RenderPipeline f_26539 = CombatPipelineBuilder.m_2718(CombatRenderPipelinesAccessor.getPositionColorSnippet())
      .m_2726("dimasik/target_esp_triangle_lines")
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.DEBUG_LINES)
      .m_2722();
   private static final RenderPipeline f_26540 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
      .m_2726("dimasik/target_esp_pentagram")
      .m_2729("Globals", UniformType.UNIFORM_BUFFER)
      .m_2734(CombatResources.m_9312("target_esp_pentagram"))
      .m_2740(CombatResources.m_9312("target_esp_pentagram"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.ADDITIVE)
      .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
      .m_2722();

   private static LivingEntity f_26543;
   private static float f_26544;
   private static float f_26545;
   private static long f_26546;
   private static String f_26547 = "";
   private static final List<Vector3f> f_26548 = new ArrayList<>();
   private static final float[][] f_26549 = new float[][]{
      {0.0F, 1.5F, 0.0F},
      {0.0F, -1.5F, 0.0F},
      {1.0F, 0.0F, 0.0F},
      {-1.0F, 0.0F, 0.0F},
      {0.0F, 0.0F, 1.0F},
      {0.0F, 0.0F, -1.0F}
   };
   private static final int[][] f_26550 = new int[][]{{0, 4, 2}, {0, 3, 4}, {0, 5, 3}, {0, 2, 5}, {1, 2, 4}, {1, 4, 3}, {1, 3, 5}, {1, 5, 2}};
   private static final float[] f_26551 = new float[]{
      1.0F,
      0.8F,
      0.6F,
      0.9F,
      0.7F,
      0.5F,
      0.4F,
      0.6F
   };
   private static int f_26552 = CombatTargetEffects.f_27990;
   private static float f_26553;
   private static float f_26554;
   private static final List<Vector3f> f_26555 = new ArrayList<>();
   private static int f_26556 = CombatTargetEffects.f_27991;
   private static final Random f_26557 = new Random();
   private static final List<CombatTargetEffects.LightningBurst> f_26558 = new ArrayList<>();
   private static long f_26559;
   private static TextureTarget f_26560;
   private static final LinkedList<Vec3>[] f_26561 = new LinkedList[]{new LinkedList(), new LinkedList(), new LinkedList(), new LinkedList(), new LinkedList()};
   private static final List<CombatTargetEffects.FadeParticle> f_26562 = new ArrayList<>();
   private static double f_26563;
   private static final long f_26564 = System.currentTimeMillis();
   private static float f_26565;
   private static long f_26566 = System.nanoTime();
   private static long f_26567;
   private static int f_26568 = -1;
   private static int f_26569 = -11486721;
   private static float f_26570 = 1.2F;
   private static String f_26571 = "Тип 1";
   private static String f_26572 = "Тип 1";
   private static final double[] f_26573 = m_7908(true);
   private static final double[] f_26574 = m_7908(false);

   private static float f_26590 = 1.0F;
   private static float f_26591;
   private static double f_26592;
   private static boolean f_26593;
   private static int f_26594 = CombatTargetEffects.f_27996;
   private static int f_26595 = 3;
   private static boolean f_26596 = true;
   private static String f_26597 = "Тип 1";
   private static long f_26598;
   private static float f_26599;
   private static float f_26600;
   private static String f_26601 = "Тип 1";
   private static boolean f_26602;
   private static String f_26603 = "Тип 2";
   private static boolean f_26604;
   private static boolean f_26605 = true;
   private static String f_26606 = "";
   private static String f_26607 = "";
   private static String f_26608 = "";
   private static final LinkedList<CombatTargetEffects.GhostPoint> f_26609 = new LinkedList<>();
   private static long f_26610;
   private static int f_26611 = CombatTargetEffects.f_28000;

   private static final List<CombatTargetEffects.CrystalParticle> f_26617 = new ArrayList<>();
   private static long f_26618;

   private static final List<CombatTargetEffects.LightningArc> f_26623 = new ArrayList<>();
   private static long f_26624;
   private static int f_26625 = CombatTargetEffects.f_28001;
   private static float f_26626;
   private static TextureTarget f_26627;
   private static boolean f_26628;
   private static float f_26629 = 12.0F;
   private static boolean f_26630;
   private static boolean f_26631 = true;
   private static boolean f_26632;
   private static int f_26633 = -1;
   private static float f_26634 = 1.0F;
   private static float f_26635;
   private static boolean f_26636;
   private static boolean f_26637;
   private static double f_26638;
   private static double f_26639;
   private static double f_26640;
   private static double f_26641;
   private static double f_26642;
   private static double f_26643;
   private static boolean f_26644;
   private static CombatWorldRenderEvent f_26645;
   private static LivingEntity f_26646;
   private static String f_26647;
   private static int f_26648;
   private static int f_26649;
   private static GpuBufferSlice f_26650;
   private static ProjectionType f_26651;
   private static final Identifier f_26652 = CombatResources.m_9311("private/glow");
   private static final RenderPipeline f_26653 = CombatQuadGeometry.m_1814("dimasik/target_esp_private_depth", CompareOp.GREATER_THAN_OR_EQUAL);
   private static final RenderPipeline f_26654 = CombatQuadGeometry.m_1814("dimasik/target_esp_private_no_depth", CompareOp.ALWAYS_PASS);
   private static float f_26655 = 0.2F;
   private static int f_26656 = 14;
   private static boolean f_26657 = true;
   private static float f_26658;
   private static long f_26659;
   private static int f_26660 = CombatTargetEffects.f_28007;
   private static int f_26661;

   private static final int f_26669 = Integer.MIN_VALUE;
   private static final int f_26670 = Integer.MIN_VALUE;
   private static final int f_26671 = Integer.MIN_VALUE;
   private static final int f_26672 = Integer.MIN_VALUE;

   private static final int f_26689 = Integer.MIN_VALUE;
   private static final int f_26690 = Integer.MIN_VALUE;
   private static final int f_26691 = Integer.MIN_VALUE;
   private static final int f_26692 = Integer.MIN_VALUE;

   private static final double f_26697 = Math.PI;

   private static final int f_26718 = Integer.MIN_VALUE;

   private static final int f_26726 = Integer.MIN_VALUE;
   private static final int f_26727 = Integer.MIN_VALUE;

   private static final double f_26900 = Math.PI / 2;
   private static final double f_26901 = Math.PI / 2;
   private static final double f_26902 = Math.PI / 2;
   private static final double f_26903 = Math.PI / 2;

   private static final double f_26935 = Math.PI;

   private static final double f_26938 = Math.PI * 2;
   private static final double f_26939 = Math.PI * 2;
   private static final double f_26940 = -Math.PI / 2;
   private static final double f_26941 = Math.PI;
   private static final double f_26942 = -Math.PI / 2;
   private static final double f_26943 = Math.PI;
   private static final double f_26944 = Math.PI * 2;
   private static final double f_26945 = Math.PI * 2;

   private static final int f_26977 = Integer.MIN_VALUE;

   private static final double f_27009 = Math.PI * 2;

   private static final double f_27102 = Math.PI;

   private static final double f_27122 = Math.PI;

   private static final double f_27168 = Math.PI * 2;

   private static final double f_27170 = Math.PI * 2;

   private static final double f_27176 = Math.PI;

   private static final double f_27195 = Math.PI;

   private static final float f_27220 = (float) Math.PI;

   private static final double f_27231 = Math.PI;

   private static final double f_27247 = Math.PI;

   private static final double f_27280 = Math.PI;

   private static final double f_27311 = Math.PI;

   private static final double f_27352 = Math.PI * 2;
   private static final double f_27353 = Math.PI * 2;

   private static final double f_27458 = Math.PI * 2;
   private static final double f_27459 = Math.PI;

   private static final double f_27463 = Math.PI;

   private static final double f_27527 = Math.PI;

   private static final double f_27536 = Math.PI / 2;
   private static final double f_27537 = Math.PI;

   private static final double f_27564 = Math.PI * 2;

   private static final double f_27575 = Math.PI;

   private static final double f_27623 = Math.PI;

   private static final double f_27675 = Math.PI * 2;

   private static final double f_27686 = Math.PI;

   private static final double f_27689 = Math.PI;

   private static final double f_27710 = Math.PI;

   private static final float f_27754 = Float.MAX_VALUE;

   private static final int f_27990 = Integer.MIN_VALUE;
   private static final int f_27991 = Integer.MIN_VALUE;

   private static final int f_27996 = Integer.MIN_VALUE;

   private static final int f_28000 = Integer.MIN_VALUE;
   private static final int f_28001 = Integer.MIN_VALUE;

   private static final int f_28007 = Integer.MIN_VALUE;

   public static boolean hasFadingTarget() {
      return f_26543 != null && f_26544 > 0.01F;
   }

   private static void m_7808(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, float var5) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      float var6 = m_7937(var4);
      if (!(var6 <= 0.01F)) {
         float var7 = var2.getBbHeight();
         float var8 = Mth.lerp(var6, 0.52F, 1.0F) * m_7906(f_26544);
         var8 *= 1.0F + var5 * 0.18F;
         float var9 = 1.0F + 0.04F * (float)Math.sin(f_26545 * 3.1);
         float var10 = Math.max(0.68F, var2.getBbWidth() * 1.14F + 0.15F) * var8 * var9;
         Matrix4f var11 = var0.getPose().last().pose();
         BufferBuilder var12 = m_7952();
         Vec3[] var13 = m_7820(m_7956(new Vec3(0.0, 1.0, 0.0)), -f_26545 * 0.55);
         m_7899(var12, var11, var1.add(0.0, 0.035, 0.0), var13[0], var13[1], var10 * 1.32F, m_7887(m_7817(var3, 0.18F), var6 * 0.4F));
         double var14 = f_26545 * 1.28 + var2.getId() * 0.17;
         double var16 = Math.toRadians(34.0 + 9.0 * Math.sin(f_26545 * 0.7));
         Vec3 var18 = new Vec3(Math.sin(var16) * Math.cos(var14), Math.cos(var16), Math.sin(var16) * Math.sin(var14));
         Vec3[] var19 = m_7820(m_7956(var18), f_26545 * 0.82);
         Vec3 var20 = var1.add(0.0, var7 * 0.48, 0.0);
         m_7899(var12, var11, var20, var19[0], var19[1], var10, m_7887(var3, var6 * (0.88F + var5 * 0.12F)));
         m_7854(var12, f_26540, true);
         float var21 = var10 * 0.85365856F;
         BufferBuilder var22 = m_7952();
         m_7893(var22, var11, var20.x, var20.y, var20.z, var10 * (0.55F + var5 * 0.22F), m_7887(m_7817(var3, 0.42F), var6 * (0.16F + var5 * 0.14F)));
         m_7893(var22, var11, var20.x, var20.y, var20.z, var10 * 0.22F, m_7887(m_7817(var3, 0.78F), var6 * (0.28F + var5 * 0.2F)));

         for (int var23 = 0; var23 < 5; var23++) {
            Vec3 var24 = m_7823(var20, var19[0], var19[1], var21, var23);
            float var25 = 0.72F + 0.28F * (float)Math.sin(f_26545 * 4.2 + var23 * 1.256637);
            m_7893(var22, var11, var24.x, var24.y, var24.z, 0.11F * var25, m_7887(m_7817(var3, 0.88F), var6 * 0.9F));
            m_7893(var22, var11, var24.x, var24.y, var24.z, 0.2F * var25, m_7887(m_7817(var3, 0.5F), var6 * 0.32F));
         }

         int[] var35 = new int[]{0, 2, 4, 1, 3};

         for (int var36 = 0; var36 < 3; var36++) {
            float var38 = (f_26545 * 0.42F + var36 / 3.0F) % 1.0F;
            float var26 = var38 * 5.0F;
            int var27 = Math.min(4, (int)var26);
            float var28 = var26 - var27;
            Vec3 var29 = m_7823(var20, var19[0], var19[1], var21, var35[var27]);
            Vec3 var30 = m_7823(var20, var19[0], var19[1], var21, var35[(var27 + 1) % 5]);
            Vec3 var31 = var29.lerp(var30, var28);
            float var32 = 0.09F - var36 * 0.015F;
            m_7893(var22, var11, var31.x, var31.y, var31.z, var32, m_7887(m_7817(var3, 0.92F), var6 * (0.7F - var36 * 0.16F)));
            m_7893(var22, var11, var31.x, var31.y, var31.z, var32 * 2.1F, m_7887(m_7817(var3, 0.55F), var6 * (0.28F - var36 * 0.06F)));
         }

         for (int var37 = 0; var37 < 5; var37++) {
            double var39 = f_26545 * 1.55 + var37 * f_27527 * 2.0 / 5.0;
            Vec3 var40 = var20.add(var19[0].scale(Math.cos(var39) * var10 * 1.04)).add(var19[1].scale(Math.sin(var39) * var10 * 1.04));
            m_7893(var22, var11, var40.x, var40.y, var40.z, 0.07F, m_7887(m_7817(var3, 0.7F), var6 * 0.48F));
         }

         m_7942(var22, f_26515, true);
      }
   }

   private static void m_7809(BufferBuilder var0, PoseStack var1, float var2, float var3, float var4, float var5, int var6, float var7, float var8) {
      var1.pushPose();
      var1.translate(var2, var3, var4);
      float var9 = var7 * 0.5F;
      float var10 = -var2;
      float var11 = var9 - var3;
      float var12 = -var4;
      float var13 = var10 * var10 + var11 * var11 + var12 * var12;
      if (var13 > 1.0E-8F) {
         float var14 = 1.0F / (float)Math.sqrt(var13);
         float var15 = Mth.clamp(var11 * var14, -1.0F, 1.0F);
         float var16 = (float)Math.toDegrees(Math.atan2(var10, var12));
         float var17 = -((float)Math.toDegrees(Math.asin(var15)));
         var1.mulPose(Axis.YP.rotationDegrees(var16));
         var1.mulPose(Axis.XP.rotationDegrees(var17));
      }

      var1.mulPose(Axis.ZP.rotationDegrees(var8));
      var1.scale(var5 * 1.34F, var5 * 1.34F, var5 * 0.84F);
      Matrix4f var18 = var1.last().pose();
      float var19 = 0.36F;
      float var20 = 1.12F;
      m_7845(var0, var18, 0.0F, 0.0F, var20, -var19, 0.0F, 0.0F, 0.0F, var19, 0.0F, var6);
      m_7845(var0, var18, 0.0F, 0.0F, var20, 0.0F, var19, 0.0F, var19, 0.0F, 0.0F, var6);
      m_7845(var0, var18, 0.0F, 0.0F, var20, var19, 0.0F, 0.0F, 0.0F, -var19, 0.0F, var6);
      m_7845(var0, var18, 0.0F, 0.0F, var20, 0.0F, -var19, 0.0F, -var19, 0.0F, 0.0F, var6);
      m_7845(var0, var18, 0.0F, 0.0F, -var20, 0.0F, var19, 0.0F, -var19, 0.0F, 0.0F, var6);
      m_7845(var0, var18, 0.0F, 0.0F, -var20, var19, 0.0F, 0.0F, 0.0F, var19, 0.0F, var6);
      m_7845(var0, var18, 0.0F, 0.0F, -var20, 0.0F, -var19, 0.0F, var19, 0.0F, 0.0F, var6);
      m_7845(var0, var18, 0.0F, 0.0F, -var20, -var19, 0.0F, 0.0F, 0.0F, -var19, 0.0F, var6);
      var1.popPose();
   }

   private static float m_7810(int var0, int var1) {
      float var2 = (float)var0 / Math.max(1, var1 - 1);
      return 0.72F + 0.28F * (float)Math.sin(f_27176 * Math.pow(var2, 0.75));
   }

   private static void m_7811(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, int var4, float var5, long var6) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      m_7871(var2);
      float var8 = (float)(var6 % 360000L) / 11.5F;
      float var9 = (float)var6 * 0.001F;
      float var10 = 1.18F - 0.18F * var5;
      float var11 = (float)Math.sin(Math.toRadians(var8));
      float var12 = (float)Math.cos(Math.toRadians(var8));
      BufferBuilder var13 = m_7952();
      PoseStack var14 = var0.getPose();

      for (int var15 = 0; var15 < f_26555.size(); var15++) {
         Vector3f var16 = f_26555.get(var15);
         float var17 = var16.x * var12 - var16.z * var11;
         float var18 = var16.x * var11 + var16.z * var12;
         float var19 = var17 * var10;
         float var20 = var18 * var10;
         float var21 = var16.y + (float)(0.035 * Math.sin(var9 * 1.7 + var15 * 2.41));
         m_7954(var13, var14, var1.x + var19, var1.y + var21, var1.z + var20, 0.68F, m_7887(var3, var5 * 0.3F));
         m_7954(var13, var14, var1.x + var19, var1.y + var21, var1.z + var20, 0.36F, m_7887(m_7817(var3, 0.28F), var5 * 0.42F));
      }

      m_7830(var13, f_26515, true, m_7831());
      BufferBuilder var32 = m_7952();

      for (int var33 = 0; var33 < f_26555.size(); var33++) {
         Vector3f var34 = f_26555.get(var33);
         float var35 = var34.x * var12 - var34.z * var11;
         float var36 = var34.x * var11 + var34.z * var12;
         float var37 = var35 * var10;
         float var38 = var36 * var10;
         float var22 = var34.y + (float)(0.035 * Math.sin(var9 * 1.7 + var33 * 2.41));
         float var23 = var8 * (var33 % 2 == 0 ? 1.15F : -0.9F) + var33 * 37.7F;
         float var24 = (float)Math.sin(var33 * 91.345F + 12.17F) * 43758.547F;
         var24 -= (float)Math.floor(var24);
         float var25 = 0.075F + var24 * 0.115F;
         float var26 = 0.03F + ((float)Math.sin(var9 * 0.8F + var33 * 1.73F) + 1.0F) * 0.06F;
         int var27 = CombatColors.m_2303(
            CombatColors.m_2309(var3), CombatColors.m_2299(var3), CombatColors.m_2308(var3), Math.round(255.0F * var5)
         );
         int var28 = CombatColors.m_2303(
            CombatColors.m_2309(var3), CombatColors.m_2299(var3), CombatColors.m_2308(var3), Math.round(155.0F * var5)
         );
         int var29 = m_7817(var27, var26);
         int var30 = CombatColors.m_2302(var29, -1, 0.7F);
         var14.pushPose();
         var14.translate(var1.x, var1.y, var1.z);
         m_7944(var32, var14, var37, var22, var38, var25 * 1.08F, var29, var30, var23, true, var33, true);
         m_7944(var32, var14, var37, var22, var38, var25, var29, var30, var23, true, var33, false);
         var14.popPose();
      }

      m_7825(var32, m_7821());
   }

   private static void m_7812(BufferBuilder var0, Matrix4f var1, float var2, float var3, float var4, float var5, float var6, int var7) {
      m_7931(var0, var1, var2, var3, var4, var5, var6, CombatColors.m_2298(var7));
   }

   private static void m_7813(BufferBuilder var0, Matrix4f var1, double var2, double var4, double var6, int var8, float var9) {
      if (!(var9 <= 0.01F)) {
         float var10 = m_7832(f_26590);
         double var11 = 3.1 * var10;
         int var13 = m_7887(var8, var9);
         int var14 = m_7887(var8, var9 * 0.4F);
         float var15 = 0.12F + 0.24F * (1.0F - var10);
         double var16 = 0.55 * var10;

         for (int var18 = 0; var18 < 26; var18++) {
            double var19 = f_27009 * var18 / 26.0 + f_26592 * 0.15;
            double var21 = var2 + Math.cos(var19) * var11;
            double var23 = var6 + Math.sin(var19) * var11;
            double var25 = var4 + Math.sin(var19 * 3.0 + f_26590 * 6.0F) * var16 * 0.35;
            m_7893(var0, var1, var21, var25, var23, var15, var13);
            m_7893(var0, var1, var21, var25, var23, var15 * 1.5F, var14);
         }
      }
   }

   private static void m_7814(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4) {
      BufferBuilder var5 = CombatVertexAllocator.m_4161().m_4162(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      Vec3 var6 = var1.subtract(mc.gameRenderer.mainCamera().position());
      PoseStack var7 = new PoseStack();
      var7.mulPose(var0.getPose().last().pose());
      double var8 = f_26545 * 240.0;
      Quaternionf var10 = mc.gameRenderer.mainCamera().rotation();
      OrbitGeometry.m_6442(var2.getBbWidth(), var2.getBbHeight(), var8, var4, f_26655, f_26656, (var5x, var6x, var7x, var8x, var9) -> {
         if (var9 != 0) {
            var7.pushPose();
            var7.translate(var6.x + var5x, var6.y + var6x, var6.z + var7x);
            var7.mulPose(var10);
            CombatQuadGeometry.m_1813(var5, var7.last().pose(), var8x, var3 & 16777215 | var9 << 24);
            var7.popPose();
         }
      });
      m_7830(var5, f_26652, false, f_26657 ? f_26654 : f_26653);
   }

   private static float m_7815(float var0, float var1) {
      float var2 = 0.0F;
      float var3 = 0.5F;
      float var4 = 1.0F;

      for (int var5 = 0; var5 < 4; var5++) {
         var2 += m_7924(var0 * var4, var1 * var4) * var3;
         var4 *= 2.03F;
         var3 *= 0.5F;
      }

      return Mth.clamp(var2, 0.0F, 1.0F);
   }

   private static void m_7816(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4) {
      if (f_26611 != var2.getId()) {
         f_26609.clear();
         f_26610 = 0L;
         f_26611 = var2.getId();
      }

      Vec3 var5 = mc.gameRenderer.mainCamera().position();
      double var6 = var1.x;
      double var8 = var1.y + var2.getBbHeight() / 2.0;
      double var10 = var1.z;
      double var12 = var6 - var5.x;
      double var14 = var8 - var5.y;
      double var16 = var10 - var5.z;
      long var18 = System.currentTimeMillis();
      PoseStack var20 = var0.getPose();
      BufferBuilder var21 = m_7952();
      m_7840(var20, var21, var5, var4, var3);

      for (int var22 = 0; var22 < 20; var22++) {
         float var23 = 1.0F - var22 / 20.0F * 0.7F;
         double var24 = 0.15 * (var18 - var22 * 10.0) / 25.0;
         float var26 = (float)(0.285 * var23);
         m_7873(var20, var21, var12, var14, var16, var24, 1.0, 1.0, -1.0, var26, var4, var3);
         m_7873(var20, var21, var12, var14, var16, var24, -1.0, 1.0, -1.0, var26, var4, var3);
         m_7873(var20, var21, var12, var14, var16, var24, -1.0, -1.0, 1.0, var26, var4, var3);
      }

      m_7951(var6, var8, var10, var18, var4);
      m_7942(var21, f_26513, true);
   }

   private static int m_7817(int var0, float var1) {
      var1 = Mth.clamp(var1, 0.0F, 1.0F);
      return CombatColors.m_2303(
         Math.round(Mth.lerpInt(var1, CombatColors.m_2309(var0), 255)),
         Math.round(Mth.lerpInt(var1, CombatColors.m_2299(var0), 255)),
         Math.round(Mth.lerpInt(var1, CombatColors.m_2308(var0), 255)),
         CombatColors.m_2297(var0)
      );
   }

   private static void m_7818(
      BufferBuilder var0,
      Matrix4f var1,
      Vec3 var2,
      Vec3 var3,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      double var14,
      float var16,
      int var17
   ) {
      for (int var18 = -1; var18 <= 1; var18 += 2) {
         for (int var19 = -1; var19 <= 1; var19 += 2) {
            for (int var20 = -1; var20 <= 1; var20 += 2) {
               double var21 = 1.0 - Math.pow(1.0F - var16, 3.0);
               Vec3 var23 = m_7894(new Vec3(var18 * var8, var19 * var8, var20 * var8), var4, var6, 0.0);
               Vec3 var24 = new Vec3(var23.x, 0.0, var23.z).normalize();
               double var25 = var21 * (0.19 + (var19 > 0 ? 0.02 : 0.0) + (var18 * var20 > 0 ? 0.014 : 0.0));
               double var27 = m_7932(var23.y, var14, var16, var18, var19, var20);
               Vec3 var29 = new Vec3(var23.x + var24.x * var25, var27, var23.z + var24.z * var25);
               double var30 = var21 * (0.3 + (var18 * var19 * var20 > 0 ? 0.12 : 0.05));
               Vec3 var32 = m_7894(new Vec3(-var18, 0.0, 0.0), var4, var6, 0.0);
               Vec3 var33 = m_7894(new Vec3(0.0, -var19, 0.0), var4, var6, 0.0);
               Vec3 var34 = m_7894(new Vec3(0.0, 0.0, -var20), var4, var6, 0.0);
               var32 = m_7894(var32, var30 * var20, var30 * var19, var30 * var18);
               var33 = m_7894(var33, var30 * var20, var30 * var19, var30 * var18);
               var34 = m_7894(var34, var30 * var20, var30 * var19, var30 * var18);
               m_7863(var0, var1, var2, var3, var29, var32, var33, var34, var10, var12, var17);
            }
         }
      }
   }

   private static int m_7819(int var0, int var1, float var2) {
      float var3 = ((float)System.currentTimeMillis() / 10.0F + var2) % 360.0F;
      float var4 = ((float)Math.sin(Math.toRadians(var3)) + 1.0F) * 0.5F;
      return CombatColors.m_2302(var0, var1, var4);
   }

   private static Vec3[] m_7820(Vec3[] var0, double var1) {
      double var3 = Math.cos(var1);
      double var5 = Math.sin(var1);
      Vec3 var7 = var0[0];
      Vec3 var8 = var0[1];
      return new Vec3[]{var7.scale(var3).add(var8.scale(var5)), var7.scale(-var5).add(var8.scale(var3))};
   }

   private static RenderPipeline m_7821() {
      return f_26602 ? f_26524 : f_26523;
   }

   private static void m_7822(BufferBuilder var0, Matrix4f var1, double var2, double var4, double var6, float var8, float var9, float var10, int var11) {
      Vector4f var12 = CombatColors.m_2298(var11);
      float var13 = var8 * 0.5F;
      float var14 = (float)Math.toRadians(var9);
      float var15 = (float)Math.toRadians(var10);
      float var16 = (float)Math.cos(var14) * var13;
      float var17 = (float)Math.sin(var14) * var13;
      float var18 = (float)Math.sin(var14) * (float)Math.sin(var15) * var13;
      float var19 = (float)Math.cos(var15) * var13;
      float var20 = -((float)Math.cos(var14)) * (float)Math.sin(var15) * var13;
      m_7931(var0, var1, (float)var2 - var16 - var18, (float)var4 - var19, (float)var6 - var17 - var20, 0.0F, 1.0F, var12);
      m_7931(var0, var1, (float)var2 - var16 + var18, (float)var4 + var19, (float)var6 - var17 + var20, 0.0F, 0.0F, var12);
      m_7931(var0, var1, (float)var2 + var16 + var18, (float)var4 + var19, (float)var6 + var17 + var20, 1.0F, 0.0F, var12);
      m_7931(var0, var1, (float)var2 + var16 - var18, (float)var4 - var19, (float)var6 + var17 - var20, 1.0F, 1.0F, var12);
   }

   private static Vec3 m_7823(Vec3 var0, Vec3 var1, Vec3 var2, float var3, int var4) {
      double var5 = f_27536 + var4 * f_27537 * 2.0 / 5.0;
      return var0.add(var1.scale(Math.cos(var5) * var3)).add(var2.scale(Math.sin(var5) * var3));
   }

   private static TextureTarget m_7824(int var0, int var1) {
      if (f_26560 == null) {
         f_26560 = new TextureTarget("target-esp-lightning-glow-mask", var0, var1, true, GpuFormat.RGBA8_UNORM);
      } else if (f_26560.width != var0 || f_26560.height != var1) {
         f_26560.resize(var0, var1);
      }

      return f_26560;
   }

   private static void m_7825(BufferBuilder var0, RenderPipeline var1) {
      m_7854(var0, var1, false);
   }

   private static int m_7826(LivingEntity var0, int var1, float var2, long var3) {
      if (f_26660 != var0.getId()) {
         f_26660 = var0.getId();
         f_26661 = var0.hurtTime;
         f_26659 = 0L;
         f_26658 = 0.0F;
      }

      if (var0.hurtTime > f_26661) {
         f_26659 = var3;
      }

      f_26661 = var0.hurtTime;
      if (!f_26596) {
         f_26658 = 0.0F;
         return var1;
      } else {
         float var5 = Mth.clamp((float)(var3 - f_26659) / 260.0F, 0.0F, 1.0F);
         float var6 = 1.0F - m_7937(var5);
         float var7 = 1.0F - (float)Math.pow(0.9, var2 * 60.0F);
         f_26658 = Mth.lerp(var7, f_26658, var6);
         return f_26658 < 0.05F ? var1 : CombatColors.m_2302(var1, -52686, f_26658);
      }
   }

   private static float m_7827(float var0) {
      return var0 * var0 * var0 * (var0 * (var0 * 6.0F - 15.0F) + 10.0F);
   }

   private static List<Vec3> m_7828(Vec3 var0, Vec3 var1, int var2, double var3) {
      if (var2 <= 0) {
         ArrayList var13 = new ArrayList(2);
         var13.add(var0);
         var13.add(var1);
         return var13;
      } else {
         Vec3 var5 = var1.subtract(var0);
         Vec3 var6 = var0.add(var5.scale(0.5));
         Vec3 var7 = m_7928(var5);
         double var8 = (f_26557.nextDouble() - 0.5) * 2.0 * var3;
         Vec3 var10 = var6.add(var7.scale(var8));
         List var11 = m_7828(var0, var10, var2 - 1, var3 * 0.5);
         List var12 = m_7828(var10, var1, var2 - 1, var3 * 0.5);
         var11.remove(var11.size() - 1);
         var11.addAll(var12);
         return var11;
      }
   }

   private static RenderPipeline m_7829() {
      return f_26602 ? f_26527 : f_26526;
   }

   private static void m_7830(BufferBuilder var0, Identifier var1, boolean var2, RenderPipeline var3) {
      MeshData var4 = var0.build();
      if (var4 != null) {
         GpuBuffer var5 = null;

         try {
            DrawState var6 = var4.drawState();
            int var7 = var6.indexCount();
            AutoStorageIndexBuffer var8 = RenderSystem.getSequentialBuffer(var6.primitiveTopology());
            GpuBuffer var9 = var8.getBuffer(var7);
            IndexType var10 = var8.type();
            var5 = RenderSystem.getDevice().createBuffer(() -> "targetEspVertices", 32, var4.vertexBuffer());
            if (var2 && f_26628) {
               CombatDistortionRenderer.f_31830.m_8874(var5, var9, var10, var7, f_26629);
            }

            RenderTarget var11 = mc.gameRenderer.mainRenderTarget();
            CommandEncoder var12 = RenderSystem.getDevice().createCommandEncoder();
            AbstractTexture var13 = mc.getTextureManager().getTexture(var1);
            RenderPass var14 = var12.createRenderPass(
               () -> "targetEsp", var11.getColorTextureView(), Optional.empty(), var11.getDepthTextureView(), OptionalDouble.empty()
            );

            try {
               RenderSystem.bindDefaultUniforms(var14);
               var14.setPipeline(var3);
               var14.bindTexture("Sampler0", var13.getTextureView(), var13.getSampler());
               var14.setVertexBuffer(0, var5.slice());
               var14.setIndexBuffer(var9, var10);
               var14.drawIndexed(var7, 1, 0, 0, 0);
            } catch (Throwable var22) {
               if (var14 != null) {
                  try {
                     var14.close();
                  } catch (Throwable var21) {
                     var22.addSuppressed(var21);
                  }
               }

               throw var22;
            }

            if (var14 != null) {
               var14.close();
            }
         } finally {
            if (var5 != null) {
               var5.close();
            }

            var4.close();
         }
      }
   }

   private static RenderPipeline m_7831() {
      return f_26602 ? f_26520 : f_26525;
   }

   private static float m_7832(float var0) {
      var0 = Mth.clamp(var0, 0.0F, 1.0F);
      return var0 * var0 * (3.0F - 2.0F * var0);
   }

   private static void m_7833(BufferBuilder var0, Identifier var1) {
      m_7942(var0, var1, false);
   }

   private static void m_7834(BufferBuilder var0, Matrix4f var1, List<Vec3> var2, float var3, float var4, int var5, float var6, float var7) {
      int var8 = var2.size() - 1;
      if (var8 > 0 && !(var3 <= 0.0F) && !(var4 <= 1.0E-4F)) {
         float var9 = var3 * var8;

         for (int var10 = 0; var10 < var8 && !(var9 <= var10); var10++) {
            float var11 = Mth.clamp(var9 - var10, 0.0F, 1.0F);
            float var12 = m_7858(var10 + 1, var6);
            float var13 = 1.0F - m_7832(Mth.clamp((var7 - var12 * 0.75F) / 0.28F, 0.0F, 1.0F));
            if (!(var13 <= 0.01F)) {
               Vec3 var14 = (Vec3)var2.get(var10);
               Vec3 var15 = (Vec3)var2.get(var10 + 1);
               Vec3 var16 = var11 < 1.0F ? var14.lerp(var15, var11) : var15;
               Vec3 var17 = var16.subtract(var14);
               if (!(var17.lengthSqr() < 1.0E-8)) {
                  var17 = var17.normalize();
                  Vec3 var18 = Math.abs(var17.y) < 0.88 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
                  Vec3 var19 = var17.cross(var18);
                  if (var19.lengthSqr() < 1.0E-8) {
                     var19 = new Vec3(0.0, 0.0, 1.0);
                  }

                  var19 = var19.normalize();
                  Vec3 var20 = var17.cross(var19).normalize();
                  if (var10 > 0) {
                     var14 = var14.subtract(var17.scale(var4 * 0.45));
                  }

                  if (var11 >= 1.0F && var10 < var8 - 1) {
                     var16 = var16.add(var17.scale(var4 * 0.45));
                  }

                  float var21 = (var10 + var11 * 0.5F) / var8;
                  float var22 = Mth.lerp(var21, 1.18F, 0.52F);
                  float var23 = var11 < 1.0F ? 1.18F : 1.0F;
                  float var24 = var4 * var22 * var23;
                  float var25 = var24 * Mth.lerp(var11, 1.0F, 0.72F);
                  float var26 = (1.0F - var21 * 0.12F) * (0.88F + var12 * 0.12F) * var13;
                  int var27 = m_7887(var5, var26);
                  byte var28 = 6;

                  for (int var29 = 0; var29 < var28; var29++) {
                     double var30 = f_27352 * var29 / var28;
                     double var32 = f_27353 * (var29 + 1) / var28;
                     Vec3 var34 = var14.add(var19.scale(Math.cos(var30) * var24)).add(var20.scale(Math.sin(var30) * var24));
                     Vec3 var35 = var14.add(var19.scale(Math.cos(var32) * var24)).add(var20.scale(Math.sin(var32) * var24));
                     Vec3 var36 = var16.add(var19.scale(Math.cos(var30) * var25)).add(var20.scale(Math.sin(var30) * var25));
                     Vec3 var37 = var16.add(var19.scale(Math.cos(var32) * var25)).add(var20.scale(Math.sin(var32) * var25));
                     m_7812(var0, var1, (float)var34.x, (float)var34.y, (float)var34.z, 0.0F, (float)var29 / var28, var27);
                     m_7812(var0, var1, (float)var35.x, (float)var35.y, (float)var35.z, 0.0F, (var29 + 1.0F) / var28, var27);
                     m_7812(var0, var1, (float)var37.x, (float)var37.y, (float)var37.z, var11, (var29 + 1.0F) / var28, var27);
                     m_7812(var0, var1, (float)var36.x, (float)var36.y, (float)var36.z, var11, (float)var29 / var28, var27);
                  }
               }
            }
         }
      }
   }

   private static void m_7835(BufferBuilder var0, Matrix4f var1, Vec3 var2, Vec3 var3, Vec3 var4, double var5, int var7, int var8, int var9) {
      for (int var10 = 0; var10 < var7; var10++) {
         double var11 = f_26940 + f_26941 * var10 / var7;
         double var13 = f_26942 + f_26943 * (var10 + 1) / var7;

         for (int var15 = 0; var15 < var8; var15++) {
            double var16 = f_26944 * var15 / var8;
            double var18 = f_26945 * (var15 + 1) / var8;
            Vec3 var20 = m_7878(var4, var5, var11, var16);
            Vec3 var21 = m_7878(var4, var5, var11, var18);
            Vec3 var22 = m_7878(var4, var5, var13, var18);
            Vec3 var23 = m_7878(var4, var5, var13, var16);
            m_7945(var0, var1, m_7870(var20, var2, var3), m_7870(var21, var2, var3), m_7870(var22, var2, var3), m_7870(var23, var2, var3), var9);
         }
      }
   }

   private static void m_7836(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, int var4, float var5) {
      Vec3 var6 = mc.gameRenderer.mainCamera().position();
      Vec3 var7 = var1.add(0.0, var2.getBbHeight() * 0.5, 0.0).subtract(var6);
      BufferBuilder var8 = m_7952();
      PoseStack var9 = var0.getPose();
      var9.pushPose();
      var9.translate(var7.x, var7.y, var7.z);
      var9.scale(1.5F, 1.5F, 1.5F);
      double var10 = (System.currentTimeMillis() - f_26564) / 4.0;

      for (int var12 = 0; var12 < 4; var12++) {
         for (int var13 = 0; var13 < 9; var13++) {
            var9.pushPose();
            float var14 = var13 / 8.0F;
            float var15 = (0.55F * (1.0F - var14) + 0.2F * var14) * 0.5F;
            double var16 = var10 - var12 * 125.0;
            double var18 = 0.2 * (var16 - var13 * 10.0) / 15.0;
            double var20 = 0.0;
            double var22 = -Math.cos(var18) * 0.45;
            double var24 = Math.sin(var18) * 0.45;
            switch (var12) {
               case 0:
                  var20 += var13 * 0.045F;
                  var9.translate(var22, var20 + 0.35F, var24);
                  break;
               case 1:
               case 3:
                  var20 -= var13 * 0.045F;
                  var9.translate(var22, var20 - 0.35F, var24);
                  break;
               case 2:
               default:
                  var20 += var13 * 0.045F;
                  var9.translate(var22, var20, var24);
            }

            int var26 = m_7887(m_7909(var3, var4, var13 * 25, false), var5);
            var9.mulPose(mc.gameRenderer.mainCamera().rotation());
            Matrix4f var27 = var9.last().pose();
            m_7850(var8, var27, var15, m_7920(var26));
            if (f_26630) {
               int var28 = m_7887(m_7817(var26, 0.2F), 0.35F);
               m_7850(var8, var27, var15 * 1.5F, m_7920(var28));
               m_7850(var8, var27, var15 * 2.25F, m_7920(m_7887(var28, 0.2F)));
            }

            var9.popPose();
         }
      }

      var9.popPose();
      m_7872(var8, f_26514);
   }

   private static Vec3 m_7837(double var0, double var2, double var4, double var6) {
      double var8 = Math.sin(var0) * 0.7;
      double var10 = Math.cos(var0) * 0.7;
      if (var2 == 1.0 && var4 == 1.0 && var6 == -1.0) {
         return new Vec3(var8, var10, -var10);
      } else {
         return var2 == -1.0 && var4 == 1.0 && var6 == -1.0 ? new Vec3(-var8, var8, -var10) : new Vec3(-var8, -var8, var10);
      }
   }

   private static void m_7839(
      BufferBuilder var0,
      Matrix4f var1,
      double var2,
      double var4,
      double var6,
      long var8,
      double var10,
      float var12,
      float var13,
      int var14,
      int var15,
      int var16
   ) {
      double var17 = 0.1 * (1.0 + 1.7 * var13);
      float var19 = 0.7F + 0.25F * var13;

      for (int var20 = 0; var20 < 20; var20++) {
         float var21 = 1.0F - var20 / 20.0F * var19;
         double var22 = var8 * 0.0035 - var20 * var17 + f_26592;
         double var24 = Math.sin(var22) * var10;
         double var26 = Math.cos(var22) * var10;
         double var28 = var2;
         double var30 = var4;
         double var32 = var6;
         switch (var16) {
            case 0:
               var28 += var24;
               var30 += var26;
               var32 -= var26;
               break;
            case 1:
               var28 -= var24;
               var30 += var24;
               var32 -= var26;
               break;
            default:
               var28 -= var24;
               var30 -= var24;
               var32 += var26;
         }

         float var34 = 4.0F * var21 * var12;
         m_7893(var0, var1, var28, var30, var32, var34 * 0.12F, var14);
         m_7893(var0, var1, var28, var30, var32, var34 * 0.21F, var15);
      }
   }

   private static void m_7840(PoseStack var0, BufferBuilder var1, Vec3 var2, float var3, int var4) {
      long var5 = System.currentTimeMillis();
      Iterator var7 = f_26609.iterator();

      while (var7.hasNext()) {
         CombatTargetEffects.GhostPoint var8 = (CombatTargetEffects.GhostPoint)var7.next();
         long var9 = var5 - var8.f_26460;
         if (var9 > 900L) {
            var7.remove();
         } else {
            float var11 = 1.0F - (float)var9 / 900.0F;
            if (var11 <= 0.0F) {
               var7.remove();
            } else {
               float var12 = Mth.clamp(var3 * var8.f_26462 * var11 * 1.25F, 0.0F, 1.0F);
               float var13 = var8.f_26461 * (1.05F + 0.4F * var11);
               m_7949(var0, var1, var8.f_26459.x - var2.x, var8.f_26459.y - var2.y, var8.f_26459.z - var2.z, var13, var12, var4);
            }
         }
      }
   }

   private static boolean m_7841(String var0) {
      if ("Молнии".equals(var0)) {
         return f_26604;
      } else {
         return "Кристалики".equals(var0) ? f_26602 : true;
      }
   }

   private static BufferBuilder m_7842() {
      return CombatVertexAllocator.m_4161().m_4162(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
   }

   private static void m_7843(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      float var5 = f_26545 * f_26570;
      float var6 = var2.getBbWidth() * 0.9F;
      float var7 = var2.getBbHeight();
      float var8 = m_7937(var4);
      int var9 = m_7898(var3, 0.5F);
      short var10 = 144;
      byte var11 = 32;
      Matrix4f var12 = var0.getPose().last().pose();
      BufferBuilder var13 = m_7952();
      double var14 = Math.abs(Math.sin(var5)) * var7;
      double var16 = Math.abs(Math.sin(var5 - 0.4F)) * var7;

      for (int var18 = 0; var18 < var10; var18++) {
         double var19 = f_27564 * var18 / var10;
         double var21 = var1.x + Math.cos(var19) * var6;
         double var23 = var1.z + Math.sin(var19) * var6;
         int var25 = m_7819(var3, var9, var18 * (360.0F / var10));
         m_7893(var13, var12, var21, var1.y + var14, var23, 0.13F, m_7887(var25, var8 * 0.52F));

         for (int var26 = 1; var26 <= var11; var26++) {
            float var27 = (float)var26 / var11;
            double var28 = var14 + (var16 - var14) * var27;
            float var30 = var8 * (1.0F - var27 * 0.82F) * 0.17F;
            float var31 = Mth.lerp(var27, 0.115F, 0.085F);
            m_7893(var13, var12, var21, var1.y + var28, var23, var31, m_7887(var25, var30));
         }
      }

      m_7942(var13, f_26518, true);
   }

   private static void m_7844(BufferBuilder var0, Matrix4f var1, List<Vec3> var2, float var3, float var4, int var5, int var6, float var7) {
      int var8 = var2.size() - 1;
      if (var8 > 0 && !(var4 <= 1.0E-4F) && !(var3 <= 0.001F)) {
         float var9 = var3 * var8;
         int var10 = Math.min(var8 - 1, (int)var9);
         Vec3[] var11 = new Vec3[var10 + 2];
         Vec3[] var12 = new Vec3[var10 + 2];
         Vec3[] var13 = new Vec3[var10 + 2];
         Vec3 var14 = null;

         for (int var15 = 0; var15 < var11.length; var15++) {
            Vec3 var16 = (Vec3)var2.get(Math.max(0, var15 - 1));
            Vec3 var17 = (Vec3)var2.get(Math.min(var2.size() - 1, var15 + 1));
            Vec3 var18 = var17.subtract(var16);
            if (var18.lengthSqr() < 1.0E-9) {
               var18 = new Vec3(0.0, 1.0, 0.0);
            }

            var18 = var18.normalize();
            Vec3 var19 = Math.abs(var18.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            Vec3 var20 = var18.cross(var19);
            if (var20.lengthSqr() < 1.0E-9) {
               var20 = new Vec3(1.0, 0.0, 0.0);
            }

            var20 = var20.normalize();
            if (var14 != null && var20.dot(var14) < 0.0) {
               var20 = var20.scale(-1.0);
            }

            if (var14 != null) {
               var20 = var14.add(var20).normalize();
               if (var20.lengthSqr() < 1.0E-9) {
                  var20 = var14;
               }
            }

            Vec3 var21 = var18.cross(var20).normalize();
            var11[var15] = var18;
            var12[var15] = var20;
            var13[var15] = var21;
            var14 = var20;
         }

         float var45 = var6 / 4.0F;
         float var46 = var45 + 0.25F;
         byte var47 = 6;
         float var49 = var4;

         for (int var50 = 0; var50 <= var10; var50++) {
            float var52 = Mth.clamp(var9 - var50, 0.0F, 1.0F);
            if (var52 <= 0.001F) {
               break;
            }

            float var53 = var49 * m_7810(var50, var2.size());
            float var22 = var49 * m_7810(var50 + 1, var2.size());
            if (var52 < 1.0F) {
               var22 = Mth.lerp(var52, var53, var22);
            }

            Vec3 var23 = (Vec3)var2.get(var50);
            Vec3 var24 = (Vec3)var2.get(var50 + 1);
            if (var52 < 1.0F) {
               var24 = var23.lerp(var24, var52);
            }

            Vec3 var25 = var12[var50];
            Vec3 var26 = var13[var50];
            Vec3 var27 = var50 + 1 < var12.length ? var12[var50 + 1] : var12[var50];
            Vec3 var28 = var50 + 1 < var13.length ? var13[var50 + 1] : var13[var50];
            if (var52 < 1.0F) {
               var27 = var25.lerp(var27, var52).normalize();
               var28 = var26.lerp(var28, var52).normalize();
            }

            float var29 = (float)var50 / var8 * 2.4F + var7;
            float var30 = (var50 + var52) / var8 * 2.4F + var7;
            int var31 = var52 < 1.0F ? m_7887(var5, var52) : var5;

            for (int var32 = 0; var32 < 6; var32++) {
               float var33 = (float)(f_27168 * var32 / 6.0);
               float var34 = (float)(f_27170 * (var32 + 1) / 6.0);
               float var35 = (float)Math.cos(var33);
               float var36 = (float)Math.sin(var33);
               float var37 = (float)Math.cos(var34);
               float var38 = (float)Math.sin(var34);
               Vec3 var39 = var23.add(var25.scale(var35 * var53)).add(var26.scale(var36 * var53));
               Vec3 var40 = var23.add(var25.scale(var37 * var53)).add(var26.scale(var38 * var53));
               Vec3 var41 = var24.add(var27.scale(var37 * var22)).add(var28.scale(var38 * var22));
               Vec3 var42 = var24.add(var27.scale(var35 * var22)).add(var28.scale(var36 * var22));
               float var43 = Mth.lerp(var32 / 6.0F, var45, var46);
               float var44 = Mth.lerp((var32 + 1) / 6.0F, var45, var46);
               m_7812(var0, var1, (float)var39.x, (float)var39.y, (float)var39.z, var29, var43, var5);
               m_7812(var0, var1, (float)var40.x, (float)var40.y, (float)var40.z, var29, var44, var5);
               m_7812(var0, var1, (float)var41.x, (float)var41.y, (float)var41.z, var30, var44, var31);
               m_7812(var0, var1, (float)var42.x, (float)var42.y, (float)var42.z, var30, var43, var31);
            }
         }
      }
   }

   private static void m_7845(
      BufferBuilder var0, Matrix4f var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, int var11
   ) {
      Vector4f var12 = CombatColors.m_2298(var11);
      m_7931(var0, var1, var2, var3, var4, 0.5F, 0.5F, var12);
      m_7931(var0, var1, var5, var6, var7, 0.5F, 0.5F, var12);
      m_7931(var0, var1, var8, var9, var10, 0.5F, 0.5F, var12);
      m_7931(var0, var1, var8, var9, var10, 0.5F, 0.5F, var12);
   }

   private static int m_7846(int var0, int var1, int var2, float var3, float var4) {
      int var5 = false ? CombatColors.m_2304(var2) : m_7909(var0, var1, var2, false);
      int var6 = m_7817(var5, 1.0F - var4);
      if (var3 > 0.0F) {
         int var7 = CombatColors.m_2300(-3329486, -29556, var2, 4);
         var6 = CombatColors.m_2302(var6, var7, var3);
      }

      return var6;
   }

   private static void m_7847(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, float var5, boolean var6) {
      if (!(var4 <= 0.01F)) {
         if (f_26594 != var2.getId()) {
            f_26594 = var2.getId();
            f_26590 = 1.0F;
            f_26591 = 0.0F;
            f_26593 = false;
         }

         boolean var7 = var6 && var2.hurtTime > 7;
         if (var7 && !f_26593) {
            f_26590 = 0.0F;
            f_26593 = true;
         } else if (!var7) {
            f_26593 = false;
         }

         if (f_26590 < 1.0F) {
            f_26590 = Math.min(1.0F, f_26590 + var5 / 1.0F);
         }

         f_26591 = m_7886(f_26591, m_7879(f_26590), var5 / 0.12F);
         float var8 = f_26591;
         f_26592 = f_26592 + var5 * var8 * 9.0;
         Vec3 var9 = var1.subtract(mc.gameRenderer.mainCamera().position());
         double var10 = var9.x;
         double var12 = var9.y + var2.getBbHeight() * 0.5;
         double var14 = var9.z;
         long var16 = System.currentTimeMillis();
         int var18 = CombatColors.m_2302(var3, -1, var8 * 0.65F);
         int var19 = m_7887(var18, Math.min(1.0F, var4 * (1.0F + var8)));
         int var20 = m_7887(var18, var4 * 0.45F);
         double var21 = 0.7 * (1.0 + 1.15 * var8);
         float var23 = 1.0F * (1.0F + 0.18F * var8);
         Matrix4f var24 = var0.getPose().last().pose();
         BufferBuilder var25 = m_7952();

         for (int var26 = 0; var26 < 3; var26++) {
            m_7839(var25, var24, var10, var12, var14, var16, var21, var23, var8, var19, var20, var26);
         }

         if (var8 > 0.001F) {
            m_7813(var25, var24, var10, var12, var14, var18, var4 * var8);
            m_7893(var25, var24, var10, var12, var14, 0.8F * var8, m_7887(var18, var4 * var8 * 0.5F));
         }

         m_7942(var25, f_26518, true);
      }
   }

   private static void m_7848(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, long var5, boolean var7) {
      f_26617.removeIf(var2x -> var5 - var2x.f_26486 > var2x.f_26487);
      f_26617.removeIf(var0x -> var0x.f_26478 < 1.0F);
      if (var7) {
         while (f_26617.size() < 2) {
            f_26617.add(m_7881(var5 - f_26557.nextInt(120)));
         }

         if (var5 - f_26618 >= 200L && f_26617.size() < 3) {
            f_26617.add(m_7881(var5));
            f_26618 = var5;
         }
      }

      if (!f_26617.isEmpty() && !(var4 <= 0.01F)) {
         Vec3 var8 = var1.subtract(mc.gameRenderer.mainCamera().position());
         float var9 = Math.max(var2.getBbHeight(), 0.6F);
         float var10 = Math.max(var2.getBbWidth() * 0.5F, 0.2F);
         float var11 = var10 + 0.38F;
         Matrix4f var12 = var0.getPose().last().pose();
         PoseStack var13 = var0.getPose();
         int var14 = m_7817(var3, 0.45F);
         int var15 = m_7817(var3, 0.85F);
         ArrayList var16 = new ArrayList(f_26617.size());
         float[] var17 = new float[f_26617.size()];

         for (int var18 = 0; var18 < f_26617.size(); var18++) {
            CombatTargetEffects.CrystalParticle var19 = f_26617.get(var18);
            float var20 = m_7960(var19, var5, var4);
            var17[var18] = var20;
            ArrayList var21 = new ArrayList(28);
            if (var20 > 0.02F) {
               int var22 = (int)((var5 - var19.f_26486) / 90L);
               m_7961(var19, var8, var9, var11, var22, var21);
            }

            var16.add(var21);
         }

         BufferBuilder var26 = m_7952();

         for (int var27 = 0; var27 < f_26617.size(); var27++) {
            float var29 = var17[var27];
            List var32 = (List)var16.get(var27);
            if (!(var29 <= 0.02F) && var32.size() >= 2) {
               CombatTargetEffects.CrystalParticle var35 = f_26617.get(var27);

               for (int var23 = 0; var23 < var32.size(); var23++) {
                  Vec3 var24 = (Vec3)var32.get(var23);
                  float var25 = m_7810(var23, var32.size()) * var35.f_26482;
                  m_7954(var26, var13, var24.x, var24.y, var24.z, 0.38F * var25, m_7887(var3, var29 * 0.14F));
                  m_7954(var26, var13, var24.x, var24.y, var24.z, 0.2F * var25, m_7887(var14, var29 * 0.22F));
               }
            }
         }

         m_7830(var26, f_26515, false, f_26535);
         BufferBuilder var28 = m_7952();

         for (int var30 = 0; var30 < f_26617.size(); var30++) {
            float var33 = var17[var30];
            List var36 = (List)var16.get(var30);
            if (!(var33 <= 0.02F) && var36.size() >= 2) {
               float var38 = 0.14F * f_26617.get(var30).f_26482;
               m_7866(var28, var12, var36, var38 * 2.2F, m_7887(var3, var33 * 0.55F));
               m_7866(var28, var12, var36, var38 * 1.35F, m_7887(var14, var33 * 0.75F));
            }
         }

         m_7830(var28, f_26518, false, f_26535);
         BufferBuilder var31 = m_7952();

         for (int var34 = 0; var34 < f_26617.size(); var34++) {
            float var37 = var17[var34];
            List var39 = (List)var16.get(var34);
            if (!(var37 <= 0.02F) && var39.size() >= 2) {
               float var40 = 0.14F * f_26617.get(var34).f_26482;
               m_7866(var31, var12, var39, var40, m_7887(var15, var37));
               m_7866(var31, var12, var39, var40 * 0.45F, m_7887(-1, var37));
            }
         }

         m_7830(var31, f_26518, false, f_26535);
      }
   }

   private static void m_7849(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, int var4, float var5, long var6) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      m_7871(var2);
      byte var8 = 12;
      byte var9 = 20;
      int var10 = var8 + var9;
      float var11 = (float)(var6 % 360000L) / 40.0F;
      float var12 = (float)(var6 % 360000L) / 11.5F;
      float var13 = (float)var6 * 0.001F;
      float var14 = var2.getBbWidth() * 1.27F;
      float var15 = 0.08F * (0.85F + 0.55F * var5);
      float var16 = var2.getBbHeight();
      float var17 = 1.18F - 0.18F * var5;
      PoseStack var18 = var0.getPose();
      BufferBuilder var19 = m_7952();

      for (int var20 = 0; var20 < var10; var20++) {
         CombatTargetEffects.OrbitParticle var21 = m_7859(var20, var8, var9, var11, var12, var13, var14, var16, var15, var17);
         float var22 = 0.18F + var21.size() * 3.6F;
         float var23 = 0.09F + var21.size() * 1.8F;
         m_7954(var19, var18, var1.x + var21.x(), var1.y + var21.y(), var1.z + var21.z(), var22, m_7887(var3, var5 * 0.24F));
         m_7954(var19, var18, var1.x + var21.x(), var1.y + var21.y(), var1.z + var21.z(), var23, m_7887(m_7817(var3, 0.28F), var5 * 0.34F));
      }

      m_7942(var19, f_26515, true);
      int var26 = CombatColors.m_2303(
         CombatColors.m_2309(var3), CombatColors.m_2299(var3), CombatColors.m_2308(var3), Math.round(255.0F * var5)
      );
      int var27 = CombatColors.m_2303(
         CombatColors.m_2309(var3), CombatColors.m_2299(var3), CombatColors.m_2308(var3), Math.round(155.0F * var5)
      );
      BufferBuilder var28 = m_7952();
      var18.pushPose();
      var18.translate(var1.x, var1.y, var1.z);

      for (int var29 = 0; var29 < var10; var29++) {
         CombatTargetEffects.OrbitParticle var24 = m_7859(var29, var8, var9, var11, var12, var13, var14, var16, var15, var17);
         m_7938(var28, var18, var24.x(), var24.y(), var24.z(), var24.size() * 1.08F, var27, var4, var24.spin(), var29, var16, true);
         m_7938(var28, var18, var24.x(), var24.y(), var24.z(), var24.size(), var26, var4, var24.spin(), var29, var16, false);
      }

      var18.popPose();
      m_7825(var28, f_26523);
   }

   private static void m_7850(BufferBuilder var0, Matrix4f var1, float var2, int var3) {
      TargetSurfaceLighting.m_9276(var1, 0.0F, 0.0F, 0.0F, var2 * 2.0F, var3);
      Vector4f var4 = CombatColors.m_2298(var3);
      m_7931(var0, var1, -var2, -var2, 0.0F, 1.0F, 1.0F, var4);
      m_7931(var0, var1, var2, -var2, 0.0F, 0.0F, 1.0F, var4);
      m_7931(var0, var1, var2, var2, 0.0F, 0.0F, 0.0F, var4);
      m_7931(var0, var1, -var2, var2, 0.0F, 1.0F, 0.0F, var4);
   }

   private static void m_7852(PoseStack var0, int var1, float var2, long var3) {
      byte var5 = 5;
      float[] var6 = new float[]{-0.95F, 0.6F};
      float[] var7 = new float[]{0.95F, 0.6F};
      float[] var8 = new float[]{0.0F, -1.05F};
      float[][][] var9 = new float[6][][];

      for (int var10 = 0; var10 <= 5; var10++) {
         float var11 = var10 / 5.0F;
         float var12 = var6[0] + (var8[0] - var6[0]) * var11;
         float var13 = var6[1] + (var8[1] - var6[1]) * var11;
         float var14 = var7[0] + (var8[0] - var7[0]) * var11;
         float var15 = var7[1] + (var8[1] - var7[1]) * var11;
         int var16 = 5 - var10;
         var9[var10] = new float[var16 + 1][2];

         for (int var17 = 0; var17 <= var16; var17++) {
            float var18 = var16 == 0 ? 0.0F : (float)var17 / var16;
            var9[var10][var17][0] = var12 + (var14 - var12) * var18;
            var9[var10][var17][1] = var13 + (var15 - var13) * var18;
         }
      }

      ArrayList var25 = new ArrayList();

      for (int var26 = 0; var26 < 5; var26++) {
         int var28 = 5 - var26;

         for (int var30 = 0; var30 < var28; var30++) {
            var25.add(new float[][]{var9[var26][var30], var9[var26][var30 + 1], var9[var26 + 1][var30]});
            if (var30 < var28 - 1) {
               var25.add(new float[][]{var9[var26][var30 + 1], var9[var26 + 1][var30 + 1], var9[var26 + 1][var30]});
            }
         }
      }

      Matrix4f var27 = var0.last().pose();
      int var29 = CombatColors.m_2312(var1, -1, 0.85F);
      BufferBuilder var31 = m_7952();

      for (int var32 = 0; var32 < var25.size(); var32++) {
         float[][] var34 = (float[][])var25.get(var32);
         float var37 = (var34[0][0] + var34[1][0] + var34[2][0]) / 3.0F;
         float var40 = (var34[0][1] + var34[1][1] + var34[2][1]) / 3.0F;
         float[] var43 = m_7868(var37, var3);
         float var19 = var43[0];
         float var20 = var43[1];
         float var21 = var43[2];
         float var22 = var43[3];
         Matrix4f var23 = m_7874(var27, var37, var40, var19, var20);
         int var24 = m_7922(var40, var32, var1);
         if (var21 > 0.0F) {
            var24 = CombatColors.m_2312(var24, -1, var21 * 0.5F);
         }

         if (var22 > 0.0F) {
            var24 = CombatColors.m_2312(var24, var29, Mth.clamp(var22 * 0.7F, 0.0F, 1.0F));
         }

         m_7845(var31, var23, var34[0][0], var34[0][1], 0.0F, var34[1][0], var34[1][1], 0.0F, var34[2][0], var34[2][1], 0.0F, m_7887(var24, var2));
      }

      m_7854(var31, f_26533, true);
      ArrayList var33 = new ArrayList();

      for (int var35 = 0; var35 < var25.size(); var35++) {
         float[][] var38 = (float[][])var25.get(var35);
         float var41 = (var38[0][0] + var38[1][0] + var38[2][0]) / 3.0F;
         if (m_7868(var41, var3)[2] > 0.08F) {
            var33.add(var35);
         }
      }

      if (!var33.isEmpty()) {
         BufferBuilder var36 = CombatVertexAllocator.m_4161().m_4162(PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

         for (Object var42_obj : var33) {
            int var42 = (Integer)var42_obj;
            float[][] var44 = (float[][])var25.get(var42);
            float var45 = (var44[0][0] + var44[1][0] + var44[2][0]) / 3.0F;
            float var46 = (var44[0][1] + var44[1][1] + var44[2][1]) / 3.0F;
            float[] var47 = m_7868(var45, var3);
            Matrix4f var48 = m_7874(var27, var45, var46, var47[0], var47[1]);
            int var49 = m_7887(CombatColors.m_2312(var1, -1, 0.9F), (80.0F + 175.0F * var47[2]) / 255.0F * var2);
            Vector4f var50 = CombatColors.m_2298(var49);
            m_7865(var36, var48, var44[0][0], var44[0][1], 0.0F, var44[1][0], var44[1][1], 0.0F, var50);
            m_7865(var36, var48, var44[1][0], var44[1][1], 0.0F, var44[2][0], var44[2][1], 0.0F, var50);
            m_7865(var36, var48, var44[2][0], var44[2][1], 0.0F, var44[0][0], var44[0][1], 0.0F, var50);
         }

         m_7923(var36, f_26539);
      }
   }

   private static void m_7853(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, int var4, float var5, float var6) {
      Vec3 var7 = mc.gameRenderer.mainCamera().position();
      float var8 = var2.getBbHeight();
      long var9 = 1500L;
      double var11 = System.currentTimeMillis() % var9;
      boolean var13 = var11 > var9 / 2.0;
      float var14 = (float)(var11 / (var9 / 2.0));
      if (var13) {
         var14--;
      } else {
         var14 = 1.0F - var14;
      }

      var14 = var14 < 0.5F ? 2.0F * var14 * var14 : (float)(1.0 - Math.pow(-2.0 * var14 + 2.0, 2.0) / 2.0);
      double var15 = var8 / 2.0 * (var14 > 0.5F ? 1.0 - var14 : var14) * (var13 ? -1 : 1);
      long var17 = System.nanoTime();
      float var19 = (float)(var17 - f_26566) / 2000000.0F;
      f_26566 = var17;
      float var20 = var2.hurtTime <= 0 ? 0.0F : (var2.hurtTime - var6) / 10.0F;
      f_26565 = f_26565 + Math.max(0.0F, var20) * var19;
      float var21 = (float)(System.currentTimeMillis() - f_26564) / 2.5F + f_26565;
      BufferBuilder var22 = m_7952();
      PoseStack var23 = var0.getPose();
      var23.pushPose();
      var23.translate(var1.x - var7.x, var1.y - var7.y + var8 * var14 + var15, var1.z - var7.z);
      float var24 = 0.7F;

      for (int var25 = 0; var25 < 4; var25++) {
         for (int var26 = 0; var26 < 15; var26++) {
            var23.pushPose();
            float var27 = var26 / 14.0F;
            float var28 = (0.5F * (1.0F - var27) + 0.5F * var27) * var5 * 0.5F;
            float var29 = 0.2F * (var21 - var26 * 3.5F) / 15.0F;
            boolean var30 = var27 < 0.5F;
            float var31 = var30 ? var27 * 2.0F : (1.0F - var27) * 2.0F;
            double var32 = Math.sin(var31 * f_27623) * 2.0;
            Random var34 = new Random(var26 * 12345L);
            double var35 = (var34.nextDouble() - 0.5) * var32;
            double var37 = (var34.nextDouble() - 0.5) * var32;
            double var39 = (var34.nextDouble() - 0.5) * var32;
            var35 = var35 * var5 - var35;
            var37 = var37 * var5 - var37;
            var39 = var39 * var5 - var39;
            switch (var25) {
               case 0:
                  var23.translate(Math.cos(var29) * var24 + var35, var37, Math.sin(var29) * var24 + var39);
                  break;
               case 1:
                  var23.translate(-Math.sin(var29) * var24 + var35, var37, Math.cos(var29) * var24 + var39);
                  break;
               case 2:
                  var23.translate(-Math.cos(var29) * var24 + var35, var37, -Math.sin(var29) * var24 + var39);
                  break;
               default:
                  var23.translate(Math.sin(var29) * var24 + var35, var37, -Math.cos(var29) * var24 + var39);
            }

            int var41 = m_7817(m_7819(var3, var4, var26 * 10.0F), 0.15F);
            int var42 = m_7887(var41, var5 * 0.5882353F);
            var23.mulPose(mc.gameRenderer.mainCamera().rotation());
            m_7850(var22, var23.last().pose(), var28, m_7920(var42));
            var23.popPose();
         }
      }

      var23.popPose();
      m_7872(var22, f_26514);
   }

   private static void m_7854(BufferBuilder var0, RenderPipeline var1, boolean var2) {
      MeshData var3 = var0.build();
      if (var3 != null) {
         GpuBuffer var4 = null;

         try {
            DrawState var5 = var3.drawState();
            int var6 = var5.indexCount();
            AutoStorageIndexBuffer var7 = RenderSystem.getSequentialBuffer(var5.primitiveTopology());
            GpuBuffer var8 = var7.getBuffer(var6);
            IndexType var9 = var7.type();
            var4 = RenderSystem.getDevice().createBuffer(() -> "targetEspColorVertices", 32, var3.vertexBuffer());
            if (var2 && f_26628) {
               CombatDistortionRenderer.f_31830.m_8874(var4, var8, var9, var6, f_26629);
            }

            RenderTarget var10 = mc.gameRenderer.mainRenderTarget();
            CommandEncoder var11 = RenderSystem.getDevice().createCommandEncoder();
            RenderPass var12 = var11.createRenderPass(
               () -> "targetEspColor", var10.getColorTextureView(), Optional.empty(), var10.getDepthTextureView(), OptionalDouble.empty()
            );

            try {
               RenderSystem.bindDefaultUniforms(var12);
               var12.setPipeline(var1);
               var12.setVertexBuffer(0, var4.slice());
               var12.setIndexBuffer(var8, var9);
               var12.drawIndexed(var6, 1, 0, 0, 0);
            } catch (Throwable var20) {
               if (var12 != null) {
                  try {
                     var12.close();
                  } catch (Throwable var19) {
                     var20.addSuppressed(var19);
                  }
               }

               throw var20;
            }

            if (var12 != null) {
               var12.close();
            }
         } finally {
            if (var4 != null) {
               var4.close();
            }

            var3.close();
         }
      }
   }

   private static void m_7855(BufferBuilder var0, Matrix4f var1, float var2, int var3) {
      TargetSurfaceLighting.m_9276(var1, 0.0F, 0.0F, 0.0F, var2, var3);
      Vector4f var4 = CombatColors.m_2298(var3);
      float var5 = var2 * 0.5F;
      m_7931(var0, var1, -var5, -var5, 0.0F, 0.0F, 1.0F, var4);
      m_7931(var0, var1, -var5, var5, 0.0F, 0.0F, 0.0F, var4);
      m_7931(var0, var1, var5, var5, 0.0F, 1.0F, 0.0F, var4);
      m_7931(var0, var1, var5, -var5, 0.0F, 1.0F, 1.0F, var4);
   }

   private static CombatTargetEffects.LightningArc m_7856(LivingEntity var0, Vec3 var1, long var2, float var4) {
      double var5 = Math.max(var0.getBbHeight(), 0.6);
      double var7 = Math.max(var0.getBbWidth() * 0.5, 0.2) + 0.22 + f_26557.nextDouble() * 0.18;
      int var9 = f_26557.nextBoolean() ? 1 : -1;
      float var10 = 1.6F + f_26557.nextFloat() * 1.2F;
      float var11 = var4 * (0.85F + f_26557.nextFloat() * 0.9F) + f_26557.nextFloat() * f_27220 * 2.0F;
      float var12 = f_26557.nextFloat() * 0.82F;
      float var13 = Math.min(1.0F, var12 + 0.1F + f_26557.nextFloat() * 0.18F);
      Vec3 var14 = m_7903(var1, var5, var7, var11, var10, var9, var12);
      Vec3 var15 = m_7903(var1, var5, var7, var11, var10, var9, var13);
      double var16 = var14.distanceTo(var15);
      List var18 = m_7828(var14, var15, 3, Math.max(0.06, var16 * 0.4));
      ArrayList var19 = new ArrayList(var18.size());

      for (Object var21_obj : var18) {
         Vec3 var21 = (Vec3)var21_obj;
         var19.add(var21.subtract(var1));
      }

      float var22 = 0.55F + f_26557.nextFloat() * 0.35F;
      return new CombatTargetEffects.LightningArc(
         var19, var2, System.nanoTime(), 520 + f_26557.nextInt(260), f_26557.nextFloat() * 100.0F, 0.85F + f_26557.nextFloat() * 0.55F, var9, var22
      );
   }

   public static void compositeOutline() {
      f_26644 = false;
      if (f_26645 != null && f_26647 != null) {
         GpuBufferSlice var0 = RenderSystem.getProjectionMatrixBuffer();
         ProjectionType var1 = RenderSystem.getProjectionType();
         CombatWorldRenderEvent var2 = f_26645;
         LivingEntity var3 = f_26646;
         String var4 = f_26647;
         int var5 = f_26648;
         int var6 = f_26649;
         f_26645 = null;
         f_26646 = null;
         f_26647 = null;

         try {
            RenderSystem.setProjectionMatrix(f_26650, f_26651);
            m_7914(var2, var3, var4, var5, var6);
            if (f_26632) {
               compositeBeforeHands();
            }
         } finally {
            RenderSystem.setProjectionMatrix(var0, var1);
         }
      }
   }

   private static float m_7858(int var0, float var1) {
      float var2 = (float)Math.sin(var0 * 12.9898 + var1 * 78.233);
      var2 *= 43758.547F;
      return var2 - (float)Math.floor(var2);
   }

   private static CombatTargetEffects.OrbitParticle m_7859(
      int var0, int var1, int var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9
   ) {
      if (var0 >= var1) {
         int var25 = var0 - var1;
         int var26 = var25 * f_26555.size() / var2;
         Vector3f var27 = f_26555.get(var26);
         float var28 = (float)Math.toRadians(var4);
         float var29 = Mth.sin(var28);
         float var30 = Mth.cos(var28);
         float var31 = var27.x * var30 - var27.z * var29;
         float var33 = var27.x * var29 + var27.z * var30;
         float var35 = Mth.sqrt(var31 * var31 + var33 * var33);
         float var36 = var6 * 0.82F;
         float var20 = Math.max(var35 * 1.18F, var36);
         if (var35 > 1.0E-5F) {
            float var21 = var20 / var35;
            var31 *= var21;
            var33 *= var21;
         }

         var31 *= var9;
         var33 *= var9;
         float var37 = var27.y + (float)Math.sin(var5 * 1.7F + var25 * 2.41F) * 0.035F;
         float var22 = m_7948(var25, 6.8F);
         float var23 = var8 * (0.88F + var22 * 0.5F);
         float var24 = var4 * (var25 % 2 == 0 ? 1.15F : -0.9F) + var25 * 37.7F;
         return new CombatTargetEffects.OrbitParticle(var31, var37, var33, var23, var24);
      } else {
         float var10 = 360.0F * var0 / var1;
         float var11 = (float)Math.toRadians(var10 + var3);
         float var12 = 0.82F + Math.abs((float)Math.sin(var11 * 2.0F)) * 0.42F;
         float var13 = var6 * var12 * var9;
         float var14 = (float)Math.sin(var11) * var13;
         float var15 = (float)Math.cos(var11) * var13;
         float var16 = 0.18F + Math.abs((float)Math.sin(var11 * 1.75F + var3 * 0.02F));
         float var17 = var7 * (0.08F + 0.82F * var16);
         float var18 = var8 * (0.82F + Math.abs((float)Math.sin(var11 * 1.35F)) * 0.45F);
         float var19 = var3 * 1.5F + var10 * 14.0F;
         return new CombatTargetEffects.OrbitParticle(var14, var17, var15, var18, var19);
      }
   }

   private static Vec3[] m_7860(Vec3 var0) {
      Vec3 var1 = Math.abs(var0.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 var2 = var1.cross(var0).normalize();
      Vec3 var3 = var0.cross(var2).normalize();
      return new Vec3[]{var2, var3};
   }

   private static CombatTargetEffects.LightningBurst m_7861(LivingEntity var0, Vec3 var1, long var2) {
      double var4 = Math.max(var0.getBbHeight(), 0.55);
      double var6 = Math.max(var0.getBbWidth() * 0.5, 0.18);
      double var8 = f_26557.nextDouble() * f_27280 * 2.0;
      double var10 = var6 * (0.2 + f_26557.nextDouble() * 0.55);
      double var12 = var4 * (0.2 + f_26557.nextDouble() * 0.55);
      Vec3 var14 = var1.add(Math.cos(var8) * var10, var12, Math.sin(var8) * var10);
      double var15 = var8 + (f_26557.nextDouble() - 0.5) * 1.25;
      double var17 = var6 + 0.9 + f_26557.nextDouble() * 1.6;
      double var19 = var4 + 2.4 + f_26557.nextDouble() * 2.6;
      Vec3 var21 = var1.add(Math.cos(var15) * var17, var19, Math.sin(var15) * var17);
      ArrayList var22 = new ArrayList();
      List var23 = m_7875(var21, var14, 4, 0.55 + f_26557.nextDouble() * 0.35);
      var22.add(new CombatTargetEffects.LightningBranch(var23, 1.0F, 0.0F));
      int var24 = 1 + f_26557.nextInt(3);

      for (int var25 = 0; var25 < var24; var25++) {
         int var26 = 3 + f_26557.nextInt(Math.max(1, var23.size() - 6));
         Vec3 var27 = (Vec3)var23.get(var26);
         Vec3 var28 = var14.subtract(var21);
         if (var28.lengthSqr() < 1.0E-6) {
            var28 = new Vec3(0.0, -1.0, 0.0);
         }

         var28 = var28.normalize();
         Vec3 var29 = var28.cross(new Vec3(0.0, 1.0, 0.0));
         if (var29.lengthSqr() < 1.0E-8) {
            var29 = new Vec3(1.0, 0.0, 0.0);
         }

         var29 = var29.normalize();
         Vec3 var30 = var29.scale(f_26557.nextBoolean() ? 1.0 : -1.0).add(0.0, f_26557.nextDouble() * 0.5 - 0.35, 0.0).normalize();
         double var31 = 0.55 + f_26557.nextDouble() * 1.0;
         Vec3 var33 = var27.add(var30.scale(var31)).add(var28.scale(0.15 + f_26557.nextDouble() * 0.4));
         float var34 = Mth.clamp((float)var26 / (var23.size() - 1) * 0.5F, 0.05F, 0.5F);
         var22.add(
            new CombatTargetEffects.LightningBranch(
               m_7875(var27, var33, 3, 0.22 + f_26557.nextDouble() * 0.18), 0.45F + f_26557.nextFloat() * 0.3F, var34
            )
         );
      }

      if (f_26557.nextFloat() < 0.5F) {
         double var35 = var8 + f_27311 * (0.4 + f_26557.nextDouble() * 0.35) * (f_26557.nextBoolean() ? 1 : -1);
         Vec3 var36 = var1.add(Math.cos(var35) * var6, var4 * (0.3 + f_26557.nextDouble() * 0.4), Math.sin(var35) * var6);
         var22.add(new CombatTargetEffects.LightningBranch(m_7875(var14, var36, 3, 0.16 + f_26557.nextDouble() * 0.12), 0.35F, 0.3F));
      }

      return new CombatTargetEffects.LightningBurst(
         var22, var14, var1, var2, 560 + f_26557.nextInt(340), f_26557.nextFloat() * 100.0F, -1, 0.0, -1.0, 0.4F, 0.6F
      );
   }

   private static void m_7862(Vec3[] var0, int var1, int var2, double var3, Random var5) {
      if (var2 - var1 > 1) {
         int var6 = var1 + var2 >>> 1;
         Vec3 var7 = var0[var1];
         Vec3 var8 = var0[var2];
         Vec3 var9 = var7.add(var8).scale(0.5);
         Vec3 var10 = var8.subtract(var7);
         if (var10.lengthSqr() < 1.0E-8) {
            var0[var6] = var9;
         } else {
            Vec3 var11 = var10.cross(new Vec3(0.0, 1.0, 0.0));
            if (var11.lengthSqr() < 1.0E-8) {
               var11 = var10.cross(new Vec3(1.0, 0.0, 0.0));
            }

            if (var11.lengthSqr() < 1.0E-8) {
               var11 = new Vec3(1.0, 0.0, 0.0);
            }

            var11 = var11.normalize();
            Vec3 var12 = var10.cross(var11);
            if (var12.lengthSqr() < 1.0E-8) {
               var12 = new Vec3(0.0, 1.0, 0.0);
            } else {
               var12 = var12.normalize();
            }

            double var13 = (var5.nextDouble() - 0.5) * 2.0 * var3;
            double var15 = (var5.nextDouble() - 0.5) * var3 * 0.55;
            var0[var6] = var9.add(var11.scale(var13)).add(var12.scale(var15));
            m_7862(var0, var1, var6, var3 * 0.52, var5);
            m_7862(var0, var6, var2, var3 * 0.52, var5);
         }
      }
   }

   private static void m_7863(
      BufferBuilder var0, Matrix4f var1, Vec3 var2, Vec3 var3, Vec3 var4, Vec3 var5, Vec3 var6, Vec3 var7, double var8, double var10, int var12
   ) {
      m_7835(var0, var1, var2, var3, var4, var10, 3, 6, var12);
      Vec3[] var13 = new Vec3[]{var5, var6, var7};

      for (Vec3 var17 : var13) {
         Vec3 var18 = var4.add(var17.scale(var8));
         m_7939(var0, var1, var2, var3, var4, var18, var10, 6, var12);
         m_7835(var0, var1, var2, var3, var18, var10, 3, 6, var12);
      }
   }

   private static void m_7864(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, int var4, boolean var5) {
      long var6 = System.currentTimeMillis();
      if (var5 && var6 - f_26567 >= 16L) {
         f_26567 = var6;
         f_26563 = f_26563 + 4.0;
         double var8 = f_26563 % 360.0;
         float var10 = 0.6F + var2.getBbWidth() * 0.5F;
         float var11 = var2.getBbHeight() / 2.0F;

         for (int var12 = 0; var12 < 4; var12++) {
            float var13 = (var12 - 1) * 0.4F;
            double var14 = var8 + var13 * 360.0;
            double var16 = Math.toRadians(-var14);
            double var18 = Math.sin(var16 + 2.0) * var13;
            double var20 = Math.cos(var16) * var10;
            double var22 = Math.sin(var16) * var10;
            int var24 = m_7909(var3, var4, (int)var8 + 90 * var12, false);
            f_26562.add(new CombatTargetEffects.FadeParticle(var1.x + var20, var1.y + var11 + var18, var1.z + var22, var6, var24));
         }
      }

      Vec3 var25 = mc.gameRenderer.mainCamera().position();
      BufferBuilder var9 = m_7952();
      PoseStack var26 = var0.getPose();
      float var27 = 0.4F;
      Iterator var28 = f_26562.iterator();

      while (var28.hasNext()) {
         CombatTargetEffects.FadeParticle var29 = (CombatTargetEffects.FadeParticle)var28.next();
         float var30 = (float)(var6 - var29.f_26466);
         var29.m_7806(var6);
         if (var30 > 100.0F && var29.f_26468 < 0.01F) {
            var28.remove();
         } else {
            float var15;
            if (var30 < 400.0F) {
               var15 = 1.0F - var30 / 400.0F;
            } else if (var30 < 800.0F) {
               var15 = var30 / 400.0F - 1.0F;
            } else {
               var15 = 1.0F;
            }

            var15 = Mth.clamp(var15, 0.0F, 1.0F);
            if (!(var29.f_26468 <= 0.01F)) {
               var26.pushPose();
               var26.translate(var29.f_26463 - var25.x, var29.f_26464 - var25.y, var29.f_26465 - var25.z);
               var26.mulPose(mc.gameRenderer.mainCamera().rotation());
               m_7850(var9, var26.last().pose(), var27 * var15, m_7920(m_7887(var29.f_26467, var29.f_26468)));
               var26.popPose();
            }
         }
      }

      m_7872(var9, f_26514);
   }

   private static void m_7865(BufferBuilder var0, Matrix4f var1, float var2, float var3, float var4, float var5, float var6, float var7, Vector4f var8) {
      var0.addVertex(var1, var2, var3, var4).setColor(var8.x, var8.y, var8.z, var8.w);
      var0.addVertex(var1, var5, var6, var7).setColor(var8.x, var8.y, var8.z, var8.w);
   }

   private static void m_7866(BufferBuilder var0, Matrix4f var1, List<Vec3> var2, float var3, int var4) {
      if (var2.size() >= 2 && !(var3 <= 1.0E-4F)) {
         Vec3 var5 = Vec3.ZERO;
         Vec3[] var6 = new Vec3[var2.size() - 1];

         for (int var7 = 0; var7 < var2.size() - 1; var7++) {
            Vec3 var8 = (Vec3)var2.get(var7);
            Vec3 var9 = (Vec3)var2.get(var7 + 1);
            Vec3 var10 = var9.subtract(var8);
            if (var10.lengthSqr() < 1.0E-10) {
               var6[var7] = var7 > 0 ? var6[var7 - 1] : new Vec3(1.0, 0.0, 0.0);
            } else {
               var10 = var10.normalize();
               Vec3 var11 = var8.add(var9).scale(0.5);
               Vec3 var12 = var5.subtract(var11);
               if (var12.lengthSqr() < 1.0E-8) {
                  var12 = new Vec3(0.0, 0.0, 1.0);
               }

               Vec3 var13 = var10.cross(var12.normalize());
               if (var13.lengthSqr() < 1.0E-8) {
                  var13 = var10.cross(new Vec3(0.0, 1.0, 0.0));
                  if (var13.lengthSqr() < 1.0E-8) {
                     var13 = new Vec3(1.0, 0.0, 0.0);
                  }
               }

               var13 = var13.normalize();
               if (var7 > 0 && var13.dot(var6[var7 - 1]) < 0.0) {
                  var13 = var13.scale(-1.0);
               }

               var6[var7] = var13;
            }
         }

         Vec3[] var15 = new Vec3[var2.size()];

         for (int var16 = 0; var16 < var2.size(); var16++) {
            Vec3 var18;
            if (var16 == 0) {
               var18 = var6[0];
            } else if (var16 == var2.size() - 1) {
               var18 = var6[var6.length - 1];
            } else {
               Vec3 var22 = var6[var16 - 1];
               Vec3 var24 = var6[var16];
               var18 = var22.add(var24);
               if (var18.lengthSqr() < 1.0E-8) {
                  var18 = var22;
               } else {
                  var18 = var18.normalize();
               }
            }

            var15[var16] = var18.scale(var3 * m_7810(var16, var2.size()));
         }

         for (int var17 = 0; var17 < var2.size() - 1; var17++) {
            Vec3 var20 = (Vec3)var2.get(var17);
            Vec3 var23 = (Vec3)var2.get(var17 + 1);
            Vec3 var25 = var20.subtract(var15[var17]);
            Vec3 var26 = var20.add(var15[var17]);
            Vec3 var28 = var23.subtract(var15[var17 + 1]);
            Vec3 var14 = var23.add(var15[var17 + 1]);
            m_7812(var0, var1, (float)var25.x, (float)var25.y, (float)var25.z, 0.0F, 0.5F, var4);
            m_7812(var0, var1, (float)var26.x, (float)var26.y, (float)var26.z, 1.0F, 0.5F, var4);
            m_7812(var0, var1, (float)var14.x, (float)var14.y, (float)var14.z, 1.0F, 0.5F, var4);
            m_7812(var0, var1, (float)var28.x, (float)var28.y, (float)var28.z, 0.0F, 0.5F, var4);
         }
      }
   }

   public static void m_7867() {
      f_26598 = 0L;
      f_26599 = 0.0F;
      f_26600 = 0.0F;
      f_26543 = null;
      f_26544 = 0.0F;
      f_26545 = 0.0F;
      f_26546 = 0L;
      f_26547 = "";
      f_26548.clear();
      f_26552 = f_26669;
      f_26555.clear();
      f_26556 = f_26670;
      f_26558.clear();
      f_26559 = 0L;
      if (f_26560 != null) {
         f_26560.destroyBuffers();
         f_26560 = null;
      }

      f_26617.clear();
      f_26618 = 0L;
      f_26623.clear();
      f_26624 = 0L;
      f_26606 = "";
      f_26607 = "";
      f_26608 = "";
      f_26609.clear();
      f_26610 = 0L;
      f_26611 = f_26671;
      m_7910();
      TargetCubeRenderer.m_1844();
      m_7892();
      f_26625 = f_26672;
      f_26632 = false;
      f_26635 = 0.0F;
      f_26636 = false;
      f_26637 = false;
      f_26638 = 0.0;
      f_26639 = 0.0;
      f_26640 = 0.0;
      f_26641 = 0.0;
      f_26642 = 0.0;
      f_26643 = 0.0;
   }

   private static float[] m_7868(float var0, long var1) {
      float var3 = 1.0F - Mth.clamp((var0 + 0.95F) / 1.9F, 0.0F, 1.0F);
      float var4 = (float)var1 * 0.002F;
      var4 -= (float)Math.floor(var4);
      float var5 = 0.35F;
      float var6 = 2.2F;
      float var7 = 0.45F;
      float var8 = 1.3F;
      if (var4 >= var5) {
         return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
      } else {
         float var9 = var4 / var5 * var6;
         float var10 = (var9 - var3) / var7;
         if (var10 > 0.0F && var10 < 1.0F) {
            float var15 = var10 * var10 * (3.0F - 2.0F * var10);
            float var16 = (float)(f_27458 * var15);
            float var17 = (float)Math.sin(f_27459 * var10);
            return new float[]{var16, var17 * 0.15F, Math.abs((float)Math.sin(var16)), 0.0F};
         } else if (var10 >= 1.0F && var10 < 1.0F + var8) {
            float var11 = (var10 - 1.0F) / var8;
            float var12 = 1.0F - var11;
            float var13 = 0.5F + 0.5F * (float)Math.sin(var11 * f_27463 * 3.0);
            return new float[]{0.0F, 0.0F, 0.0F, var12 * var13};
         } else {
            return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
         }
      }
   }

   private static RenderPipeline m_7869() {
      return f_26602 ? f_26529 : f_26528;
   }

   private static Vec3 m_7870(Vec3 var0, Vec3 var1, Vec3 var2) {
      return var0.add(var1).subtract(var2);
   }

   private static void m_7871(LivingEntity var0) {
      if (f_26556 != var0.getId()) {
         f_26556 = var0.getId();
         f_26555.clear();
         float var1 = var0.getBbWidth() * 0.85F + 0.12F;
         float var2 = var0.getBbHeight();
         float[][] var3 = new float[][]{
            {18.0F, 0.16F, 0.42F},
            {205.0F, 0.78F, 0.95F},
            {112.0F, 0.35F, 0.6F},
            {300.0F, 0.58F, 0.82F},
            {55.0F, 0.48F, 0.35F},
            {238.0F, 0.2F, 1.05F},
            {150.0F, 0.68F, 0.58F},
            {342.0F, 0.88F, 0.9F},
            {88.0F, 0.55F, 0.48F},
            {275.0F, 0.4F, 1.12F},
            {24.0F, 0.72F, 0.68F},
            {190.0F, 0.3F, 0.38F},
            {128.0F, 0.62F, 0.94F},
            {318.0F, 0.83F, 0.55F},
            {63.0F, 0.12F, 0.76F},
            {224.0F, 0.5F, 1.08F},
            {165.0F, 0.92F, 0.45F},
            {355.0F, 0.26F, 0.87F},
            {98.0F, 0.66F, 0.62F},
            {260.0F, 0.44F, 1.0F},
            {38.0F, 0.76F, 0.52F},
            {208.0F, 0.57F, 0.72F},
            {330.0F, 0.14F, 0.58F},
            {82.0F, 0.32F, 0.5F},
            {176.0F, 0.5F, 0.68F},
            {286.0F, 0.7F, 0.56F},
            {44.0F, 0.88F, 0.74F}
         };

         for (float[] var7 : var3) {
            float var8 = (float)Math.toRadians(var7[0]);
            float var9 = var1 * var7[2];
            f_26555.add(new Vector3f(Mth.cos(var8) * var9, Mth.clamp(var7[1] * var2, 0.08F, var2 - 0.04F), Mth.sin(var8) * var9));
         }
      }
   }

   private static void m_7872(BufferBuilder var0, Identifier var1) {
      m_7830(var0, var1, false, f_26631 ? f_26521 : f_26522);
   }

   private static void m_7873(
      PoseStack var0,
      BufferBuilder var1,
      double var2,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      double var14,
      float var16,
      float var17,
      int var18
   ) {
      Vec3 var19 = m_7837(var8, var10, var12, var14);
      Vec3 var20 = m_7837(var8 + 0.14, var10, var12, var14);
      double var21 = var20.x - var19.x;
      double var23 = var20.y - var19.y;
      double var25 = var20.z - var19.z;
      double var27 = Math.sqrt(var21 * var21 + var23 * var23 + var25 * var25);
      double var29 = var27 > 1.0E-5 ? 1.0 / var27 : 0.0;
      double var31 = var21 * var29;
      double var33 = var23 * var29;
      double var35 = var25 * var29;
      float var37 = (float)Mth.clamp(var27 * 5.0, 0.5, 1.5);
      var0.pushPose();
      var0.translate(
         var2 + var19.x - var31 * (0.1 + var16 * 0.65),
         var4 + var19.y - var33 * (0.1 + var16 * 0.65),
         var6 + var19.z - var35 * (0.1 + var16 * 0.65)
      );
      var0.mulPose(mc.gameRenderer.mainCamera().rotation());
      Matrix4f var38 = var0.last().pose();
      float var39 = var16 * (2.1F + 0.7F * var37);
      m_7855(var1, var38, var39, m_7887(var18, var17 * (0.24F + 0.14F * var37)));
      m_7855(var1, var38, var16, m_7887(var18, var17));
      var0.popPose();
   }

   private static Matrix4f m_7874(Matrix4f var0, float var1, float var2, float var3, float var4) {
      Matrix4f var5 = new Matrix4f(var0);
      var5.translate(var1, var2, var4);
      var5.rotate(var3, 0.0F, 1.0F, 0.0F);
      var5.translate(-var1, -var2, 0.0F);
      return var5;
   }

   private static List<Vec3> m_7875(Vec3 var0, Vec3 var1, int var2, double var3) {
      int var5 = (1 << var2) + 1;
      Vec3[] var6 = new Vec3[var5];
      var6[0] = var0;
      var6[var5 - 1] = var1;
      m_7862(var6, 0, var5 - 1, var3, f_26557);
      ArrayList var7 = new ArrayList(var5);

      for (Vec3 var11 : var6) {
         var7.add(var11);
      }

      return var7;
   }

   public static void compositeBeforeHands() {
      if (f_26632 && f_26627 != null) {
         f_26632 = false;
         RenderTarget var0 = mc.gameRenderer.mainRenderTarget();
         if (var0 != null && var0.getColorTextureView() != null) {
            CombatGlowRenderer.m_8389(f_26627, var0, f_26633, 5.5F, f_26634, 1.0F);
            CommandEncoder var1 = RenderSystem.getDevice().createCommandEncoder();
            RenderPass var2 = var1.createRenderPass(() -> "targetEspCubeComposite", var0.getColorTextureView(), Optional.empty());

            try {
               var2.setPipeline(f_26534);
               var2.bindTexture("InSampler", f_26627.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
               var2.draw(3, 1, 0, 0);
            } catch (Throwable var6) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (var2 != null) {
               var2.close();
            }
         }
      }
   }

   private static Vec3 m_7878(Vec3 var0, double var1, double var3, double var5) {
      double var7 = Math.cos(var3) * var1;
      return var0.add(Math.cos(var5) * var7, Math.sin(var3) * var1, Math.sin(var5) * var7);
   }

   private static float m_7879(float var0) {
      if (var0 <= 0.0F || var0 >= 1.0F) {
         return 0.0F;
      } else {
         return var0 < 0.22F ? m_7832(var0 / 0.22F) : m_7832(1.0F - (var0 - 0.22F) / 0.78F);
      }
   }

   private static CombatTargetEffects.CrystalParticle m_7881(long var0) {
      float var2 = f_26557.nextFloat();
      float var3;
      float var4;
      float var5;
      if (var2 < 0.6F) {
         float var6 = 0.25F + f_26557.nextFloat() * 0.45F;
         float var7 = 0.12F + f_26557.nextFloat() * 0.16F;
         var4 = var6 - var7 * 0.5F;
         var3 = var6 + var7 * 0.5F;
         var5 = 1.05F + f_26557.nextFloat() * 0.55F;
      } else if (var2 < 0.82F) {
         var4 = 0.1F + f_26557.nextFloat() * 0.12F;
         var3 = 0.55F + f_26557.nextFloat() * 0.25F;
         var5 = 1.25F + f_26557.nextFloat() * 0.55F;
      } else {
         var4 = 0.9F - f_26557.nextFloat() * 0.1F;
         var3 = 0.25F + f_26557.nextFloat() * 0.2F;
         var5 = 1.25F + f_26557.nextFloat() * 0.55F;
      }

      return new CombatTargetEffects.CrystalParticle(
         f_26557.nextDouble() * f_27102 * 2.0,
         f_26557.nextBoolean() ? 1 : -1,
         var5,
         var3,
         var4,
         1.0F,
         1.2F + f_26557.nextFloat() * 0.35F,
         0.03F,
         f_26557.nextInt(4),
         f_26557.nextFloat() * 100.0F,
         var0,
         1500 + f_26557.nextInt(500),
         f_26557.nextFloat() * 100.0F
      );
   }

   private CombatTargetEffects() {
   }

   private static void m_7883(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, int var4, float var5, long var6) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      m_7958(var2);
      float var8 = (float)var6 * 0.001F + f_26626;
      float var9 = var2.getBbHeight();
      Vec3 var10 = var1.add(0.0, var9 * 0.58, 0.0);
      Matrix4f var11 = var0.getPose().last().pose();
      float var12 = var2.getBbWidth() * 0.95F + 0.05F;
      float var13 = var12 * 2.6667F;
      float[] var14 = new float[]{58.0F, 58.0F, 16.0F};
      float[] var15 = new float[]{5.5F, -4.2F, 7.4F};
      float[] var16 = new float[]{0.0F, 130.0F, 250.0F};
      BufferBuilder var17 = m_7952();
      BufferBuilder var18 = m_7952();
      m_7893(var18, var11, var10.x, var10.y, var10.z, var12 * 0.9F, m_7887(m_7817(var3, 0.35F), var5 * 0.16F));
      m_7893(var18, var11, var10.x, var10.y, var10.z, var12 * 0.4F, m_7887(m_7817(var3, 0.6F), var5 * 0.34F));

      for (int var19 = 0; var19 < var14.length; var19++) {
         float var20 = var8 * var15[var19] * 6.0F + var16[var19];
         double var21 = Math.toRadians(var20);
         double var23 = Math.toRadians(var14[var19]);
         Vec3 var25 = new Vec3(Math.sin(var23) * Math.cos(var21), Math.cos(var23), Math.sin(var23) * Math.sin(var21));
         Vec3[] var26 = m_7860(var25);
         int var27 = var19 == var14.length - 1 ? m_7817(var4, 0.1F) : m_7817(var3, 0.08F);
         m_7955(var17, var11, var10, var26[0], var26[1], var13, m_7887(var27, var5 * 0.82F));
         double var28 = Math.toRadians(var20 * (var15[var19] < 0.0F ? -7.0F : 7.0F) + var16[var19]);
         Vec3 var30 = var10.add(var26[0].scale(Math.cos(var28) * var12)).add(var26[1].scale(Math.sin(var28) * var12));
         m_7893(var18, var11, var30.x, var30.y, var30.z, 0.095F, m_7887(m_7817(var3, 0.88F), var5));
         m_7893(var18, var11, var30.x, var30.y, var30.z, 0.2F, m_7887(m_7817(var3, 0.5F), var5 * 0.32F));
      }

      m_7825(var17, f_26530);
      m_7942(var18, f_26515, true);
   }

   private static void m_7884(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, int var4, float var5, float var6) {
      float var7 = var5 * 0.65F;
      if (!(var7 <= 0.01F)) {
         Vec3 var8 = mc.gameRenderer.mainCamera().position();
         float var9 = var2.getBbHeight();
         long var10 = Math.max(1L, Math.round(1550.0 / f_26570));
         double var12 = (double)(System.currentTimeMillis() % var10) / var10 * f_27575 * 2.0;
         float var14 = 0.0F;
         if (f_26596 && var2.hurtTime > 0) {
            var14 = Mth.clamp((var2.hurtTime - var6) / 10.0F, 0.0F, 1.0F);
         }

         float var15 = var2.getBbWidth() * 0.25F;
         float var16 = var2.getBbWidth() + var15 - var15 * var7;
         float var17 = (float)(var1.x - var8.x);
         float var18 = (float)(var1.y - var8.y);
         float var19 = (float)(var1.z - var8.z);
         float var20 = var18 + var9 * (float)(0.5 + 0.5 * Math.cos(var12));
         float var21 = (float)(Math.sin(var12) * var9 * 0.2875);
         Matrix4f var22 = var0.getPose().last().pose();
         BufferBuilder var23 = m_7952();
         byte var24 = 2;
         float var25 = 0.005F;
         float var26 = 0.008F;

         for (int var27 = 0; var27 < var24; var27++) {
            float var28 = var20 + var25 / var24 * var27;
            float var29 = var7 * (1.0F - (float)var27 / var24 * 0.3F);
            int var30 = var27 * 10;

            for (int var31 = 0; var31 < 360; var31 += 5) {
               int var32 = var31 / 5;
               int var33 = m_7846(var3, var4, var31 * 5 + var30, var14, 0.75F);
               Vector4f var34 = CombatColors.m_2298(m_7887(var33, var29));
               float var35 = var17 + (float)(f_26573[var32] * var16);
               float var36 = var19 + (float)(f_26574[var32] * var16);
               float var37 = var17 + (float)(f_26573[var32 + 1] * var16);
               float var38 = var19 + (float)(f_26574[var32 + 1] * var16);
               float var39 = var37 - var35;
               float var40 = var38 - var36;
               float var41 = Mth.sqrt(var39 * var39 + var40 * var40);
               float var42 = var41 == 0.0F ? var26 : -var40 / var41 * var26;
               float var43 = var41 == 0.0F ? 0.0F : var39 / var41 * var26;
               m_7931(var23, var22, var35 + var42, var28, var36 + var43, 0.0F, 0.0F, var34);
               m_7931(var23, var22, var37 + var42, var28, var38 + var43, 0.0F, 1.0F, var34);
               m_7931(var23, var22, var37 - var42, var28, var38 - var43, 1.0F, 1.0F, var34);
               m_7931(var23, var22, var35 - var42, var28, var36 - var43, 1.0F, 0.0F, var34);
            }
         }

         for (int var44 = 0; var44 < 360; var44 += 5) {
            int var45 = var44 / 5;
            int var46 = m_7846(var3, var4, var44 * 20, var14, 0.65F);
            int var47 = m_7846(var3, var4, (var44 + 5) * 20, var14, 0.65F);
            Vector4f var48 = CombatColors.m_2298(m_7887(var46, var7));
            Vector4f var49 = CombatColors.m_2298(m_7887(var46, 0.0F));
            Vector4f var50 = CombatColors.m_2298(m_7887(var47, var7));
            Vector4f var51 = CombatColors.m_2298(m_7887(var47, 0.0F));
            float var52 = var17 + (float)(f_26573[var45] * var16);
            float var53 = var19 + (float)(f_26574[var45] * var16);
            float var54 = var17 + (float)(f_26573[var45 + 1] * var16);
            float var55 = var19 + (float)(f_26574[var45 + 1] * var16);
            float var56 = var20 + var21;
            m_7931(var23, var22, var52, var20, var53, 0.0F, 0.0F, var48);
            m_7931(var23, var22, var52, var56, var53, 0.0F, 1.0F, var49);
            m_7931(var23, var22, var54, var56, var55, 1.0F, 1.0F, var51);
            m_7931(var23, var22, var54, var20, var55, 1.0F, 0.0F, var50);
         }

         m_7825(var23, f_26532);
      }
   }

   private static void m_7885(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, int var4, float var5) {
      if (var5 <= 0.01F) {
         m_7892();
      } else {
         var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
         long var6 = System.currentTimeMillis();
         PoseStack var8 = var0.getPose();
         BufferBuilder var9 = m_7952();

         for (int var10 = 0; var10 < f_26561.length; var10++) {
            double var11 = var6 * 0.004;
            double var13 = var10 * (f_27675 / f_26561.length);
            double var15 = var2.getBbWidth() + 0.15;
            double var17 = Math.sin(var11 + var13) * var15;
            double var19 = Math.cos(var11 + var13) * var15;
            double var21 = var10 % 2 == 0 ? 1.0 : -1.0;
            double var23 = Math.sin((var11 + var13) * 0.8) * var21;
            double var25 = var23 * (var2.getBbHeight() * 0.55) + var2.getBbHeight() * 0.5;
            LinkedList var27 = f_26561[var10];
            Vec3 var28 = new Vec3(var17, var25, var19);
            if (!var27.isEmpty()) {
               Vec3 var29 = (Vec3)var27.getFirst();
               double var30 = var29.distanceTo(var28);
               int var32 = Mth.clamp((int)Math.ceil(var30 / 0.04), 1, 6);

               for (int var33 = 1; var33 <= var32; var33++) {
                  double var34 = (double)var33 / var32;
                  var27.addFirst(var29.lerp(var28, var34));
               }
            } else {
               var27.addFirst(var28);
            }

            while (var27.size() > 34) {
               var27.removeLast();
            }

            int var42 = 0;
            int var43 = var27.size();

            for (Object var44_obj : var27) {
               Vec3 var44 = (Vec3)var44_obj;
               float var45 = 1.0F - (float)var42 / var43;
               float var46 = (float)(var1.x + var44.x);
               float var35 = (float)(var1.y + var44.y);
               float var36 = (float)(var1.z + var44.z);
               int var37 = CombatColors.m_2302(var3, var4, var45);
               float var38 = var42 == 0 ? 0.34F : 0.21F;
               float var39 = var38 * var45 * var5;
               float var40 = (float)(Math.pow(var45, 1.8) * var5);
               m_7954(var9, var8, var46, var35, var36, var39, m_7887(var37, var40));
               if (var42 == 0) {
                  m_7954(var9, var8, var46, var35, var36, 0.2F * var5, m_7887(-1, var5));
               }

               var42++;
            }
         }

         m_7942(var9, f_26516, false);
      }
   }

   private static float m_7886(float var0, float var1, float var2) {
      if (var0 < var1) {
         return Math.min(var0 + var2, var1);
      } else {
         return var0 > var1 ? Math.max(var0 - var2, var1) : var0;
      }
   }

   private static int m_7887(int var0, float var1) {
      return CombatColors.m_2303(
         CombatColors.m_2309(var0),
         CombatColors.m_2299(var0),
         CombatColors.m_2308(var0),
         CombatColors.m_2297(var0) * Mth.clamp(var1, 0.0F, 1.0F)
      );
   }

   private static void m_7888(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, long var5) {
      Vec3 var7 = mc.gameRenderer.mainCamera().position();
      Vec3 var8 = var2.position().add(0.0, var2.getBbHeight() / 2.0, 0.0);
      float var9 = var2.getBbWidth() * 1.5F;
      float var10 = var2.getBbHeight();
      float var11 = 1.25F - 0.5F * var4;
      float var12 = (float)(var5 % 360000L) / 2.5F + var4;
      PoseStack var13 = var0.getPose();
      BufferBuilder var14 = m_7842();

      for (int var15 = 0; var15 < 360; var15 += 20) {
         double var16 = Math.toRadians(var15 + var12 * 0.3F);
         float var18 = (float)Math.sin(var16) * var9 * var11;
         float var19 = (float)Math.cos(var16) * var9 * var11;
         float var20 = 0.1F + var10 * Math.abs((float)Math.sin(var15));
         Vec3 var21 = var1.add(var18, var20, var19);
         var13.pushPose();
         var13.translate(var21.x - var7.x, var21.y - var7.y, var21.z - var7.z);
         Vector3f var22 = new Vector3f((float)(var8.x - var21.x), (float)(var8.y - var21.y), (float)(var8.z - var21.z)).normalize();
         var13.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), var22));
         m_7946(var14, var13, 0.1F, m_7887(var3, var4));
         var13.popPose();
      }

      m_7825(var14, m_7829());
      BufferBuilder var23 = m_7952();

      for (int var24 = 0; var24 < 360; var24 += 20) {
         double var17 = Math.toRadians(var24 + var12 * 0.3F);
         float var25 = (float)Math.sin(var17) * var9 * var11;
         float var26 = (float)Math.cos(var17) * var9 * var11;
         float var27 = 0.1F + var10 * Math.abs((float)Math.sin(var24));
         Vec3 var28 = var1.add(var25, var27, var26);
         m_7954(var23, var13, var28.x - var7.x, var28.y - var7.y, var28.z - var7.z, 1.5F * var4, m_7887(var3, var4 * 0.2F));
         m_7954(var23, var13, var28.x - var7.x, var28.y - var7.y, var28.z - var7.z, 0.6F * var4, m_7887(var3, var4 * 0.2F));
      }

      m_7830(var23, f_26517, false, m_7869());
   }

   public static void m_7889(boolean var0, float var1, int var2) {
      f_26657 = var0;
      f_26655 = var1;
      f_26656 = var2;
   }

   private static float m_7891(int var0) {
      float var1 = (float)Math.sin(var0 * 12.9898F) * 43758.547F;
      return var1 - (float)Math.floor(var1);
   }

   private static void m_7892() {
      for (LinkedList var3 : f_26561) {
         var3.clear();
      }
   }

   private static void m_7893(BufferBuilder var0, Matrix4f var1, double var2, double var4, double var6, float var8, int var9) {
      PoseStack var10 = new PoseStack();
      var10.mulPose(var1);
      var10.translate(var2, var4, var6);
      var10.mulPose(mc.gameRenderer.mainCamera().rotation());
      m_7855(var0, var10.last().pose(), var8, var9);
   }

   private static Vec3 m_7894(Vec3 var0, double var1, double var3, double var5) {
      double var7 = Math.sin(var1);
      double var9 = Math.cos(var1);
      double var11 = var0.y * var9 - var0.z * var7;
      double var13 = var0.y * var7 + var0.z * var9;
      var0 = new Vec3(var0.x, var11, var13);
      double var15 = Math.sin(var3);
      double var17 = Math.cos(var3);
      double var19 = var0.x * var17 - var0.z * var15;
      var13 = var0.x * var15 + var0.z * var17;
      var0 = new Vec3(var19, var0.y, var13);
      double var21 = Math.sin(var5);
      double var23 = Math.cos(var5);
      var19 = var0.x * var23 - var0.y * var21;
      var11 = var0.x * var21 + var0.y * var23;
      return new Vec3(var19, var11, var0.z);
   }

   private static void m_7895(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, float var5, long var6) {
      float var8 = (float)Math.sin(var2.hurtTime * f_27686 / 10.0);
      f_26600 = f_26600 + (var8 - f_26600) * (float)Math.sin(Math.min(var5 / 0.1F, 1.0F) * f_27689 / 2.0);
      int var9 = (int)((var6 / 10L + 1L) % 360L);
      var9 = var9 >= 180 ? 360 - var9 : var9;
      int var10 = CombatColors.m_2303(
         Math.min(255, Math.round(CombatColors.m_2309(var3) / 0.75F)),
         Math.min(255, Math.round(CombatColors.m_2299(var3) / 0.75F)),
         Math.min(255, Math.round(CombatColors.m_2308(var3) / 0.75F)),
         255.0
      );
      int var11 = CombatColors.m_2302(var3, var10, var9 / 180.0F);
      if (f_26596) {
         var11 = CombatColors.m_2302(var11, -39836, f_26600);
      }

      var11 &= 16777215;
      float var12 = (float)Math.cos(var6 / 2000.0) * 360.0F + f_26600 * 20.0F;
      float var13 = (1.0F - 0.3F * f_26600) * var4;
      Camera var14 = mc.gameRenderer.mainCamera();
      Vec3 var15 = var14.position();
      PoseStack var16 = var0.getPose();

      for (int var17 = 0; var17 < 3; var17++) {
         BufferBuilder var18 = CombatTextureQuads.m_9213(var17 == 2, var17 == 0);

         for (int var19 = 0; var19 < 360; var19 += 12) {
            float var20 = (float)Math.toRadians(var19 + var12);
            float var21 = (float)(Math.sin(var20) * var2.getBbWidth() * 1.5 * 0.7F);
            float var22 = (float)(Math.cos(var20) * var2.getBbWidth() * 1.5 * 0.7F);
            double var23 = var2.getBbHeight() * Math.abs(Math.sin(((int)(var19 * 10430.378350470453) & 65535) * f_27710 * 2.0 / 65536.0));
            var16.pushPose();
            var16.translate(var1.x + var21 - var15.x, var1.y + var23 - var15.y, var1.z + var22 - var15.z);
            if (var17 == 0) {
               var16.mulPose(var14.rotation());
               CombatTextureQuads.m_9215(var18, var16.last().pose(), 0.3F * var13, var11 | (int)(var4 * 0.35F * 255.0F) << 24);
            } else {
               var16.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(-var21, (float)(-var23), -var22).normalize()));
               if (var17 == 1) {
                  CombatTextureQuads.m_9217(var18, var16.last().pose(), 0.06F * var13, var11 | (int)(var4 * 0.2F * 255.0F) << 24);
               } else {
                  CombatTextureQuads.m_9211(var18, var16.last().pose(), 0.06F * var13, var11 | (int)(var4 * 255.0F) << 24);
               }
            }

            var16.popPose();
         }

         CombatTextureQuads.m_9218(var18, var17 == 2, var17 == 0 ? CombatTextureQuads.f_33138 : null, !f_26602);
      }
   }

   private static void m_7896(double var0, double var2, double var4, double var6, double var8, double var10, double var12, float var14, float var15) {
      Vec3 var16 = m_7837(var6, var8, var10, var12);
      f_26609.addLast(
         new CombatTargetEffects.GhostPoint(new Vec3(var0 + var16.x, var2 + var16.y, var4 + var16.z), System.currentTimeMillis(), var14, var15)
      );

      while (f_26609.size() > 130) {
         f_26609.pollFirst();
      }
   }

   private static void m_7897(CombatWorldRenderEvent var0, Vec3 var1, int var2, float var3) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      BufferBuilder var4 = m_7952();
      PoseStack var5 = var0.getPose();
      float var6 = f_26545 * 5.0F;
      float var7 = m_7832(f_26544);

      for (int var8 = 0; var8 < 9; var8 += 3) {
         for (int var9 = 0; var9 < 12; var9++) {
            float var10 = var6 + var9 * 0.1F;
            int var11 = var8 * var8;
            double var12 = var1.x + 0.8F * Mth.sin(var10 + var11);
            double var14 = var1.y + 0.5 + 0.3F * Mth.sin(var6 + var9 * 0.2F) + 0.2F * var8;
            double var16 = var1.z + 0.8F * Mth.cos(var10 - var11);
            float var18 = 50.0F * var7 * (0.005F + var9 / 1800.0F);
            var5.pushPose();
            var5.translate(var12, var14, var16);
            var5.mulPose(mc.gameRenderer.mainCamera().rotation());
            m_7855(var4, var5.last().pose(), var18, m_7887(var2, var3));
            var5.popPose();
         }
      }

      m_7942(var4, f_26513, true);
   }

   private static int m_7898(int var0, float var1) {
      return CombatColors.m_2303(
         Math.min(255, Math.round(CombatColors.m_2309(var0) * var1)),
         Math.min(255, Math.round(CombatColors.m_2299(var0) * var1)),
         Math.min(255, Math.round(CombatColors.m_2308(var0) * var1)),
         CombatColors.m_2297(var0)
      );
   }

   private static void m_7899(BufferBuilder var0, Matrix4f var1, Vec3 var2, Vec3 var3, Vec3 var4, float var5, int var6) {
      m_7955(var0, var1, var2, var3, var4, var5 * 2.0F / 0.82F, var6);
   }

   private static float m_7900(int var0, float var1, float var2) {
      return switch (var0 & 3) {
         case 0 -> var1 + var2;
         case 1 -> -var1 + var2;
         case 2 -> var1 - var2;
         default -> -var1 - var2;
      };
   }

   public static void beginFrame() {
      TargetSurfaceLighting.m_9270();
      f_26644 = true;
      f_26645 = null;
      f_26646 = null;
      f_26647 = null;
      f_26650 = RenderSystem.getProjectionMatrixBuffer();
      f_26651 = RenderSystem.getProjectionType();
   }

   private static Vec3 m_7903(Vec3 var0, double var1, double var3, float var5, float var6, int var7, float var8) {
      double var9 = var5 + var7 * var6 * f_27231 * 2.0 * var8 + (f_26557.nextDouble() - 0.5) * 0.35;
      double var11 = var3 * (0.92 + f_26557.nextDouble() * 0.16);
      double var13 = var1 * (0.06 + var8 * 0.88) + (f_26557.nextDouble() - 0.5) * var1 * 0.05;
      return var0.add(Math.cos(var9) * var11, var13, Math.sin(var9) * var11);
   }

   private static void m_7904(
      BufferBuilder var0,
      Matrix4f var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      int var11,
      int var12,
      int var13
   ) {
      Vector4f var14 = CombatColors.m_2298(var11);
      Vector4f var15 = CombatColors.m_2298(var12);
      Vector4f var16 = CombatColors.m_2298(var13);
      m_7931(var0, var1, var2, var3, var4, 0.5F, 0.5F, var14);
      m_7931(var0, var1, var8, var9, var10, 0.5F, 0.5F, var16);
      m_7931(var0, var1, var5, var6, var7, 0.5F, 0.5F, var15);
      m_7931(var0, var1, var5, var6, var7, 0.5F, 0.5F, var15);
   }

   public static void m_7905(boolean var0, float var1) {
      f_26628 = var0;
      f_26629 = var1;
   }

   private static float m_7906(float var0) {
      float var1 = Mth.clamp(var0, 0.0F, 1.0F) - 1.0F;
      return 1.0F + 2.70158F * var1 * var1 * var1 + 1.70158F * var1 * var1;
   }

   private static void m_7907(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, long var5, boolean var7) {
      double var8 = System.nanoTime() * 1.0E-9;
      float var10 = (float)(var8 * 0.75);
      if (var7 && var5 - f_26624 >= 60L && f_26623.size() < 18) {
         for (int var11 = 0; var11 < 2 && f_26623.size() < 18; var11++) {
            f_26623.add(m_7856(var2, var1, var5, var10));
         }

         f_26624 = var5;
      }

      f_26623.removeIf(var2x -> var5 - var2x.f_26493 > var2x.f_26495);
      if (!f_26623.isEmpty() && !(var4 <= 0.01F)) {
         Vec3 var35 = mc.gameRenderer.mainCamera().position();
         Matrix4f var12 = var0.getPose().last().pose();
         PoseStack var13 = var0.getPose();
         int var14 = m_7817(var3, 0.4F);
         int var15 = m_7817(var3, 0.9F);
         BufferBuilder var16 = m_7952();
         BufferBuilder var17 = m_7952();

         for (CombatTargetEffects.LightningArc var19 : f_26623) {
            float var20 = (float)(var5 - var19.f_26493) / (float)var19.f_26495;
            if (!(var20 < 0.0F) && !(var20 > 1.0F)) {
               float var21 = (float)Math.sin(f_27195 * Mth.clamp(var20, 0.0F, 1.0F));
               var21 = m_7832(var21);
               float var22 = var4 * var21;
               if (!(var22 <= 0.02F)) {
                  double var23 = (System.nanoTime() - var19.f_26494) * 1.0E-9;
                  float var25 = (float)(var23 * var19.f_26499 * var19.f_26498);
                  float var26 = (float)Math.cos(var25);
                  float var27 = (float)Math.sin(var25);
                  ArrayList var28 = new ArrayList(var19.f_26492.size());

                  for (Vec3 var30 : var19.f_26492) {
                     double var31 = var30.x * var26 - var30.z * var27;
                     double var33 = var30.x * var27 + var30.z * var26;
                     var28.add(var1.add(var31, var30.y, var33).subtract(var35));
                  }

                  if (var28.size() >= 2) {
                     for (int var37 = 0; var37 < var28.size(); var37++) {
                        Vec3 var38 = (Vec3)var28.get(var37);
                        float var39 = 1.0F - (float)var37 / Math.max(1, var28.size() - 1) * 0.3F;
                        m_7954(var16, var13, var38.x, var38.y, var38.z, 0.34F * var39 * var19.f_26497, m_7887(var3, var22 * 0.18F));
                        m_7954(var16, var13, var38.x, var38.y, var38.z, 0.18F * var39 * var19.f_26497, m_7887(var14, var22 * 0.3F));
                        m_7954(var16, var13, var38.x, var38.y, var38.z, 0.09F * var39 * var19.f_26497, m_7887(var15, var22 * 0.42F));
                     }

                     m_7866(var17, var12, var28, 0.05F * var19.f_26497, m_7887(var3, var22 * 0.6F));
                     m_7866(var17, var12, var28, 0.028F * var19.f_26497, m_7887(var14, var22 * 0.85F));
                     m_7866(var17, var12, var28, 0.014F * var19.f_26497, m_7887(var15, var22));
                     m_7866(var17, var12, var28, 0.006F * var19.f_26497, m_7887(-1, var22));
                  }
               }
            }
         }

         m_7830(var16, f_26515, false, f_26535);
         m_7830(var17, f_26518, false, f_26535);
      }
   }

   private static double[] m_7908(boolean var0) {
      double[] var1 = new double[73];

      for (int var2 = 0; var2 <= 72; var2++) {
         double var3 = Math.toRadians(var2 * 5.0);
         var1[var2] = var0 ? Math.cos(var3) : Math.sin(var3);
      }

      return var1;
   }

   private static int m_7909(int var0, int var1, int var2, boolean var3) {
      int var4 = (int)((System.currentTimeMillis() / 4L + var2) % 360L);
      if (var4 >= 180) {
         var4 = 360 - var4;
      }

      int var5 = CombatColors.m_2302(var0, var1, var4 / 180.0F);
      return var3 ? m_7817(var5, 0.85F) : var5;
   }

   private static void m_7910() {
      f_26590 = 1.0F;
      f_26591 = 0.0F;
      f_26592 = 0.0;
      f_26593 = false;
      f_26594 = f_26977;
   }

   private static void m_7911(BufferBuilder var0, BufferBuilder var1, RenderPipeline var2) {
      m_7913(var0, var1, var2, -1);
   }

   private static void m_7912(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      float var5 = var2.getBbWidth() * 1.4F;
      float var6 = (float)(System.currentTimeMillis() % 100000L) / 1.5F;
      int var7 = Mth.clamp(f_26595, 2, 4);
      float var8 = 360.0F / var7;
      float var9 = 80.0F;
      float var10 = var8 - var9;
      byte var11 = 0;
      int var12 = 0;
      int var13 = 0;
      Matrix4f var14 = var0.getPose().last().pose();
      BufferBuilder var15 = m_7952();

      for (int var16 = 0; var16 < 360; var16 += 2) {
         if (var12 > 0) {
            var12 -= 2;
            if (var12 <= 0) {
               var13++;
            }
         } else {
            float var17 = 0.08F + 0.002F * var11;
            float var18 = 0.3F + 0.007F * var11;
            var11 += 2;
            if (var11 > var9) {
               var12 = (int)var10;
               var11 = 0;
            } else {
               float var19 = Math.max(0.5F, 1.2F - 0.5F * var4);
               float var20 = (float)Math.toRadians(var16 + var6);
               float var21 = (float)(var1.x + Math.sin(var20) * var5 * var19);
               float var22 = (float)(var1.z + Math.cos(var20) * var5 * var19);
               float var23 = (var13 % 2 == 0 ? 0.15F : -0.15F) * var2.getBbHeight();
               float var24 = (float)Math.sin(Math.toRadians(var16 / 2.0F + var6 / 5.0F));
               float var25 = (float)(var1.y + var2.getBbHeight() * 0.62F + var2.getBbHeight() / 6.0F * var24 + var23);
               m_7893(var15, var14, var21, var25, var22, var18, m_7887(var3, var4 * 0.05F));
               m_7893(var15, var14, var21, var25, var22, var17, m_7887(var3, var4));
            }
         }
      }

      m_7942(var15, f_26518, true);
   }

   private static void m_7913(BufferBuilder var0, BufferBuilder var1, RenderPipeline var2, int var3) {
      RenderTarget var4 = mc.gameRenderer.mainRenderTarget();
      if (var4 != null && var4.getColorTextureView() != null && var4.getDepthTextureView() != null) {
         TextureTarget var5 = m_7824(var4.width, var4.height);
         var5.copyDepthFrom(var4);
         m_7929(var1, var2, var5, true);
         CombatGlowRenderer.m_8389(var5, var4, var3, 4.0F, 1.25F, 1.0F);
         m_7825(var0, var2);
         CommandEncoder var6 = RenderSystem.getDevice().createCommandEncoder();
         RenderPass var7 = var6.createRenderPass(() -> "targetEspLightningCore", var4.getColorTextureView(), Optional.empty());

         try {
            var7.setPipeline(f_26538);
            var7.bindTexture("InSampler", var5.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            var7.draw(3, 1, 0, 0);
         } catch (Throwable var11) {
            if (var7 != null) {
               try {
                  var7.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (var7 != null) {
            var7.close();
         }
      }
   }

   public static void m_7914(CombatWorldRenderEvent var0, LivingEntity var1, String var2, int var3, int var4) {
      if (f_26644 && f_26605 && (var2.equals("Призраки") || var2.equals("Кристалики") || var2.equals("Молнии")) && (var1 != null && var1.isAlive() || hasFadingTarget())) {
         TargetSurfaceLighting.m_9277();
      }

      if (f_26644 && m_7841(var2)) {
         f_26645 = var0;
         f_26646 = var1;
         f_26647 = var2;
         f_26648 = var3;
         f_26649 = var4;
      } else {
         long var5 = System.currentTimeMillis();
         float var7 = f_26546 == 0L ? 0.0F : Math.min(0.1F, (float)(var5 - f_26546) / 1000.0F);
         f_26546 = var5;
         boolean var8 = var1 != null && var1.isAlive();
         if (var8) {
            f_26568 = var3;
            f_26569 = var4;
         }

         boolean var9 = var2.equals("Кольцо") && f_26571.equals("Тип 3");
         float var10 = var9 ? 1.0F : (!var8 && var2.equals("Куб") ? 1.05F : 0.3F);
         f_26544 = m_7886(f_26544, var8 ? 1.0F : 0.0F, var7 / var10);
         if (var8) {
            f_26543 = var1;
         }

         if (!f_26547.equals(var2)) {
            if ("Молнии".equals(f_26547) && "Тип 3".equals(f_26608)) {
               TargetCubeRenderer.m_1844();
            }

            f_26545 = 0.0F;
            f_26555.clear();
            f_26556 = f_26689;
            f_26548.clear();
            f_26552 = f_26690;
            f_26558.clear();
            f_26559 = 0L;
            f_26617.clear();
            f_26618 = 0L;
            f_26623.clear();
            f_26624 = 0L;
            m_7892();
            f_26562.clear();
            f_26563 = 0.0;
            f_26625 = f_26691;
            f_26632 = false;
            f_26635 = 0.0F;
            f_26636 = false;
            f_26637 = false;
            f_26638 = 0.0;
            f_26639 = 0.0;
            f_26640 = 0.0;
            f_26641 = 0.0;
            f_26642 = 0.0;
            f_26643 = 0.0;
            f_26606 = "";
            f_26607 = "";
            f_26608 = "";
            f_26609.clear();
            f_26610 = 0L;
            f_26611 = f_26692;
            m_7910();
            f_26547 = var2;
         }

         boolean var11 = var2.equals("Кристалики") && f_26601.equals("Тип 4");
         float var12 = f_26598 == 0L ? 0.0F : (float)(var5 - f_26598) / 1000.0F;
         if (var11) {
            f_26599 = f_26599 + ((var8 ? 1.0F : 0.0F) - f_26599) * (float)Math.sin(Math.min(var12 / 0.15F, 1.0F) * f_26697 / 2.0);
            f_26544 = f_26599;
            f_26598 = var5;
         } else {
            f_26598 = 0L;
            f_26599 = 0.0F;
            f_26600 = 0.0F;
         }

         if (f_26543 != null && !(f_26544 <= (var11 ? 0.001F : 0.01F))) {
            f_26545 += var7;
            float var13 = var0.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            Vec3 var14 = new Vec3(
               Mth.lerp(var13, f_26543.xo, f_26543.getX()), Mth.lerp(var13, f_26543.yo, f_26543.getY()), Mth.lerp(var13, f_26543.zo, f_26543.getZ())
            );
            float var15 = f_26543.hurtDuration <= 0 ? 0.0F : Mth.clamp((float)f_26543.hurtTime / f_26543.hurtDuration, 0.0F, 1.0F);
            int var16 = f_26596 ? CombatColors.m_2302(var3, -65536, var15) : var3;
            int var17 = f_26596 ? CombatColors.m_2302(var4, -65536, var15) : var4;
            float var18 = var11 ? f_26544 : m_7832(f_26544);
            TargetSurfaceLighting.m_9272(f_26605 && (var2.equals("Призраки") || var2.equals("Кристалики") || var2.equals("Молнии")), var0.getPose().last().pose());

            try {
               switch (var2) {
                  case "Кольцо":
                     if ("Тип 3".equals(f_26571)) {
                        float var29 = var8 ? m_7937(f_26544) : 1.0F - (1.0F - f_26544) * (1.0F - f_26544);
                        m_7814(var0, var14, f_26543, m_7826(f_26543, var3, var7, var5), var29);
                     } else if ("Тип 2".equals(f_26571)) {
                        m_7884(var0, var14, f_26543, var3, var4, var18, var13);
                     } else {
                        m_7843(var0, var14, f_26543, var16, var18);
                     }
                     break;
                  case "Обруч":
                     m_7917(var0, var14, f_26543, var16, var18);
                     break;
                  case "Призраки":
                     if (!f_26606.equals(f_26597)) {
                        m_7892();
                        f_26562.clear();
                        f_26563 = 0.0;
                        f_26609.clear();
                        f_26610 = 0L;
                        f_26611 = f_26718;
                        m_7910();
                        f_26606 = f_26597;
                     }

                     switch (f_26597) {
                        case "Тип 2":
                           m_7836(var0, var14, f_26543, var16, var17, var18);
                           return;
                        case "Тип 3":
                           m_7885(var0, var14, f_26543, var16, var17, var18);
                           return;
                        case "Тип 4":
                           m_7864(var0, var14, f_26543, var16, var17, var8);
                           return;
                        case "Тип 5":
                           m_7853(var0, var14, f_26543, var16, var17, f_26544, var13);
                           return;
                        case "Тип 6":
                           m_7912(var0, var14, f_26543, var16, var18);
                           return;
                        case "Тип 7":
                           m_7816(var0, var14, f_26543, var16, var18);
                           return;
                        case "Тип 8":
                           m_7847(var0, var14, f_26543, var16, var18, var7, var8);
                           return;
                        default:
                           m_7897(var0, var14, var16, var18);
                           return;
                     }
                  case "Кристалики":
                     if (!f_26607.equals(f_26601)) {
                        f_26555.clear();
                        f_26556 = f_26726;
                        f_26548.clear();
                        f_26552 = f_26727;
                        f_26607 = f_26601;
                     }

                     switch (f_26601) {
                        case "Тип 2":
                           m_7935(var0, var14, f_26543, var16, var18, var5);
                           return;
                        case "Тип 3":
                           m_7888(var0, var14, f_26543, var16, var18, var5);
                           return;
                        case "Тип 4":
                           m_7895(var0, var14, f_26543, var3, var18, var12, var5);
                           return;
                        default:
                           m_7811(var0, var14, f_26543, var16, var4, var18, var5);
                           return;
                     }
                  case "Ромбики":
                     m_7849(var0, var14, f_26543, var16, var4, var18, var5);
                     break;
                  case "Молнии":
                     try {
                        if (!f_26608.equals(f_26603)) {
                           if ("Тип 3".equals(f_26608)) {
                              TargetCubeRenderer.m_1844();
                           }

                           f_26558.clear();
                           f_26617.clear();
                           f_26623.clear();
                           f_26559 = 0L;
                           f_26618 = 0L;
                           f_26624 = 0L;
                           f_26608 = f_26603;
                        }

                        if ("Тип 1".equals(f_26603)) {
                           m_7916(var0, var14, f_26543, var16, var18, var5, var8, var15);
                        } else if ("Тип 3".equals(f_26603)) {
                           TargetCubeRenderer.m_1853(mc.gameRenderer.mainCamera(), var13, f_26543, var18, var3, f_26596, f_26604 ? "Never" : "Auto");
                        } else {
                           m_7907(var0, var14, f_26543, var16, var18, var5, var8);
                        }
                     } catch (Throwable var26) {
                        var26.printStackTrace();
                        f_26558.clear();
                        f_26617.clear();
                        f_26623.clear();
                        f_26559 = 0L;
                        f_26618 = 0L;
                        f_26624 = 0L;
                     }
                     break;
                  case "Куб":
                     m_7959(var0, var14, f_26543, var16, f_26544, var7, var8, var15);
                     break;
                  case "Квадрат":
                     m_7915(var0, var14, f_26543, var16, var18);
                     break;
                  case "Треугольник":
                     m_7943(var0, var14, f_26543, var16, var18, var5);
                     break;
                  case "Пентограмма":
                     m_7808(var0, var14, f_26543, var16, var18, var15);
               }
            } finally {
               TargetSurfaceLighting.m_9274(var0.getView());
            }
         } else {
            if (!var8) {
               f_26543 = null;
            }

            m_7892();
            f_26562.clear();
            f_26609.clear();
         }
      }
   }

   private static void m_7915(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      boolean var5 = "Тип 2".equals(f_26572);
      float var6 = var2.hurtDuration <= 0 ? 0.0F : Mth.clamp((float)var2.hurtTime / var2.hurtDuration, 0.0F, 1.0F);
      float var7 = Mth.lerpInt(f_26544, 2, 1) * (var5 ? 1.5F : 1.2F);
      float var8 = (float)Math.sin(f_26545 * 1.667) * 180.0F;
      int var9 = var3;
      if (var5) {
         int var10 = Mth.clamp((int)(CombatColors.m_2309(var3) * (1.0F - var6) + 255.0F * var6), 0, 255);
         int var11 = Mth.clamp((int)(CombatColors.m_2299(var3) * (1.0F - var6)), 0, 255);
         int var12 = Mth.clamp((int)(CombatColors.m_2308(var3) * (1.0F - var6)), 0, 255);
         var9 = CombatColors.m_2303(var10, var11, var12, CombatColors.m_2297(var3));
      }

      BufferBuilder var14 = m_7952();
      PoseStack var15 = var0.getPose();
      var15.pushPose();
      var15.translate(var1.x, var1.y + var2.getBbHeight() * 0.5, var1.z);
      var15.mulPose(mc.gameRenderer.mainCamera().rotation());
      var15.mulPose(Axis.ZP.rotationDegrees(var8));
      m_7855(var14, var15.last().pose(), var7, m_7887(var9, var4));
      var15.popPose();
      m_7942(var14, var5 ? f_26512 : f_26511, true);
   }

   private static void m_7916(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, long var5, boolean var7, float var8) {
      if (var7 && var5 - f_26559 >= 80L && f_26558.size() < 5) {
         f_26558.add(m_7861(var2, var1, var5));
         if (f_26557.nextFloat() < 0.4F && f_26558.size() < 5) {
            f_26558.add(m_7861(var2, var1, var5 + 30L));
         }

         f_26559 = var5;
      }

      Iterator var9 = f_26558.iterator();

      while (var9.hasNext()) {
         CombatTargetEffects.LightningBurst var10 = (CombatTargetEffects.LightningBurst)var9.next();
         if (var5 - var10.f_26503 > var10.f_26504) {
            var9.remove();
         }
      }

      if (!f_26558.isEmpty()) {
         int var34 = m_7817(var3, 0.35F);
         int var11 = m_7817(var3, 0.85F);
         BufferBuilder var12 = m_7952();
         BufferBuilder var13 = m_7952();
         Matrix4f var14 = var0.getPose().last().pose();
         Vec3 var15 = mc.gameRenderer.mainCamera().position();

         for (CombatTargetEffects.LightningBurst var17 : f_26558) {
            float var18 = (float)(var5 - var17.f_26503) / (float)var17.f_26504;
            if (!(var18 < 0.0F)) {
               float var19 = m_7937(Mth.clamp(var18 / 0.2F, 0.0F, 1.0F));
               float var20 = 1.0F - m_7832(Mth.clamp((var18 - 0.4F) / 0.6F, 0.0F, 1.0F));
               float var21 = 1.0F - m_7832(Mth.clamp(var18 / 0.07F, 0.0F, 1.0F));
               float var22 = 0.8F + 0.2F * (float)Math.sin(var5 * 0.05 + var17.f_26505);
               float var23 = var4 * var20 * var22;
               if (!(var23 <= 0.015F)) {
                  Vec3 var24 = var1.subtract(var17.f_26502);

                  for (CombatTargetEffects.LightningBranch var26 : var17.f_26500) {
                     float var27 = Mth.clamp((var19 - var26.f_26491) / Math.max(0.001F, 1.0F - var26.f_26491), 0.0F, 1.0F);
                     if (!(var27 <= 0.001F)) {
                        ArrayList var28 = new ArrayList(var26.f_26489.size());

                        for (Vec3 var30 : var26.f_26489) {
                           var28.add(var30.add(var24).subtract(var15));
                        }

                        float var35 = var23 * (0.5F + var26.f_26490 * 0.5F);

                        for (int var36 = 1; var36 <= 2; var36++) {
                           float var31 = (var28.size() - 1) * var27 * var36 * 0.5F;
                           int var32 = Math.min((int)var31, var28.size() - 1);
                           Vec3 var33 = ((Vec3)var28.get(var32)).lerp((Vec3)var28.get(Math.min(var32 + 1, var28.size() - 1)), var31 - var32);
                           TargetSurfaceLighting.m_9273((float)var33.x, (float)var33.y, (float)var33.z, 0.5F * var26.f_26490, m_7887(var3, var35));
                        }

                        m_7834(var12, var14, var28, var27, var26.f_26490 * 0.045F, m_7887(var34, var35 * (0.6F + var21 * 0.5F)), var17.f_26505, 0.0F);
                        m_7834(
                           var13,
                           var14,
                           var28,
                           var27,
                           var26.f_26490 * 0.024F,
                           m_7887(var11, Math.min(1.0F, var35 * (1.0F + var21))),
                           var17.f_26505 + 11.0F,
                           0.0F
                        );
                        m_7834(
                           var12,
                           var14,
                           var28,
                           var27,
                           var26.f_26490 * 0.012F,
                           m_7887(-1, Math.min(1.0F, var35 * (0.55F + var21 * 0.45F))),
                           var17.f_26505 + 23.0F,
                           0.0F
                        );
                     }
                  }
               }
            }
         }

         m_7913(var12, var13, f_26537, var3);
      }
   }

   private static void m_7917(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      float var5 = m_7937(var4);
      float var6 = var2.getBbHeight();
      float var7 = Mth.lerp(var5, 0.68F, 1.0F);
      float var8 = Math.max(0.72F, var2.getBbWidth() * 1.15F + 0.16F) * var7;
      double var9 = f_26545 * 2.0 + var2.getId() * 0.17;
      double var11 = Math.toRadians(22.0);
      Vec3 var13 = new Vec3(Math.sin(var11) * Math.cos(var9), Math.cos(var11), Math.sin(var11) * Math.sin(var9));
      Vec3[] var14 = m_7860(var13);
      Vec3 var15 = var1.add(0.0, var6 * 0.46F, 0.0);
      Matrix4f var16 = var0.getPose().last().pose();
      BufferBuilder var17 = m_7952();
      m_7955(var17, var16, var15, var14[0], var14[1], var8 * 2.72F, m_7887(var3, var5 * 0.22F));
      m_7955(var17, var16, var15, var14[0], var14[1], var8 * 2.6667F, m_7887(m_7817(var3, 0.48F), var5 * 0.94F));
      m_7825(var17, f_26531);
      BufferBuilder var18 = m_7952();
      double var19 = f_26545 * 5.0;

      for (int var21 = 0; var21 < 13; var21++) {
         float var22 = 1.0F - var21 / 13.0F;
         double var23 = var19 - var21 * 0.075;
         Vec3 var25 = var15.add(var14[0].scale(Math.cos(var23) * var8)).add(var14[1].scale(Math.sin(var23) * var8));
         float var26 = var5 * var22 * var22 * 0.42F;
         float var27 = 0.07F + var22 * 0.105F;
         m_7893(var18, var16, var25.x, var25.y, var25.z, var27, m_7887(m_7817(var3, 0.72F), var26));
      }

      m_7942(var18, f_26515, true);
   }

   private static int m_7918(int var0) {
      int var1 = var0 * 374761393 + 668265263;
      var1 = (var1 ^ var1 >> 13) * 1274126177;
      return var1 & 0xFF;
   }

   public static void m_7919(boolean var0, boolean var1) {
      f_26630 = var0;
      f_26631 = var1;
   }

   private static int m_7920(int var0) {
      float var1 = CombatColors.m_2297(var0) / 255.0F;
      float var2 = CombatColors.m_2309(var0) * var1;
      float var3 = CombatColors.m_2299(var0) * var1;
      float var4 = CombatColors.m_2308(var0) * var1;
      float var5 = Math.max(var2, Math.max(var3, var4)) * 0.5F;
      if (var5 <= 0.0F) {
         return 0;
      }

      int var6 = Mth.clamp(Math.round(var5), 1, 255);
      float var7 = 255.0F / (var6 * 2.0F);
      return CombatColors.m_2303(
         Mth.clamp(Math.round(var2 * var7), 0, 255), Mth.clamp(Math.round(var3 * var7), 0, 255), Mth.clamp(Math.round(var4 * var7), 0, 255), var6
      );
   }

   public static void m_7921(
      float var0, String var1, int var2, boolean var3, String var4, String var5, boolean var6, String var7, boolean var8, boolean var9, String var10
   ) {
      f_26570 = Mth.clamp(var0, 0.1F, 5.0F);
      f_26571 = var1 != null && !var1.isEmpty() ? var1 : "Тип 1";
      f_26572 = var10 != null && !var10.isEmpty() ? var10 : "Тип 1";
      f_26595 = Mth.clamp(var2, 2, 4);
      f_26596 = var3;
      f_26597 = var4 != null && !var4.isEmpty() ? var4 : "Тип 1";
      f_26601 = var5 != null && !var5.isEmpty() ? var5 : "Тип 1";
      f_26602 = var6;
      f_26603 = var7 != null && !var7.isEmpty() ? var7 : "Тип 2";
      f_26604 = var8;
      f_26605 = var9;
   }

   private static int m_7922(float var0, int var1, int var2) {
      float var3 = Mth.clamp((0.6F - var0) / 1.65F, 0.0F, 1.0F);
      int var4 = CombatColors.m_2312(var2, -1, 0.55F);
      int var5 = CombatColors.m_2312(var4, var2, var3);
      float var6 = (m_7891(var1) - 0.5F) * 0.16F;
      return CombatColors.m_2312(var5, var6 >= 0.0F ? -1 : var2, Math.abs(var6));
   }

   private static void m_7923(BufferBuilder var0, RenderPipeline var1) {
      MeshData var2 = var0.build();
      if (var2 != null) {
         GpuBuffer var3 = null;

         try {
            DrawState var4 = var2.drawState();
            int var5 = var4.indexCount();
            AutoStorageIndexBuffer var6 = RenderSystem.getSequentialBuffer(var4.primitiveTopology());
            GpuBuffer var7 = var6.getBuffer(var5);
            IndexType var8 = var6.type();
            var3 = RenderSystem.getDevice().createBuffer(() -> "targetEspTriangleLines", 32, var2.vertexBuffer());
            RenderTarget var9 = mc.gameRenderer.mainRenderTarget();
            CommandEncoder var10 = RenderSystem.getDevice().createCommandEncoder();
            RenderPass var11 = var10.createRenderPass(
               () -> "targetEspTriangleLines", var9.getColorTextureView(), Optional.empty(), var9.getDepthTextureView(), OptionalDouble.empty()
            );

            try {
               RenderSystem.bindDefaultUniforms(var11);
               var11.setPipeline(var1);
               var11.setVertexBuffer(0, var3.slice());
               var11.setIndexBuffer(var7, var8);
               var11.drawIndexed(var5, 1, 0, 0, 0);
            } catch (Throwable var19) {
               if (var11 != null) {
                  try {
                     var11.close();
                  } catch (Throwable var18) {
                     var19.addSuppressed(var18);
                  }
               }

               throw var19;
            }

            if (var11 != null) {
               var11.close();
            }
         } finally {
            if (var3 != null) {
               var3.close();
            }

            var2.close();
         }
      }
   }

   private static float m_7924(float var0, float var1) {
      int var2 = (int)Math.floor(var0) & 0xFF;
      int var3 = (int)Math.floor(var1) & 0xFF;
      float var4 = var0 - (float)Math.floor(var0);
      float var5 = var1 - (float)Math.floor(var1);
      float var6 = m_7827(var4);
      float var7 = m_7827(var5);
      int var8 = m_7918(var2) + var3;
      int var9 = m_7918(var2) + var3 + 1;
      int var10 = m_7918(var2 + 1) + var3;
      int var11 = m_7918(var2 + 1) + var3 + 1;
      float var12 = Mth.lerp(var6, m_7900(m_7918(var8), var4, var5), m_7900(m_7918(var10), var4 - 1.0F, var5));
      float var13 = Mth.lerp(var6, m_7900(m_7918(var9), var4, var5 - 1.0F), m_7900(m_7918(var11), var4 - 1.0F, var5 - 1.0F));
      return Mth.clamp(Mth.lerp(var7, var12, var13) * 0.5F + 0.5F, 0.0F, 1.0F);
   }

   public static void renderFade(CombatWorldRenderEvent var0) {
      if (f_26543 != null && f_26544 > 0.01F) {
         m_7914(var0, null, f_26547, f_26568, f_26569);
      }
   }

   private static void m_7927(LivingEntity var0) {
      float var1 = var0.getBbWidth() * 1.5F;
      float var2 = var0.getBbHeight();
      if (f_26552 != var0.getId() || f_26553 != var1 || f_26554 != var2) {
         f_26552 = var0.getId();
         f_26553 = var1;
         f_26554 = var2;
         f_26548.clear();
         Random var3 = new Random(var0.getId() * 133769420L);
         int var4 = (int)(var1 + var2 * 12.0F);

         for (int var5 = 0; var5 < var4; var5++) {
            Vector3f var6 = null;
            float var7 = -1.0F;

            for (int var8 = 0; var8 < 15; var8++) {
               float var9 = var3.nextFloat() * 360.0F;
               float var10 = var3.nextFloat() * var2;
               float var11 = (float)Math.toRadians(var9);
               float var12 = (float)Math.sin(var11) * var1;
               float var13 = (float)Math.cos(var11) * var1;
               if (f_26548.isEmpty()) {
                  var6 = new Vector3f(var12, var10, var13);
                  break;
               }

               float var14 = f_27754;

               for (Vector3f var16 : f_26548) {
                  float var17 = var16.x - var12;
                  float var18 = var16.y - var10;
                  float var19 = var16.z - var13;
                  var14 = Math.min(var14, var17 * var17 + var18 * var18 + var19 * var19);
               }

               if (var14 > var7) {
                  var7 = var14;
                  var6 = new Vector3f(var12, var10, var13);
               }
            }

            if (var6 != null) {
               f_26548.add(var6);
            }
         }
      }
   }

   private static Vec3 m_7928(Vec3 var0) {
      Vec3 var1 = var0.normalize();
      Vec3 var2 = Math.abs(var1.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      Vec3 var3 = var1.cross(var2).normalize();
      Vec3 var4 = var1.cross(var3).normalize();
      double var5 = f_26557.nextDouble() * f_27247 * 2.0;
      return var3.scale(Math.cos(var5)).add(var4.scale(Math.sin(var5)));
   }

   private static void m_7929(BufferBuilder var0, RenderPipeline var1, RenderTarget var2, boolean var3) {
      MeshData var4 = var0.build();
      if (var4 != null) {
         GpuBuffer var5 = null;

         try {
            DrawState var6 = var4.drawState();
            int var7 = var6.indexCount();
            AutoStorageIndexBuffer var8 = RenderSystem.getSequentialBuffer(var6.primitiveTopology());
            GpuBuffer var9 = var8.getBuffer(var7);
            IndexType var10 = var8.type();
            var5 = RenderSystem.getDevice().createBuffer(() -> "targetEspMaskVertices", 32, var4.vertexBuffer());
            CommandEncoder var11 = RenderSystem.getDevice().createCommandEncoder();
            RenderPass var12 = var11.createRenderPass(
               () -> "targetEspMask",
               var2.getColorTextureView(),
               var3 ? Optional.of(new Vector4f(0.0F)) : Optional.empty(),
               var2.getDepthTextureView(),
               OptionalDouble.empty()
            );

            try {
               RenderSystem.bindDefaultUniforms(var12);
               var12.setPipeline(var1);
               var12.setVertexBuffer(0, var5.slice());
               var12.setIndexBuffer(var9, var10);
               var12.drawIndexed(var7, 1, 0, 0, 0);
            } catch (Throwable var20) {
               if (var12 != null) {
                  try {
                     var12.close();
                  } catch (Throwable var19) {
                     var20.addSuppressed(var19);
                  }
               }

               throw var20;
            }

            if (var12 != null) {
               var12.close();
            }
         } finally {
            if (var5 != null) {
               var5.close();
            }

            var4.close();
         }
      }
   }

   private static void m_7931(BufferBuilder var0, Matrix4f var1, float var2, float var3, float var4, float var5, float var6, Vector4f var7) {
      var0.addVertex(var1, var2, var3, var4).setUv(var5, var6).setColor(var7.x, var7.y, var7.z, var7.w);
   }

   private static double m_7932(double var0, double var2, float var4, int var5, int var6, int var7) {
      double var8 = 2.45 + (var5 > 0 ? 0.05 : 0.0) + (var7 > 0 ? 0.04 : 0.0);
      double var10 = 0.045 + (var6 > 0 ? 0.018 : 0.0);
      double var12 = var0 + var10 * var4 - var8 * var4 * var4;
      if (var12 >= var2) {
         return var12;
      }

      double var14 = Mth.clamp(Math.sqrt(Math.max(0.0, (var0 - var2) / var8)), 0.0, 0.96);
      double var16 = Mth.clamp((var4 - var14) / Math.max(0.04, 1.0 - var14), 0.0, 1.0);
      double var18 = 0.065 + (var5 * var7 > 0 ? 0.018 : 0.0);
      double var20 = Math.abs(Math.sin(var16 * f_26935 * 2.15)) * var18 * (1.0 - var16) * (1.0 - var16);
      return var2 + var20;
   }

   private static List<Vec3> m_7934(List<Vec3> var0, int var1) {
      if (var0.size() >= 3 && var1 > 1) {
         ArrayList var2 = new ArrayList((var0.size() - 1) * var1 + 1);

         for (int var3 = 0; var3 < var0.size() - 1; var3++) {
            Vec3 var4 = (Vec3)var0.get(Math.max(0, var3 - 1));
            Vec3 var5 = (Vec3)var0.get(var3);
            Vec3 var6 = (Vec3)var0.get(var3 + 1);
            Vec3 var7 = (Vec3)var0.get(Math.min(var0.size() - 1, var3 + 2));

            for (int var8 = 0; var8 < var1; var8++) {
               double var9 = (double)var8 / var1;
               var2.add(m_7962(var4, var5, var6, var7, var9));
            }
         }

         var2.add((Vec3)var0.get(var0.size() - 1));
         return var2;
      } else {
         return new ArrayList<>(var0);
      }
   }

   private static void m_7935(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, long var5) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      m_7927(var2);
      float var7 = (float)(var5 % 360000L) / 7.25F;
      float var8 = (float)var5 * 0.001F;
      float var9 = var2.getBbWidth() * 1.5F;
      float var10 = var2.getBbHeight();
      float var11 = 1.25F - 0.5F * var4;
      float var12 = (float)Math.sin(Math.toRadians(var7));
      float var13 = (float)Math.cos(Math.toRadians(var7));
      int var14 = m_7887(var3, var4);
      PoseStack var15 = var0.getPose();
      BufferBuilder var16 = m_7952();

      for (int var17 = 0; var17 < f_26548.size(); var17++) {
         Vector3f var18 = f_26548.get(var17);
         float var19 = var18.x * var13 - var18.z * var12;
         float var20 = var18.x * var12 + var18.z * var13;
         float var21 = var19 * var11;
         float var22 = var20 * var11;
         float var23 = var18.y + (float)(0.05 * Math.sin(var8 * 2.0F + var17 * 1337));
         var15.pushPose();
         var15.translate(var1.x, var1.y, var1.z);
         m_7809(var16, var15, var21, var23, var22, 0.15F, var14, var10, var7 + var17 * 19.3F);
         var15.popPose();
         m_7954(var16, var15, var1.x + var21, var1.y + var23, var1.z + var22, 0.9625F, m_7887(var3, var4 * 0.2F));
         m_7954(var16, var15, var1.x + var21, var1.y + var23, var1.z + var22, 0.4375F, m_7887(var3, var4 * 0.22F));
      }

      m_7830(var16, f_26515, true, m_7831());
   }

   private static float m_7937(float var0) {
      float var1 = 1.0F - Mth.clamp(var0, 0.0F, 1.0F);
      return 1.0F - var1 * var1 * var1;
   }

   private static void m_7938(
      BufferBuilder var0, PoseStack var1, float var2, float var3, float var4, float var5, int var6, int var7, float var8, int var9, float var10, boolean var11
   ) {
      var1.pushPose();
      var1.translate(var2, var3, var4);
      float var12 = var10 * 0.5F;
      float var13 = -var2;
      float var14 = var12 - var3;
      float var15 = -var4;
      float var16 = var13 * var13 + var14 * var14 + var15 * var15;
      if (var16 > 1.0E-8F) {
         float var17 = 1.0F / (float)Math.sqrt(var16);
         float var18 = Mth.clamp(var14 * var17, -1.0F, 1.0F);
         float var19 = (float)Math.toDegrees(Math.atan2(var13, var15));
         float var20 = -((float)Math.toDegrees(Math.asin(var18)));
         var1.mulPose(Axis.YP.rotationDegrees(var19));
         var1.mulPose(Axis.XP.rotationDegrees(var20));
      }

      var1.mulPose(Axis.ZP.rotationDegrees(var8));
      var1.scale(var5 * 1.3F, var5 * 1.3F, var5 * 1.3F);
      Matrix4f var29 = var1.last().pose();
      float var30 = 0.92F + m_7948(var9, 1.7F) * 0.16F;
      float var31 = 0.54F * var30;
      float var32 = 0.76F * (0.92F + m_7948(var9, 4.3F) * 0.16F);
      float var21 = 1.12F * (0.92F + m_7948(var9, 8.1F) * 0.12F);
      float var22 = (m_7948(var9, 12.6F) - 0.5F) * 0.07F;
      float var23 = (m_7948(var9, 17.2F) - 0.5F) * 0.07F;
      int var24 = CombatColors.m_2297(var6);
      int var25 = CombatColors.m_2303(
         CombatColors.m_2309(var7), CombatColors.m_2299(var7), CombatColors.m_2308(var7), var24
      );
      int var26 = var11 ? m_7817(var6, 0.58F) : var6;
      int var27 = var11 ? m_7817(var25, 0.42F) : var25;
      int var28 = var11 ? m_7817(var6, 0.18F) : var6;
      m_7904(var0, var29, var22, var23, var32, -var31, 0.0F, 0.0F, 0.0F, var31, 0.0F, var26, var28, var28);
      m_7904(var0, var29, var22, var23, var32, 0.0F, var31, 0.0F, var31, 0.0F, 0.0F, var26, var28, var28);
      m_7904(var0, var29, var22, var23, var32, var31, 0.0F, 0.0F, 0.0F, -var31, 0.0F, var26, var28, var28);
      m_7904(var0, var29, var22, var23, var32, 0.0F, -var31, 0.0F, -var31, 0.0F, 0.0F, var26, var28, var28);
      m_7904(var0, var29, -var22 * 0.45F, -var23 * 0.45F, -var21, 0.0F, var31, 0.0F, -var31, 0.0F, 0.0F, var27, var28, var28);
      m_7904(var0, var29, -var22 * 0.45F, -var23 * 0.45F, -var21, var31, 0.0F, 0.0F, 0.0F, var31, 0.0F, var27, var28, var28);
      m_7904(var0, var29, -var22 * 0.45F, -var23 * 0.45F, -var21, 0.0F, -var31, 0.0F, var31, 0.0F, 0.0F, var27, var28, var28);
      m_7904(var0, var29, -var22 * 0.45F, -var23 * 0.45F, -var21, -var31, 0.0F, 0.0F, 0.0F, -var31, 0.0F, var27, var28, var28);
      var1.popPose();
   }

   private static void m_7939(BufferBuilder var0, Matrix4f var1, Vec3 var2, Vec3 var3, Vec3 var4, Vec3 var5, double var6, int var8, int var9) {
      Vec3 var10 = var5.subtract(var4).normalize();
      Vec3 var11 = Math.abs(var10.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      Vec3 var12 = var10.cross(var11).normalize().scale(var6);
      Vec3 var13 = var10.cross(var12).normalize().scale(var6);

      for (int var14 = 0; var14 < var8; var14++) {
         double var15 = f_26938 * var14 / var8;
         double var17 = f_26939 * (var14 + 1) / var8;
         Vec3 var19 = var12.scale(Math.cos(var15)).add(var13.scale(Math.sin(var15)));
         Vec3 var20 = var12.scale(Math.cos(var17)).add(var13.scale(Math.sin(var17)));
         m_7945(
            var0,
            var1,
            m_7870(var4.add(var19), var2, var3),
            m_7870(var4.add(var20), var2, var3),
            m_7870(var5.add(var20), var2, var3),
            m_7870(var5.add(var19), var2, var3),
            var9
         );
      }
   }

   public static boolean m_7940() {
      return f_26632;
   }

   private static void m_7941(BufferBuilder var0, BufferBuilder var1) {
      m_7913(var0, var1, f_26537, -1);
   }

   private static void m_7942(BufferBuilder var0, Identifier var1, boolean var2) {
      m_7830(var0, var1, var2, f_26520);
   }

   private static void m_7943(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, long var5) {
      var1 = var1.subtract(mc.gameRenderer.mainCamera().position());
      float var7 = (float)Math.sin(f_26545 * 3.0F) * 0.08F;
      float var8 = 1.0F + 0.04F * (float)Math.sin(f_26545 * 2.0F);
      float var9 = (0.42F + var2.getBbWidth() * 0.32F) * var4 * m_7906(f_26544) * var8;
      if (!(var9 <= 0.001F)) {
         double var10 = var1.x;
         double var12 = var1.y + var2.getBbHeight() + 0.85F + var7;
         double var14 = var1.z;
         float var16 = -mc.gameRenderer.mainCamera().yRot();
         PoseStack var17 = var0.getPose();
         BufferBuilder var18 = m_7952();
         var17.pushPose();
         var17.translate(var10, var12, var14);
         var17.mulPose(Axis.YP.rotationDegrees(var16));
         int var19 = m_7887(CombatColors.m_2312(var3, -1, 0.3F), 0.1764706F * var4);
         m_7855(var18, var17.last().pose(), var9 * 2.1F, var19);
         var17.popPose();
         m_7942(var18, f_26518, true);
         var17.pushPose();
         var17.translate(var10, var12, var14);
         var17.mulPose(Axis.YP.rotationDegrees(var16));
         var17.scale(var9, var9, var9);
         m_7852(var17, var3, var4, var5);
         var17.popPose();
      }
   }

   private static void m_7944(
      BufferBuilder var0,
      PoseStack var1,
      float var2,
      float var3,
      float var4,
      float var5,
      int var6,
      int var7,
      float var8,
      boolean var9,
      int var10,
      boolean var11
   ) {
      var1.pushPose();
      var1.translate(var2, var3, var4);
      var1.mulPose(Axis.XP.rotationDegrees(var8 * 0.61F + 23.0F));
      var1.mulPose(Axis.YP.rotationDegrees(var8 * 1.27F + 51.0F));
      var1.mulPose(Axis.ZP.rotationDegrees(var8 + 17.0F));
      var1.scale(var5 * 1.3F, var5 * 1.3F, var5 * 1.3F);
      Matrix4f var12 = var1.last().pose();
      float var13 = 0.62F * (0.92F + m_7948(var10, 1.7F) * 0.16F);
      float var14 = 0.76F * (0.92F + m_7948(var10, 4.3F) * 0.16F);
      float var15 = 0.76F * (0.92F + m_7948(var10, 8.1F) * 0.16F);
      float var16 = (m_7948(var10, 12.6F) - 0.5F) * 0.08F;
      float var17 = (m_7948(var10, 17.2F) - 0.5F) * 0.08F;
      int var18 = var9 ? m_7817(var6, 0.58F) : var6;
      int var19 = var6;
      int var20 = var9
         ? CombatColors.m_2303(
            CombatColors.m_2309(var7), CombatColors.m_2299(var7), CombatColors.m_2308(var7), CombatColors.m_2297(var6)
         )
         : var6;
      int var21 = var11 ? var20 : var18;
      int var22 = var11 ? var18 : var20;
      m_7904(var0, var12, var16, var17, var14, -var13, 0.0F, 0.0F, 0.0F, var13, 0.0F, var21, var19, var19);
      m_7904(var0, var12, var16, var17, var14, 0.0F, var13, 0.0F, var13, 0.0F, 0.0F, var21, var19, var19);
      m_7904(var0, var12, var16, var17, var14, var13, 0.0F, 0.0F, 0.0F, -var13, 0.0F, var21, var19, var19);
      m_7904(var0, var12, var16, var17, var14, 0.0F, -var13, 0.0F, -var13, 0.0F, 0.0F, var21, var19, var19);
      m_7904(var0, var12, -var16 * 0.6F, -var17 * 0.6F, -var15, 0.0F, var13, 0.0F, -var13, 0.0F, 0.0F, var22, var19, var19);
      m_7904(var0, var12, -var16 * 0.6F, -var17 * 0.6F, -var15, var13, 0.0F, 0.0F, 0.0F, var13, 0.0F, var22, var19, var19);
      m_7904(var0, var12, -var16 * 0.6F, -var17 * 0.6F, -var15, 0.0F, -var13, 0.0F, var13, 0.0F, 0.0F, var22, var19, var19);
      m_7904(var0, var12, -var16 * 0.6F, -var17 * 0.6F, -var15, -var13, 0.0F, 0.0F, 0.0F, -var13, 0.0F, var22, var19, var19);
      var1.popPose();
   }

   private static void m_7945(BufferBuilder var0, Matrix4f var1, Vec3 var2, Vec3 var3, Vec3 var4, Vec3 var5, int var6) {
      m_7812(var0, var1, (float)var2.x, (float)var2.y, (float)var2.z, 0.0F, 0.0F, var6);
      m_7812(var0, var1, (float)var3.x, (float)var3.y, (float)var3.z, 1.0F, 0.0F, var6);
      m_7812(var0, var1, (float)var4.x, (float)var4.y, (float)var4.z, 1.0F, 1.0F, var6);
      m_7812(var0, var1, (float)var5.x, (float)var5.y, (float)var5.z, 0.0F, 1.0F, var6);
   }

   private static void m_7946(BufferBuilder var0, PoseStack var1, float var2, int var3) {
      var1.pushPose();
      var1.scale(var2, var2, var2);
      Matrix4f var4 = var1.last().pose();

      for (int var5 = 0; var5 < f_26550.length; var5++) {
         int var6 = CombatColors.m_2303(
            Math.round(CombatColors.m_2309(var3) * f_26551[var5]),
            Math.round(CombatColors.m_2299(var3) * f_26551[var5]),
            Math.round(CombatColors.m_2308(var3) * f_26551[var5]),
            CombatColors.m_2297(var3)
         );
         float[] var7 = f_26549[f_26550[var5][0]];
         float[] var8 = f_26549[f_26550[var5][1]];
         float[] var9 = f_26549[f_26550[var5][2]];
         Vector4f var10 = CombatColors.m_2298(var6);
         m_7931(var0, var4, var7[0], var7[1], var7[2], 0.5F, 0.5F, var10);
         m_7931(var0, var4, var8[0], var8[1], var8[2], 0.5F, 0.5F, var10);
         m_7931(var0, var4, var9[0], var9[1], var9[2], 0.5F, 0.5F, var10);
      }

      var1.popPose();
   }

   private static float m_7948(int var0, float var1) {
      float var2 = (float)Math.sin(var0 * 78.233F + var1) * 43758.547F;
      return var2 - (float)Math.floor(var2);
   }

   private static void m_7949(PoseStack var0, BufferBuilder var1, double var2, double var4, double var6, float var8, float var9, int var10) {
      var0.pushPose();
      var0.translate(var2, var4, var6);
      var0.mulPose(mc.gameRenderer.mainCamera().rotation());
      Matrix4f var11 = var0.last().pose();
      m_7855(var1, var11, var8 * 2.1F, m_7887(var10, var9 * 0.48F));
      m_7855(var1, var11, var8, m_7887(var10, var9));
      var0.popPose();
   }

   private static void m_7950(Vec3[] var0, int var1, int var2, double var3, int var5, Random var6) {
      if (var2 - var1 >= 2 && !(var3 < 0.015) && var5 <= 3) {
         int var7 = (var1 + var2) / 2;
         Vec3 var8 = var0[var1];
         Vec3 var9 = var0[var2];
         Vec3 var10 = var9.subtract(var8);
         if (!(var10.lengthSqr() < 1.0E-8)) {
            var10 = var10.normalize();
            Vec3 var11 = var10.cross(Math.abs(var10.y) < 0.85 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0));
            if (var11.lengthSqr() < 1.0E-8) {
               var11 = new Vec3(0.0, 0.0, 1.0);
            }

            var11 = var11.normalize();
            Vec3 var12 = var10.cross(var11).normalize();
            double var13 = (var6.nextDouble() - 0.5) * 2.0 * var3;
            double var15 = (var6.nextDouble() - 0.5) * 2.0 * var3 * 0.35;
            var0[var7] = var8.lerp(var9, 0.5).add(var11.scale(var13)).add(var12.scale(var15));
            m_7950(var0, var1, var7, var3 * 0.5, var5 + 1, var6);
            m_7950(var0, var7, var2, var3 * 0.5, var5 + 1, var6);
         }
      }
   }

   private static void m_7951(double var0, double var2, double var4, long var6, float var8) {
      long var9 = System.currentTimeMillis();
      if (var9 - f_26610 >= 25L) {
         f_26610 = var9;
         double var11 = 0.15 * var6 / 25.0;
         m_7896(var0, var2, var4, var11, 1.0, 1.0, -1.0, 0.2F, var8);
         m_7896(var0, var2, var4, var11, -1.0, 1.0, -1.0, 0.2F, var8);
         m_7896(var0, var2, var4, var11, -1.0, -1.0, 1.0, 0.2F, var8);
      }
   }

   private static BufferBuilder m_7952() {
      return CombatVertexAllocator.m_4161().m_4162(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
   }

   private static void m_7954(BufferBuilder var0, PoseStack var1, double var2, double var4, double var6, float var8, int var9) {
      var1.pushPose();
      var1.translate(var2, var4, var6);
      var1.mulPose(mc.gameRenderer.mainCamera().rotation());
      m_7855(var0, var1.last().pose(), var8, var9);
      var1.popPose();
   }

   private static void m_7955(BufferBuilder var0, Matrix4f var1, Vec3 var2, Vec3 var3, Vec3 var4, float var5, int var6) {
      Vector4f var7 = CombatColors.m_2298(var6);
      float var8 = var5 * 0.5F;
      Vec3 var9 = var2.subtract(var3.scale(var8)).subtract(var4.scale(var8));
      Vec3 var10 = var2.subtract(var3.scale(var8)).add(var4.scale(var8));
      Vec3 var11 = var2.add(var3.scale(var8)).add(var4.scale(var8));
      Vec3 var12 = var2.add(var3.scale(var8)).subtract(var4.scale(var8));
      m_7931(var0, var1, (float)var9.x, (float)var9.y, (float)var9.z, 0.0F, 0.0F, var7);
      m_7931(var0, var1, (float)var10.x, (float)var10.y, (float)var10.z, 0.0F, 1.0F, var7);
      m_7931(var0, var1, (float)var11.x, (float)var11.y, (float)var11.z, 1.0F, 1.0F, var7);
      m_7931(var0, var1, (float)var12.x, (float)var12.y, (float)var12.z, 1.0F, 0.0F, var7);
   }

   private static Vec3[] m_7956(Vec3 var0) {
      Vec3 var1 = new Vec3(0.0, 1.0, 0.0).subtract(var0.scale(var0.y));
      if (var1.lengthSqr() < 1.0E-6) {
         return m_7860(var0);
      }

      Vec3 var2 = var1.normalize();
      Vec3 var3 = var2.cross(var0).normalize();
      return new Vec3[]{var3, var2};
   }

   private static void m_7958(LivingEntity var0) {
      if (f_26625 != var0.getId()) {
         f_26625 = var0.getId();
         f_26626 = var0.getId() % 1000 * 0.0628F;
      }
   }

   private static void m_7959(CombatWorldRenderEvent var0, Vec3 var1, LivingEntity var2, int var3, float var4, float var5, boolean var6, float var7) {
      Matrix4f var8 = var0.getPose().last().pose();
      Vec3 var9 = mc.gameRenderer.mainCamera().position();
      boolean var10 = var6 && var2.hurtTime > 7;
      if (var10 && !f_26636) {
         f_26640 = f_26638;
         f_26641 = f_26639;
         f_26642 = f_26640;
         f_26643 = f_26641;
         switch (f_26557.nextInt(4)) {
            case 0:
               f_26643 = f_26643 + f_26900;
               break;
            case 1:
               f_26643 = f_26643 - f_26901;
               break;
            case 2:
               f_26642 = f_26642 + f_26902;
               break;
            default:
               f_26642 = f_26642 - f_26903;
         }

         f_26635 = 0.0F;
         f_26637 = true;
         f_26636 = true;
      } else if (!var10) {
         f_26636 = false;
      }

      if (!var6 && f_26637) {
         float var11 = m_7832(f_26635);
         f_26638 = Mth.lerp(var11, f_26640, f_26642);
         f_26639 = Mth.lerp(var11, f_26641, f_26643);
         f_26637 = false;
      }

      if (f_26637) {
         f_26635 = Math.min(1.0F, f_26635 + var5 / 0.22F);
         if (f_26635 >= 1.0F) {
            f_26638 = f_26642;
            f_26639 = f_26643;
            f_26637 = false;
         }
      }

      float var39 = m_7832(f_26635);
      float var12 = 0.0F;
      var12 = m_7832(var12);
      float var13 = var6 ? Mth.clamp(var4, 0.0F, 1.0F) : 1.0F;
      float var14 = var6 ? Math.min(1.08F, m_7906(m_7832(var13))) : 1.0F;
      float var15 = var6 ? 0.0F : Mth.clamp(1.0F - var4, 0.0F, 1.0F);
      double var16 = 1.0;
      double var18 = 0.18 + var14 * 0.82;
      double var20 = 0.34 * var16 * var18;
      double var22 = 0.155 * var16 * var18;
      double var24 = 0.031 * var16 * (0.2 + var14 * 0.8);
      double var26 = f_26637 ? Mth.lerp(var39, f_26640, f_26642) : f_26638;
      double var28 = f_26637 ? Mth.lerp(var39, f_26641, f_26643) : f_26639;
      Vec3 var30 = var1.add(0.0, var2.getBbHeight() * 0.58, 0.0);
      double var31 = -var2.getBbHeight() * 0.58 + var24;
      int var33 = pizza.dlc.api.theme.ThemeManager.accent();
      int var34 = f_26596 ? CombatColors.m_2302(var33, -65536, var7) : var33;
      float var35 = var6 ? 1.0F : 1.0F - m_7832(Mth.clamp((var15 - 0.76F) / 0.24F, 0.0F, 1.0F));
      int var36 = m_7887(var34, var35);
      RenderTarget var37 = mc.gameRenderer.mainRenderTarget();
      m_7963(var37.width, var37.height);
      BufferBuilder var38 = m_7952();
      m_7818(var38, var8, var30, var9, var26, var28, var20, var22, var24, var31, var15, var36);
      m_7929(var38, f_26533, f_26627, true);
      if (f_26628) {
         CombatDistortionRenderer.f_31830.m_8868(f_26627, f_26629);
      }

      f_26633 = var34;
      f_26634 = 1.0F + var12 * 0.35F;
      f_26632 = true;
   }

   private static float m_7960(CombatTargetEffects.CrystalParticle var0, long var1, float var3) {
      float var4 = (float)(var1 - var0.f_26486) / (float)var0.f_26487;
      if (!(var4 < 0.0F) && !(var4 > 1.0F)) {
         float var5 = m_7937(Mth.clamp(var4 / 0.12F, 0.0F, 1.0F));
         float var6 = 1.0F - m_7832(Mth.clamp((var4 - 0.78F) / 0.22F, 0.0F, 1.0F));
         float var7 = 0.92F + 0.08F * m_7858((int)((var1 - var0.f_26486) / 90L) + 1, var0.f_26488);
         return var3 * var5 * var6 * var7;
      } else {
         return 0.0F;
      }
   }

   private static void m_7961(CombatTargetEffects.CrystalParticle var0, Vec3 var1, float var2, float var3, int var4, List<Vec3> var5) {
      var5.clear();
      byte var6 = 12;
      Vec3[] var7 = new Vec3[var6];
      float var8 = var0.f_26485 + var4 * 19.37F;
      float var9 = Math.max(var0.f_26478, 1.0F);

      for (int var10 = 0; var10 < var6; var10++) {
         float var11 = (float)var10 / (var6 - 1);
         float var12 = (m_7858(var10 + 3 + var4 * 11, var8) - 0.5F) * 0.35F;
         float var13 = (m_7858(var10 + 71 + var4 * 5, var8) - 0.5F) * 0.1F;
         float var14 = (m_7858(var10 + 19 + var4, var8) - 0.5F) * 0.07F;
         double var15 = var0.f_26476 + var0.f_26477 * var9 * f_27122 * 2.0 * var11 + var12;
         double var17 = var3 * var0.f_26481 * (1.0F + var13);
         var17 = Mth.clamp((float)var17, var3 * 0.92F, var3 * 1.1F);
         double var19 = var2 * Mth.lerp(var11, var0.f_26480, var0.f_26479) + var2 * var14;
         var7[var10] = var1.add(Math.cos(var15) * var17, var19, Math.sin(var15) * var17);
      }

      m_7950(var7, 0, var6 - 1, var3 * 0.14, 0, new Random(Float.floatToIntBits(var8) * 31L + var4 * 97L));

      for (int var21 = 0; var21 < var6; var21++) {
         Vec3 var23 = var7[var21].subtract(var1);
         double var25 = Math.sqrt(var23.x * var23.x + var23.z * var23.z);
         if (var25 > 1.0E-6) {
            double var28 = var3 * var0.f_26481;
            double var16 = var28 / var25;
            double var18 = 0.55;
            var7[var21] = var1.add(var23.x * (1.0 - var18 + var18 * var16), var23.y, var23.z * (1.0 - var18 + var18 * var16));
         }
      }

      for (int var22 = 0; var22 < 28; var22++) {
         float var24 = var22 / 27.0F;
         float var26 = var24 * (var6 - 1);
         int var27 = Math.min(var6 - 2, (int)var26);
         float var29 = var26 - var27;
         var5.add(var7[var27].lerp(var7[var27 + 1], var29));
      }
   }

   private static Vec3 m_7962(Vec3 var0, Vec3 var1, Vec3 var2, Vec3 var3, double var4) {
      double var6 = var4 * var4;
      double var8 = var6 * var4;
      return new Vec3(
         0.5
            * (
               2.0 * var1.x
                  + (-var0.x + var2.x) * var4
                  + (2.0 * var0.x - 5.0 * var1.x + 4.0 * var2.x - var3.x) * var6
                  + (-var0.x + 3.0 * var1.x - 3.0 * var2.x + var3.x) * var8
            ),
         0.5
            * (
               2.0 * var1.y
                  + (-var0.y + var2.y) * var4
                  + (2.0 * var0.y - 5.0 * var1.y + 4.0 * var2.y - var3.y) * var6
                  + (-var0.y + 3.0 * var1.y - 3.0 * var2.y + var3.y) * var8
            ),
         0.5
            * (
               2.0 * var1.z
                  + (-var0.z + var2.z) * var4
                  + (2.0 * var0.z - 5.0 * var1.z + 4.0 * var2.z - var3.z) * var6
                  + (-var0.z + 3.0 * var1.z - 3.0 * var2.z + var3.z) * var8
            )
      );
   }

   private static void m_7963(int var0, int var1) {
      if (f_26627 == null) {
         f_26627 = new TextureTarget("target-esp-cube-core", var0, var1, false, GpuFormat.RGBA8_UNORM);
      } else if (f_26627.width != var0 || f_26627.height != var1) {
         f_26627.resize(var0, var1);
      }
   }

   private static final class GhostPoint {
      public final Vec3 f_26459;
      public final long f_26460;
      public final float f_26461;
      public final float f_26462;

      public GhostPoint(Vec3 var1, long var2, float var4, float var5) {
         this.f_26459 = var1;
         this.f_26460 = var2;
         this.f_26461 = var4;
         this.f_26462 = var5;
      }
   }

   private record OrbitParticle(float x, float y, float z, float size, float spin) {
   }

   private static final class FadeParticle {
      public final double f_26463;
      public final double f_26464;
      public final double f_26465;
      public final long f_26466;
      public final int f_26467;
      public float f_26468;
      public float f_26469;
      public float f_26470 = 1.0F;
      public long f_26471;
      public long f_26472;

      public void m_7806(long var1) {
         float var3 = Mth.clamp((float)(var1 - this.f_26471) / (float)this.f_26472, 0.0F, 1.0F);
         this.f_26468 = Mth.lerp(var3, this.f_26469, this.f_26470);
         this.f_26469 = this.f_26468;
         this.f_26470 = var1 - this.f_26466 < 200L ? 1.0F : 0.0F;
         this.f_26471 = var1;
         this.f_26472 = 50L;
      }

      public FadeParticle(double var1, double var3, double var5, long var7, int var9) {
         this.f_26472 = 100L;
         this.f_26463 = var1;
         this.f_26464 = var3;
         this.f_26465 = var5;
         this.f_26466 = var7;
         this.f_26467 = var9;
         this.f_26471 = var7;
      }
   }

   private static final class CrystalParticle {
      public final double f_26476;
      public final int f_26477;
      public final float f_26478;
      public final float f_26479;
      public final float f_26480;
      public final float f_26481;
      public final float f_26482;
      public final float f_26483;
      public final int f_26484;
      public final float f_26485;
      public final long f_26486;
      public final long f_26487;
      public final float f_26488;

      public CrystalParticle(
         double var1,
         int var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         float var9,
         int var10,
         float var11,
         long var12,
         long var14,
         float var16
      ) {
         this.f_26476 = var1;
         this.f_26477 = var3;
         this.f_26478 = var4;
         this.f_26479 = var5;
         this.f_26480 = var6;
         this.f_26481 = var7;
         this.f_26482 = var8;
         this.f_26483 = var9;
         this.f_26484 = var10;
         this.f_26485 = var11;
         this.f_26486 = var12;
         this.f_26487 = var14;
         this.f_26488 = var16;
      }
   }

   private static final class LightningBranch {
      public final List<Vec3> f_26489;
      public final float f_26490;
      public final float f_26491;

      public LightningBranch(List<Vec3> var1, float var2, float var3) {
         this.f_26489 = var1;
         this.f_26490 = var2;
         this.f_26491 = var3;
      }
   }

   private static final class LightningArc {
      public final List<Vec3> f_26492;
      public final long f_26493;
      public final long f_26494;
      public final long f_26495;
      public final float f_26496;
      public final float f_26497;
      public final int f_26498;
      public final float f_26499;

      public LightningArc(List<Vec3> var1, long var2, long var4, long var6, float var8, float var9, int var10, float var11) {
         this.f_26492 = var1;
         this.f_26493 = var2;
         this.f_26494 = var4;
         this.f_26495 = var6;
         this.f_26496 = var8;
         this.f_26497 = var9;
         this.f_26498 = var10;
         this.f_26499 = var11;
      }
   }

   private static final class LightningBurst {
      public final List<CombatTargetEffects.LightningBranch> f_26500;
      public final Vec3 f_26501;
      public final Vec3 f_26502;
      public final long f_26503;
      public final long f_26504;
      public final float f_26505;
      public final int f_26506;
      public final double f_26507;
      public final double f_26508;
      public final float f_26509;
      public final float f_26510;

      public LightningBurst(
         List<CombatTargetEffects.LightningBranch> var1,
         Vec3 var2,
         Vec3 var3,
         long var4,
         long var6,
         float var8,
         int var9,
         double var10,
         double var12,
         float var14,
         float var15
      ) {
         this.f_26500 = var1;
         this.f_26501 = var2;
         this.f_26502 = var3;
         this.f_26503 = var4;
         this.f_26504 = var6;
         this.f_26505 = var8;
         this.f_26506 = var9;
         this.f_26507 = var10;
         this.f_26508 = var12;
         this.f_26509 = var14;
         this.f_26510 = var15;
      }
   }
}
