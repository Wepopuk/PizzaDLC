package pizza.dlc.client.modules.impl.visuals.hud;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.render.Render2DUtil;

public final class MusicHud implements QuickImports {
   private static final float TITLE_FS = 8.0F;
   private static final float ARTIST_FS = 6.5F;
   private static final int PAD = 4;
   private static final int ART = 20;
   private static final float ART_RADIUS = 4.0F;
   private static final int GAP = 6;
   private static final int TEXT_W = 80;
   private static final int EQ_H = 8;
   private static final int BARS = 4;
   private static final int BAR_W = 2;
   private static final int BAR_GAP = 1;
   private static final int EQ_W = BARS * BAR_W + (BARS - 1) * BAR_GAP;
   private static final int MARGIN = 4;
   private static final float SLIDE = 6.0F;
   private static final float SCROLL_PXPS = 22.0F;
   private static final float SCROLL_SPACING = 24.0F;
   private final Draggable drag = new Draggable("Music", 0.0F, 0.0F, 0.0F, 0.0F);
   private boolean anchored;
   private final float[] levels = new float[BARS];
   private float vis;
   private String lastTitle;
   private String lastArtist;

   public void render(GuiGraphicsExtractor ctx, boolean editMode) {
      MusicTrackerService svc = MusicTrackerService.getInstance();
      IMediaSession session = svc.getSession();
      boolean present = session != null;
      MediaInfo media = present ? session.getMedia() : null;
      float target = !present && !editMode ? 0.0F : 1.0F;
      this.vis = this.vis + (target - this.vis) * 0.2F;
      if (this.vis < 0.01F && target < 0.5F) {
         this.vis = 0.0F;
      } else {
         String title;
         String artist;
         if (present && media != null) {
            title = nz(media.getTitle());
            artist = media.getArtist() != null ? media.getArtist().trim() : "";
            this.lastTitle = title;
            this.lastArtist = artist;
         } else {
            title = this.lastTitle != null ? this.lastTitle : pizza.dlc.api.translation.Translations.text("Музыка");
            artist = this.lastArtist != null ? this.lastArtist : pizza.dlc.api.translation.Translations.text("Ничего не играет");
         }

         boolean playing = present && media != null && media.isPlaying();
         int panelW = PAD + ART + GAP + TEXT_W + GAP + EQ_W + PAD + 2;
         int panelH = PAD * 2 + ART;
         this.drag.setWidth(panelW);
         this.drag.setHeight(panelH);
         if (!this.drag.isPositioned()) {
            this.drag.setX(MARGIN);
            this.drag.setY(ctx.guiHeight() - MARGIN - panelH);
            this.drag.syncRenderPosition();
            this.drag.setPositioned(true);
            this.anchored = true;
         } else if (this.anchored && !this.drag.isDragging()) {
            this.drag.setY(ctx.guiHeight() - MARGIN - panelH);
            this.drag.syncRenderPosition();
         }

         this.drag.update(ctx);
         if (this.drag.isDragging()) {
            this.anchored = false;
         }

         float a = this.vis;
         int px = (int)this.drag.getRenderX();
         int py = (int)this.drag.getRenderY() + Math.round((1.0F - a) * SLIDE);
         HudStyle.panel(ctx, px, py, panelW, panelH, HudStyle.RADIUS, a);
         int artX = px + PAD;
         int artY = py + PAD;
         Identifier art = present ? svc.getArtwork() : null;
         if (art != null) {
            Render2DUtil.roundedTexture(ctx, art, artX, artY, ART, ART, ART_RADIUS, true, HudStyle.fade(-1, a));
         } else {
            Render2DUtil.roundedRect(ctx, (float)artX, (float)artY, (float)ART, (float)ART, ART_RADIUS, HudStyle.fade(HudStyle.PILL, a));
            HudStyle.gem(ctx, artX + ART / 2.0F, artY + ART / 2.0F, 11.0F, a);
         }

         int tx = artX + ART + GAP;
         MsdfFont titleFont = HudStyle.title();
         MsdfFont bodyFont = HudStyle.body();
         float titleY = py + PAD + 2.5F;
         float artistY = titleY + 0.7F * TITLE_FS + 4.0F;
         ctx.enableScissor(tx, py, tx + TEXT_W, py + panelH);
         float full = titleFont.width(title, TITLE_FS);
         int titleCol = HudStyle.fade(HudStyle.TEXT, a);
         if (full > TEXT_W) {
            float period = full + SCROLL_SPACING;
            float off = (float)(System.nanoTime() / 1.0E9 * SCROLL_PXPS % period);
            Render2DUtil.msdfText(ctx, titleFont, title, tx - off, titleY, TITLE_FS, titleCol);
            Render2DUtil.msdfText(ctx, titleFont, title, tx - off + period, titleY, TITLE_FS, titleCol);
         } else {
            Render2DUtil.msdfText(ctx, titleFont, title, tx, titleY, TITLE_FS, titleCol);
         }

         ctx.disableScissor();
         String shownArtist = HudStyle.truncate(bodyFont, artist.isEmpty() ? "—" : artist, ARTIST_FS, TEXT_W);
         Render2DUtil.msdfText(ctx, bodyFont, shownArtist, tx, artistY, ARTIST_FS, HudStyle.fade(HudStyle.TEXT_DIM, a));
         this.drawEqualizer(ctx, px + panelW - PAD - 1 - EQ_W, py + (panelH + EQ_H) / 2, playing, a);
      }
   }

   private void drawEqualizer(GuiGraphicsExtractor ctx, int ex, int bottom, boolean playing, float a) {
      double t = System.nanoTime() / 1.0E9;
      int col = HudStyle.fade(playing ? HudStyle.TEXT : HudStyle.TEXT_OFF, a);

      for (int i = 0; i < BARS; i++) {
         float tg = playing
            ? Math.max(0.15F, Math.min(1.0F, 0.45F + 0.3F * (float)Math.sin(t * 7.0 + i * 1.7) + 0.22F * (float)Math.sin(t * 11.0 + i * 2.6 + 0.5)))
            : 0.15F;
         this.levels[i] = this.levels[i] + (tg - this.levels[i]) * 0.3F;
         int bh = Math.max(BAR_W, Math.round(this.levels[i] * EQ_H));
         Render2DUtil.roundedRect(ctx, ex + i * (BAR_W + BAR_GAP), bottom - bh, BAR_W, bh, BAR_W / 2.0F, col);
      }
   }

   private static String nz(String s) {
      return s != null && !s.isBlank() ? s : "—";
   }
}
