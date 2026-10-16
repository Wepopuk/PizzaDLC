package pizza.dlc.api.render.msdf;

import java.util.List;

public final class FontData {
   public Atlas atlas;
   public Metrics metrics;
   public List<Glyph> glyphs;

   public static final class Atlas {
      public float width;
      public float height;
   }

   public static final class Bounds {
      public float left;
      public float top;
      public float right;
      public float bottom;
   }

   public static final class Glyph {
      public int unicode;
      public float advance;
      public Bounds planeBounds;
      public Bounds atlasBounds;
   }

   public static final class Metrics {
      public float ascender;
      public float lineHeight;
      public float descender;
   }
}
