package pizza.dlc.mixin.client;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseStreamProofAccessor {
   @Invoker("onButton") void pizzadlc$onButton(long handle, MouseButtonInfo button, int action);
   @Invoker("onMove") void pizzadlc$onMove(long handle, double x, double y);
   @Invoker("onScroll") void pizzadlc$onScroll(long handle, double horizontal, double vertical);
   @Accessor("isLeftPressed") void pizzadlc$leftPressed(boolean pressed);
   @Accessor("isMiddlePressed") void pizzadlc$middlePressed(boolean pressed);
   @Accessor("isRightPressed") void pizzadlc$rightPressed(boolean pressed);
   @Accessor("activeButton") void pizzadlc$activeButton(MouseButtonInfo button);
   @Accessor("accumulatedDX") void pizzadlc$deltaX(double delta);
   @Accessor("accumulatedDY") void pizzadlc$deltaY(double delta);
}
