package pizza.dlc.api.util.render.combat;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

public final class CombatPipelineBuilder {
   public static final VertexFormat f_7670 = VertexFormat.builder(0).build();
   public static final Snippet f_7671 = RenderPipeline.builder(new Snippet[0]).withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).buildSnippet();
   private final Builder f_7672;
   private final com.mojang.blaze3d.pipeline.BindGroupLayout.Builder f_7673 = BindGroupLayout.builder();
   private final Set<String> f_7674 = new HashSet<>();
   private boolean f_7675;
   private CompareOp f_7676 = CompareOp.GREATER_THAN_OR_EQUAL;
   private boolean f_7677 = true;
   private boolean f_7678;
   private float f_7679;
   private float f_7680;
   private Optional<BlendFunction> f_7681 = Optional.empty();
   private int f_7682 = 15;
   private boolean f_7683;
   private GpuFormat f_7684 = GpuFormat.RGBA8_UNORM;

   public CombatPipelineBuilder m_2714(GpuFormat var1) {
      this.f_7684 = var1;
      this.f_7683 = true;
      return this;
   }

   public CombatPipelineBuilder m_2715(PolygonMode var1) {
      this.f_7672.withPolygonMode(var1);
      return this;
   }

   private CombatPipelineBuilder(Snippet... var1) {
      this.f_7672 = RenderPipeline.builder(var1);

      for (Snippet var5 : var1) {
         var5.bindGroupLayouts().ifPresent(var1x -> var1x.forEach(var1xx -> {
            this.f_7674.addAll(var1xx.getSamplers());
            var1xx.getUniforms().forEach(var1xxx -> this.f_7674.add(var1xxx.name()));
         }));
         var5.depthStencilState().ifPresent(var1x -> {
            this.f_7676 = var1x.depthTest();
            this.f_7677 = var1x.writeDepth();
            this.f_7679 = var1x.depthBiasScaleFactor();
            this.f_7680 = var1x.depthBiasConstant();
         });
         if (var5.colorTargetStates() != null && var5.activeColorTargetStateCount() > 0 && var5.colorTargetStates()[0] != null) {
            this.f_7684 = var5.colorTargetStates()[0].format();
            this.f_7681 = var5.colorTargetStates()[0].blendFunction();
            this.f_7682 = var5.colorTargetStates()[0].writeMask();
         }
      }
   }

   public static CombatPipelineBuilder m_2718(Snippet... var0) {
      return new CombatPipelineBuilder(var0);
   }

   public CombatPipelineBuilder m_2719(CompareOp var1) {
      this.f_7676 = var1;
      this.f_7678 = true;
      return this;
   }

   public CombatPipelineBuilder m_2720(boolean var1, boolean var2) {
      this.f_7682 = (var1 ? 7 : 0) | (var2 ? 8 : 0);
      this.f_7683 = true;
      return this;
   }

   public CombatPipelineBuilder m_2721(float var1, float var2) {
      this.f_7679 = var1;
      this.f_7680 = var2;
      this.f_7678 = true;
      return this;
   }

   public RenderPipeline m_2722() {
      this.m_2724();
      return this.f_7672.build();
   }

   public CombatPipelineBuilder m_2723(String var1, float var2) {
      this.f_7672.withShaderDefine(var1, var2);
      return this;
   }

   private void m_2724() {
      if (this.f_7675) {
         this.f_7672.withBindGroupLayout(this.f_7673.build());
         this.f_7675 = false;
      }

      if (this.f_7678) {
         this.f_7672
            .withDepthStencilState(
               this.f_7676 == CompareOp.ALWAYS_PASS ? Optional.empty() : Optional.of(new DepthStencilState(this.f_7676, this.f_7677, this.f_7679, this.f_7680))
            );
      }

      if (this.f_7683) {
         this.f_7672.withColorTargetState(new ColorTargetState(this.f_7681, this.f_7684, this.f_7682));
      }
   }

   public CombatPipelineBuilder m_2725(boolean var1) {
      this.f_7672.withCull(var1);
      return this;
   }

   public CombatPipelineBuilder m_2726(String var1) {
      this.f_7672.withLocation(var1);
      return this;
   }

   public CombatPipelineBuilder m_2727() {
      this.f_7681 = Optional.empty();
      this.f_7683 = true;
      return this;
   }

   public Snippet m_2728() {
      this.m_2724();
      return this.f_7672.buildSnippet();
   }

   public CombatPipelineBuilder m_2729(String var1, UniformType var2) {
      if (this.f_7674.add(var1)) {
         this.f_7673.withUniform(var1, var2);
         this.f_7675 = true;
      }

      return this;
   }

   public CombatPipelineBuilder m_2730(String var1) {
      this.f_7672.withFragmentShader(var1);
      return this;
   }

   public CombatPipelineBuilder m_2732(BlendFunction var1) {
      this.f_7681 = Optional.of(var1);
      this.f_7683 = true;
      return this;
   }

   public CombatPipelineBuilder m_2733(String var1, int var2) {
      this.f_7672.withShaderDefine(var1, var2);
      return this;
   }

   public CombatPipelineBuilder m_2734(Identifier var1) {
      this.f_7672.withVertexShader(var1);
      return this;
   }

   public CombatPipelineBuilder m_2736(String var1) {
      this.f_7672.withShaderDefine(var1);
      return this;
   }

   public CombatPipelineBuilder m_2737(boolean var1) {
      this.f_7677 = var1;
      this.f_7678 = true;
      return this;
   }

   public CombatPipelineBuilder m_2738(String var1) {
      this.f_7672.withVertexShader(var1);
      return this;
   }

   public CombatPipelineBuilder m_2739(VertexFormat var1, PrimitiveTopology var2) {
      if (var1.getVertexSize() != 0) {
         this.f_7672.withVertexBinding(0, var1);
      }

      this.f_7672.withPrimitiveTopology(var2);
      return this;
   }

   public CombatPipelineBuilder m_2740(Identifier var1) {
      this.f_7672.withFragmentShader(var1);
      return this;
   }

   public CombatPipelineBuilder m_2741(String var1) {
      if (this.f_7674.add(var1)) {
         this.f_7673.withSampler(var1);
         this.f_7675 = true;
      }

      return this;
   }

   public CombatPipelineBuilder m_2742(Identifier var1) {
      this.f_7672.withLocation(var1);
      return this;
   }
}
