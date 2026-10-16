package pizza.dlc.api.util.render.streamproof;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import org.lwjgl.glfw.GLFWNativeWin32;

final class WindowsCaptureOverlay {
   private WindowsCaptureOverlay() { }

   static void protect(long overlay, long main) throws Exception {
      try (Arena arena = Arena.ofConfined()) {
         SymbolLookup user32 = SymbolLookup.libraryLookup("user32.dll", arena);
         Linker linker = Linker.nativeLinker();
         MethodHandle getStyle = linker.downcallHandle(user32.find("GetWindowLongPtrW").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
         MethodHandle setStyle = linker.downcallHandle(user32.find("SetWindowLongPtrW").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
         MethodHandle setAffinity = linker.downcallHandle(user32.find("SetWindowDisplayAffinity").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
         MethodHandle getAffinity = linker.downcallHandle(user32.find("GetWindowDisplayAffinity").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
         long hwnd = GLFWNativeWin32.glfwGetWin32Window(overlay);
         long owner = GLFWNativeWin32.glfwGetWin32Window(main);
         long style = (long)getStyle.invokeExact(hwnd, -20);
         long ignored = (long)setStyle.invokeExact(hwnd, -20, (style | 0x08000080L) & ~0x00040000L);
         ignored = (long)setStyle.invokeExact(hwnd, -8, owner);
         if ((long)getStyle.invokeExact(hwnd, -8) != owner) throw new IllegalStateException("Не удалось привязать слой к Minecraft");
         MemorySegment affinity = arena.allocate(ValueLayout.JAVA_INT);
         if ((int)setAffinity.invokeExact(hwnd, 0x11) == 0
            || (int)getAffinity.invokeExact(hwnd, affinity) == 0 || affinity.get(ValueLayout.JAVA_INT, 0) != 0x11) {
            throw new IllegalStateException("Windows не поддерживает исключение слоя из захвата");
         }
      } catch (Exception error) {
         throw error;
      } catch (Throwable error) {
         throw new IllegalStateException("Не удалось настроить слой Windows", error);
      }
   }
}
