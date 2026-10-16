package pizza.dlc.api.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.SharedLibrary;

public final class WindowTheme {
   private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
   private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19;
   private static SharedLibrary dwmapi;
   private static long dwmSetWindowAttribute;

   private WindowTheme() {
   }

   public static void applyFromSystem() {
      if (!isWindows()) return;

      try {
         long hwnd = hwnd();
         if (hwnd == 0L) {
            return;
         }

         setDarkTitleBar(hwnd, isSystemDark());
      } catch (Throwable ignored) {
      }
   }

   private static boolean isWindows() {
      return System.getProperty("os.name", "").toLowerCase().contains("win");
   }

   private static long hwnd() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null) {
         long glfw = mc.getWindow().handle();
         return glfw == 0L ? 0L : GLFWNativeWin32.glfwGetWin32Window(glfw);
      }

      return 0L;
   }

   private static void setDarkTitleBar(long hwnd, boolean dark) {
      long fn = fnAddress();
      if (fn != 0L) {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            IntBuffer value = stack.mallocInt(1);
            value.put(0, dark ? 1 : 0);
            long pv = MemoryUtil.memAddress(value);
            int hr = JNI.invokePPI(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, pv, 4, fn);
            if (hr != 0) {
               JNI.invokePPI(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, pv, 4, fn);
            }
         } catch (Throwable ex) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable ex2) {
                  ex.addSuppressed(ex2);
               }
            }

            throw ex;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   private static long fnAddress() {
      if (dwmSetWindowAttribute == 0L) {
         if (dwmapi == null) {
            dwmapi = APIUtil.apiCreateLibrary("dwmapi");
         }

         dwmSetWindowAttribute = APIUtil.apiGetFunctionAddress(dwmapi, "DwmSetWindowAttribute");
      }

      return dwmSetWindowAttribute;
   }

   private static boolean isSystemDark() {
      try {
         Process p = new ProcessBuilder("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme")
            .redirectErrorStream(true)
            .start();
         StringBuilder out = new StringBuilder();
         BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

         String line;
         try {
            while ((line = r.readLine()) != null) {
               out.append(line).append('\n');
            }
         } catch (Throwable ex) {
            try {
               r.close();
            } catch (Throwable ex2) {
               ex.addSuppressed(ex2);
            }

            throw ex;
         }

         r.close();
         p.waitFor();
         String s = out.toString();
         int nameIdx = s.indexOf("AppsUseLightTheme");
         if (nameIdx < 0) {
            return false;
         }

         int idx = s.indexOf("0x", nameIdx);
         if (idx < 0) {
            return false;
         }

         int end = idx + 2;

         while (end < s.length() && isHex(s.charAt(end))) {
            end++;
         }

         return Integer.parseInt(s.substring(idx + 2, end), 16) == 0;
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static boolean isHex(char c) {
      return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
   }
}
