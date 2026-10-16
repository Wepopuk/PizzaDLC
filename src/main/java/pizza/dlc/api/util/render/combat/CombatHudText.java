package pizza.dlc.api.util.render.combat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class CombatHudText {
 private static final ThreadLocal<GuiGraphicsExtractor> CURRENT=new ThreadLocal<>();
 public static void begin(GuiGraphicsExtractor context) { CURRENT.set(context); }
 public static void end() { CURRENT.remove(); }
 public static float width(String text,float size,float spacing) { return pizza.dlc.api.render.msdf.PizzaDLCFonts.regular().width(text,size); }
 public static void draw(String text,float x,float y,float size,int color) {
  var context=CURRENT.get(); if(context==null) throw new IllegalStateException("Font extraction outside GUI event");
  float scale=(float)net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
  pizza.dlc.api.util.render.Render2DUtil.msdfText(context,text,x/scale,y/scale,size/scale,color);
 }
}
