package pizza.dlc.api.ui;

import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class TextEdit {
   private final int maxLength;
   private final IntPredicate filter;
   private String text = "";
   private int caret;
   private int anchor = -1;
   private long lastChangeNanos = System.nanoTime();

   public TextEdit(int maxLength, IntPredicate filter) {
      this.maxLength = maxLength;
      this.filter = filter;
   }

   public String text() {
      return this.text;
   }

   public int caret() {
      return this.caret;
   }

   public boolean hasSelection() {
      return this.anchor >= 0 && this.anchor != this.caret;
   }

   public int selStart() {
      return this.hasSelection() ? Math.min(this.anchor, this.caret) : this.caret;
   }

   public int selEnd() {
      return this.hasSelection() ? Math.max(this.anchor, this.caret) : this.caret;
   }

   public String selected() {
      return this.text.substring(this.selStart(), this.selEnd());
   }

   public boolean allSelected() {
      return !this.text.isEmpty() && this.selStart() == 0 && this.selEnd() == this.text.length();
   }

   public float sinceChange() {
      return (System.nanoTime() - this.lastChangeNanos) / 1.0E9F;
   }

   public void set(String value) {
      this.text = this.sanitize(value == null ? "" : value, this.maxLength);
      this.caret = this.text.length();
      this.anchor = -1;
      this.touch();
   }

   public void selectAll() {
      this.anchor = 0;
      this.caret = this.text.length();
      this.touch();
   }

   public void clearSelection() {
      this.anchor = -1;
   }

   public boolean insert(String value) {
      String before = this.text;
      int start = this.selStart();
      int end = this.selEnd();
      String base = this.text.substring(0, start) + this.text.substring(end);
      String add = this.sanitize(value, this.maxLength - base.length());
      this.text = base.substring(0, start) + add + base.substring(start);
      this.caret = start + add.length();
      this.anchor = -1;
      this.touch();
      return !before.equals(this.text);
   }

   public boolean key(KeyEvent e) {
      if (e.isSelectAll()) {
         this.selectAll();
         return true;
      }
      if (e.isCopy()) {
         setClipboard(this.hasSelection() ? this.selected() : this.text);
         return true;
      }
      if (e.isCut()) {
         setClipboard(this.hasSelection() ? this.selected() : this.text);
         if (!this.hasSelection()) this.selectAll();
         this.insert("");
         return true;
      }
      if (e.isPaste()) {
         this.insert(getClipboard().replace("\r", "").replace("\n", " "));
         return true;
      }

      boolean shift = e.hasShiftDown();
      boolean word = e.hasControlDown();
      switch (e.key()) {
         case GLFW.GLFW_KEY_BACKSPACE -> {
            if (!this.hasSelection()) {
               this.anchor = this.caret;
               this.caret = word ? this.wordLeft(this.caret) : this.prev(this.caret);
            }
            this.insert("");
         }
         case GLFW.GLFW_KEY_DELETE -> {
            if (!this.hasSelection()) {
               this.anchor = this.caret;
               this.caret = word ? this.wordRight(this.caret) : this.next(this.caret);
            }
            this.insert("");
         }
         case GLFW.GLFW_KEY_LEFT -> this.moveTo(word ? this.wordLeft(this.caret) : (this.hasSelection() && !shift ? this.selStart() : this.prev(this.caret)), shift);
         case GLFW.GLFW_KEY_RIGHT -> this.moveTo(word ? this.wordRight(this.caret) : (this.hasSelection() && !shift ? this.selEnd() : this.next(this.caret)), shift);
         case GLFW.GLFW_KEY_HOME -> this.moveTo(0, shift);
         case GLFW.GLFW_KEY_END -> this.moveTo(this.text.length(), shift);
         default -> {
            return false;
         }
      }
      return true;
   }

   private void moveTo(int pos, boolean extend) {
      if (extend) {
         if (this.anchor < 0) this.anchor = this.caret;
      } else {
         this.anchor = -1;
      }
      this.caret = Math.max(0, Math.min(this.text.length(), pos));
      this.touch();
   }

   private int prev(int pos) {
      return pos <= 0 ? 0 : this.text.offsetByCodePoints(pos, -1);
   }

   private int next(int pos) {
      return pos >= this.text.length() ? this.text.length() : this.text.offsetByCodePoints(pos, 1);
   }

   private int wordLeft(int pos) {
      int i = pos;
      while (i > 0 && Character.isWhitespace(this.text.charAt(i - 1))) i--;
      while (i > 0 && !Character.isWhitespace(this.text.charAt(i - 1))) i--;
      return i;
   }

   private int wordRight(int pos) {
      int i = pos;
      int n = this.text.length();
      while (i < n && Character.isWhitespace(this.text.charAt(i))) i++;
      while (i < n && !Character.isWhitespace(this.text.charAt(i))) i++;
      return i;
   }

   private String sanitize(String value, int room) {
      StringBuilder sb = new StringBuilder();
      value.codePoints().forEach(cp -> {
         if (cp >= ' ' && cp != 127 && this.filter.test(cp) && sb.length() + Character.charCount(cp) <= Math.max(0, room)) {
            sb.appendCodePoint(cp);
         }
      });
      return sb.toString();
   }

   private void touch() {
      this.lastChangeNanos = System.nanoTime();
   }

   private static String getClipboard() {
      String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
      return clip == null ? "" : clip;
   }

   private static void setClipboard(String value) {
      if (!value.isEmpty()) Minecraft.getInstance().keyboardHandler.setClipboard(value);
   }
}
