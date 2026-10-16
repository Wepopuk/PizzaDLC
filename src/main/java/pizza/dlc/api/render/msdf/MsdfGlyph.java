package pizza.dlc.api.render.msdf;

public final class MsdfGlyph {
   public final float advance;
   public final float minU;
   public final float maxU;
   public final float minV;
   public final float maxV;
   public final float width;
   public final float height;
   public final float topPos;

   public MsdfGlyph(FontData.Glyph g, float atlasW, float atlasH) {
      this.advance = g.advance;
      FontData.Bounds a = g.atlasBounds;
      if (a != null) {
         this.minU = a.left / atlasW;
         this.maxU = a.right / atlasW;
         this.minV = 1.0F - a.top / atlasH;
         this.maxV = 1.0F - a.bottom / atlasH;
      } else {
         this.minU = this.maxU = this.minV = this.maxV = 0.0F;
      }

      FontData.Bounds p = g.planeBounds;
      if (p != null) {
         this.width = p.right - p.left;
         this.height = p.top - p.bottom;
         this.topPos = p.top;
      } else {
         this.width = this.height = this.topPos = 0.0F;
      }
   }
}
