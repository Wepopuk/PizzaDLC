package zako.opt.mixin.gl;

import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={RenderType.class})
public interface RenderTypeAccessor {
    @Accessor(value="name")
    public String zakoopt$name();
}

