package pizza.dlc.api.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import pizza.dlc.mixin.client.RenderPipelinesAccessor;

public final class PizzaDLCPipelines {
   public static final VertexFormat ROUNDED_FORMAT = VertexFormat.builder(0)
      .addAttribute("Position", GpuFormat.RGB32_FLOAT)
      .addAttribute("Color", GpuFormat.RGBA8_UNORM)
      .addAttribute("UV0", GpuFormat.RG32_FLOAT)
      .addAttribute("UV1", GpuFormat.RG16_SINT)
      .addAttribute("UV2", GpuFormat.RG16_SINT)
      .build();
   public static final RenderPipeline ROUNDED = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/rounded"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/rounded"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/rounded"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline ROUNDED_OUTLINE = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/rounded_outline"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/rounded_outline"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/rounded_outline"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline CHEVRON = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/chevron"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/chevron"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/chevron"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline ARC = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/arc"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/arc"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/arc"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline STAR = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/star"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/star"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/star"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline CRYSTAL = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/crystal"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/crystal"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/crystal"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline ROUNDED_GRAD = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/roundedgrad"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/roundedgrad"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/roundedgrad"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline SV_SQUARE = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/svsquare"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/roundedgrad"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/svsquare"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline HUE_BAR = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/huebar"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/roundedgrad"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/huebar"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline ALPHA_BAR = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/alphabar"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/roundedgrad"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/alphabar"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline MENU_BG = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/menubg"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/menubg"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/menubg"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final VertexFormat MSDF_FORMAT = VertexFormat.builder(0)
      .addAttribute("Position", GpuFormat.RGB32_FLOAT)
      .addAttribute("UV0", GpuFormat.RG32_FLOAT)
      .addAttribute("Color", GpuFormat.RGBA8_UNORM)
      .build();
   public static final RenderPipeline MSDF = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiTexturedSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/msdf"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/msdf"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/msdf"))
      .withVertexBinding(0, MSDF_FORMAT)
      .build();
   public static final RenderPipeline GLOW = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/glow"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/glow"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/glow"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline KAWASE_DOWN = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/kawase_down"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/kawase_down"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/kawase_down"))
      .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
      .build();
   public static final RenderPipeline KAWASE_UP = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/kawase_up"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/kawase_up"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/kawase_up"))
      .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
      .build();
   public static final RenderPipeline SHADERESP_BLUR = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/shaderesp_blur"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
      .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
      .build();
   public static final RenderPipeline SHADERESP_EDGE = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/shaderesp_edge"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_edge"))
      .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
      .build();
   public static final RenderPipeline SHADERESP_GLOW = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/shaderesp_glow"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_glow"))
      .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
      .withColorTargetState(0, new ColorTargetState(Optional.of(BlendFunction.LIGHTNING), GpuFormat.RGBA8_UNORM, 7))
      .build();
   public static final RenderPipeline SHADERESP_DIR_BOTH = shaderEspDir("shaderesp_dir_both");
   public static final RenderPipeline SHADERESP_DIR_OUT = shaderEspDir("shaderesp_dir_out");
   public static final RenderPipeline SHADERESP_DIR_IN = shaderEspDir("shaderesp_dir_in");
   public static final RenderPipeline SATURATION_COPY = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/saturation_copy"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_glow"))
      .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
      .withColorTargetState(0, ColorTargetState.DEFAULT)
      .build();
   private static final BindGroupLayout SAT_LAYOUT = BindGroupLayout.builder()
      .withSampler("InSampler")
      .withUniform("SatConfig", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline SATURATION = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/saturation"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/saturation"))
      .withBindGroupLayout(SAT_LAYOUT)
      .withColorTargetState(0, ColorTargetState.DEFAULT)
      .build();
   private static final BindGroupLayout SKY_LAYOUT = BindGroupLayout.builder().withUniform("SkyConfig", UniformType.UNIFORM_BUFFER).build();
   public static final RenderPipeline SKY_AURORA = skyPipe("sky_aurora");
   public static final RenderPipeline SKY_SAKURA = skyPipe("sky_sakura");
   public static final RenderPipeline SKY_PLASMA = skyPipe("sky_plasma");
   public static final RenderPipeline SKY_PLASMA2 = skyPipe("sky_plasma2");
   public static final RenderPipeline SKY_NORTHERN = skyPipe("sky_northern");
   public static final RenderPipeline SKY_NIGHT = skyPipe("sky_night");
   public static final RenderPipeline SKY_SUMMER = skyPipe("sky_summer");
   public static final RenderPipeline SKY_CAUSTIC = skyPipe("sky_caustic");
   private static final BindGroupLayout ARRAYSHADOW_LAYOUT = BindGroupLayout.builder().withUniform("ShadowConfig", UniformType.UNIFORM_BUFFER).build();   public static final RenderPipeline ARRAYSHADOW = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/arrayshadow"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/arrayshadow"))
      .withBindGroupLayout(ARRAYSHADOW_LAYOUT)
      .withColorTargetState(0, new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), GpuFormat.RGBA8_UNORM, 7))
      .build();
   public static final RenderPipeline MENU_BG_PASS = RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/menubg_pass"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/menubg_pass"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/menubg_pass"))
      .build();
   public static final RenderPipeline GLASS = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiTexturedSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/glass"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/glass"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/glass"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline FACE = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiTexturedSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/face"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/face"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/face"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();
   public static final RenderPipeline ROUNDED_TEXTURE = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.pizzadlc$guiTexturedSnippet()})
      .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/rounded_texture"))
      .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/rounded_texture"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/rounded_texture"))
      .withVertexBinding(0, ROUNDED_FORMAT)
      .build();

   private PizzaDLCPipelines() {
   }

   private static RenderPipeline shaderEspDir(String fsh) {
      return RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/" + fsh))
         .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
         .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/" + fsh))
         .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1)
         .withColorTargetState(0, new ColorTargetState(Optional.of(BlendFunction.LIGHTNING), GpuFormat.RGBA8_UNORM, 7))
         .build();
   }

   private static RenderPipeline skyPipe(String fsh) {
      return RenderPipeline.builder(new Snippet[]{RenderPipelines.POST_PROCESSING_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/" + fsh))
         .withVertexShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/shaderesp_blur"))
         .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/" + fsh))
         .withBindGroupLayout(SKY_LAYOUT)
         .withColorTargetState(0, ColorTargetState.DEFAULT)
         .build();
   }
}
