package pizza.dlc.api.util.input;

import com.mojang.blaze3d.platform.InputConstants.Type;

public final class KeyNameUtil {
   public static final int MOUSE_BUTTON_BASE = -100;
   public static final int MOUSE_WHEEL_UP = -200;
   public static final int MOUSE_WHEEL_DOWN = -201;

   private KeyNameUtil() {
   }

   public static String displayName(int code) {
      if (code == -1 || code == -1) {
         return "None";
      }

      if (code == -200) {
         return "WheelUp";
      }

      if (code == -201) {
         return "WheelDn";
      }

      if (code <= -100) {
         return "MB" + (-100 - code + 1);
      }

      if (code >= 320 && code <= 329) {
         return "Num" + (code - 320);
      }

      switch (code) {
         case 330:
            return "Num.";
         case 331:
            return "Num/";
         case 332:
            return "Num*";
         case 333:
            return "Num-";
         case 334:
            return "Num+";
         case 335:
            return "NumEnter";
         case 336:
            return "Num=";
         default:
            if (code >= 65 && code <= 90) {
               return String.valueOf((char)(65 + (code - 65)));
            }

            if (code >= 48 && code <= 57) {
               return String.valueOf((char)(48 + (code - 48)));
            }

            switch (code) {
               case 39:
                  return "'";
               case 44:
                  return ",";
               case 45:
                  return "-";
               case 46:
                  return ".";
               case 47:
                  return "/";
               case 59:
                  return ";";
               case 61:
                  return "=";
               case 91:
                  return "[";
               case 92:
                  return "\\";
               case 93:
                  return "]";
               case 96:
                  return "`";
               default:
                  return Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
            }
      }
   }

   public static String getKeyName(int glfwKeyCode) {
      if (glfwKeyCode == -1) {
         return "UNKNOWN";
      }

      if (glfwKeyCode == -200) {
         return "WHEELUP";
      }

      if (glfwKeyCode == -201) {
         return "WHEELDN";
      }

      if (glfwKeyCode <= -100) {
         return "MB" + (-100 - glfwKeyCode + 1);
      }

      if (glfwKeyCode >= 65 && glfwKeyCode <= 90) {
         return String.valueOf((char)(65 + (glfwKeyCode - 65)));
      }

      if (glfwKeyCode >= 48 && glfwKeyCode <= 57) {
         return String.valueOf((char)(48 + (glfwKeyCode - 48)));
      }

      if (glfwKeyCode >= 290 && glfwKeyCode <= 314) {
         return "F" + (glfwKeyCode - 290 + 1);
      }

      return switch (glfwKeyCode) {
         case 32 -> "SPACE";
         case 39 -> "'";
         case 44 -> ",";
         case 45 -> "-";
         case 46 -> ".";
         case 47 -> "/";
         case 59 -> ";";
         case 61 -> "=";
         case 91 -> "[";
         case 92 -> "\\";
         case 93 -> "]";
         case 96 -> "`";
         case 256 -> "ESC";
         case 257 -> "ENTER";
         case 258 -> "TAB";
         case 259 -> "BACK";
         case 260 -> "INS";
         case 261 -> "DEL";
         case 262 -> "RIGHT";
         case 263 -> "LEFT";
         case 264 -> "DOWN";
         case 265 -> "UP";
         case 266 -> "PGUP";
         case 267 -> "PGDN";
         case 268 -> "HOME";
         case 269 -> "END";
         case 280 -> "CAPS";
         case 281 -> "SCRL";
         case 282 -> "NUM";
         case 283 -> "PRT";
         case 284 -> "PAUSE";
         case 320 -> "NUM0";
         case 321 -> "NUM1";
         case 322 -> "NUM2";
         case 323 -> "NUM3";
         case 324 -> "NUM4";
         case 325 -> "NUM5";
         case 326 -> "NUM6";
         case 327 -> "NUM7";
         case 328 -> "NUM8";
         case 329 -> "NUM9";
         case 330 -> "NUM.";
         case 331 -> "NUM/";
         case 332 -> "NUM*";
         case 333 -> "NUM-";
         case 334 -> "NUM+";
         case 335 -> "NUMENTER";
         case 336 -> "NUM=";
         case 340 -> "LSHIFT";
         case 341 -> "LCTRL";
         case 342 -> "LALT";
         case 343 -> "LWIN";
         case 344 -> "RSHIFT";
         case 345 -> "RCTRL";
         case 346 -> "RALT";
         case 347 -> "RWIN";
         case 348 -> "MENU";
         default -> "KEY" + glfwKeyCode;
      };
   }

   public static String getKeyName(int glfwKeyCode, int maxLen) {
      String name = getKeyName(glfwKeyCode);
      if (maxLen <= 0) {
         return "";
      }

      return name.length() > maxLen ? name.substring(0, maxLen) : name;
   }
}
