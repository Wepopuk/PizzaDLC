package pizza.dlc.api.render.msdf;

import com.google.gson.Gson;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class MsdfFont {
   private static final Gson GSON = new Gson();
   private final Identifier atlasId;
   private final Map<Integer, MsdfGlyph> glyphs = new HashMap<>();

   private MsdfFont(Identifier atlasId, FontData data) {
      this.atlasId = atlasId;
      float aw = data.atlas.width;
      float ah = data.atlas.height;

      for (FontData.Glyph g : data.glyphs) {
         this.glyphs.put(g.unicode, new MsdfGlyph(g, aw, ah));
      }
   }

   public static MsdfFont load(String name) {
      String jsonPath = "/assets/pizzadlc/fonts/msdf/" + name + ".json";
      Identifier atlasId = Identifier.fromNamespaceAndPath("pizzadlc", "fonts/msdf/" + name + ".png");

      FontData data;
      try (InputStream is = MsdfFont.class.getResourceAsStream(jsonPath)) {
         if (is == null) {
            throw new IllegalStateException("MSDF JSON не найден на classpath: " + jsonPath);
         }

         data = (FontData)GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), FontData.class);
      } catch (Exception ex) {
         throw new RuntimeException("Не удалось загрузить MSDF-шрифт '" + name + "'", ex);
      }

      if (data != null && data.atlas != null && data.glyphs != null) {
         return new MsdfFont(atlasId, data);
      }

      throw new RuntimeException("Битые данные MSDF-шрифта: " + jsonPath);
   }

   public MsdfGlyph glyph(int codepoint) {
      return this.glyphs.get(codepoint);
   }

   public GpuTextureView atlasView() {
      return Minecraft.getInstance().getTextureManager().getTexture(this.atlasId).getTextureView();
   }

   public float width(String text, float size) {
      text = ReplaceSymbols.normalize(text);
      float w = 0.0F;

      for (int i = 0; i < text.length(); i++) {
         MsdfGlyph g = this.glyphs.get(Integer.valueOf(text.charAt(i)));
         if (g != null) {
            w += g.advance * size;
         }
      }

      return w;
   }

   public float visualWidth(String text, float size) {
      text = ReplaceSymbols.normalize(text);
      float pen = 0.0F;
      float right = 0.0F;

      for (int i = 0; i < text.length(); i++) {
         MsdfGlyph g = this.glyphs.get(Integer.valueOf(text.charAt(i)));
         if (g != null) {
            right = pen + g.width * size;
            pen += g.advance * size;
         }
      }

      return right;
   }
}
