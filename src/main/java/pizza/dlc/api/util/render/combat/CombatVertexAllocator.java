package pizza.dlc.api.util.render.combat;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class CombatVertexAllocator implements AutoCloseable {
   private final ByteBufferBuilder f_11800;

   public void m_4160() {
      this.f_11800.clear();
   }

   @Override
   public void close() {
      this.f_11800.close();
   }

   public static CombatVertexAllocator m_4161() {
      return CombatVertexAllocator.Holder.f_11798;
   }

   public CombatVertexAllocator(int var1) {
      this.f_11800 = new ByteBufferBuilder(var1);
   }

   public BufferBuilder m_4162(PrimitiveTopology var1, VertexFormat var2) {
      return new BufferBuilder(this.f_11800, var1, var2);
   }

   private static final class Holder {
      public static final CombatVertexAllocator f_11798 = new CombatVertexAllocator(786432);

      public Holder() {
      }
   }
}
