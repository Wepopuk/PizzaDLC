package pizza.dlc.client.modules.impl.visuals.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.interfaces.QuickImports;

public final class MusicTrackerService implements QuickImports {
   private static final MusicTrackerService INSTANCE = new MusicTrackerService();
   private final Thread worker;
   private final Map<Integer, Identifier> textureCache = new ConcurrentHashMap<>();
   private volatile IMediaSession session;
   private volatile Identifier artworkId;
   private volatile int artW;
   private volatile int artH;
   private int lastArtHash;

   private MusicTrackerService() {
      this.worker = new Thread(this::runLoop, "pizzadlc-music-poll");
      this.worker.setDaemon(true);
      this.worker.start();
   }

   public static MusicTrackerService getInstance() {
      return INSTANCE;
   }

   private void runLoop() {
      while (!Thread.currentThread().isInterrupted()) {
         try {
            Thread.sleep(100L);
            this.poll();
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         } catch (Throwable ignored) {
         }
      }
   }

   private void poll() {
      try {
         List<IMediaSession> sessions = MediaPlayerInfo.INSTANCE.getMediaSessions();
         IMediaSession s = sessions.stream()
            .filter(c -> c != null && c.getMedia() != null)
            .filter(c -> !safe(c.getMedia().getTitle()).isBlank())
            .findFirst()
            .orElse(null);
         this.session = s;
         if (s == null) {
            this.artworkId = null;
            this.lastArtHash = 0;
            return;
         }

         byte[] data = s.getMedia().getArtworkPng();
         if (data == null || data.length == 0) {
            this.artworkId = null;
            this.lastArtHash = 0;
            return;
         }

         int hash = Arrays.hashCode(data);
         if (hash == this.lastArtHash && this.artworkId != null) {
            return;
         }

         this.lastArtHash = hash;
         Identifier cached = this.textureCache.get(hash);
         if (cached != null) {
            this.artworkId = cached;
            return;
         }

         NativeImage original = NativeImage.read(new ByteArrayInputStream(data));
         boolean spotify = s.getOwner() != null && s.getOwner().toLowerCase().contains("spotify");
         NativeImage img = spotify ? this.cropSpotify(original) : original;
         this.roundCorners(img);
         this.artW = img.getWidth();
         this.artH = img.getHeight();
         if (this.textureCache.size() > 12) {
            this.textureCache.clear();
         }

         Identifier id = PizzaDLC.id("music/art_" + Integer.toHexString(hash));
         this.textureCache.put(hash, id);
         this.artworkId = id;
         mc.execute(() -> {
            try {
               mc.getTextureManager().register(id, new DynamicTexture(() -> "pizzadlc-music", img));
            } catch (Throwable var3x) {
               img.close();
            }
         });
      } catch (Throwable ignored) {
         this.session = null;
         this.artworkId = null;
      }
   }

   private NativeImage cropSpotify(NativeImage src) {
      int w = src.getWidth();
      int h = src.getHeight();
      int leftCut = (int)(w * 0.11);
      int rightCut = (int)(w * 0.11);
      int bottomCut = (int)(h * 0.22);
      int nw = w - leftCut - rightCut;
      int nh = h - bottomCut;
      if (nw > 0 && nh > 0) {
         NativeImage out = new NativeImage(nw, nh, false);

         for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
               out.setPixel(x, y, src.getPixel(x + leftCut, y));
            }
         }

         src.close();
         return out;
      }

      return src;
   }

   private void roundCorners(NativeImage img) {
      int w = img.getWidth();
      int h = img.getHeight();
      float r = Math.min(w, h) * 0.14F;
      if (!(r < 1.0F)) {
         float r2 = r * r;

         for (int y = 0; y < h; y++) {
            boolean yEdge = y < r || y >= h - r;
            if (yEdge) {
               float cy = y < r ? r : h - r;

               for (int x = 0; x < w; x++) {
                  if (x >= r && x < w - r) continue;

                  float cx = x < r ? r : w - r;
                  float dx = x - cx;
                  float dy = y - cy;
                  if (dx * dx + dy * dy > r2) {
                     img.setPixel(x, y, 0);
                  }
               }
            }
         }
      }
   }

   public boolean hasSession() {
      return this.session != null;
   }

   public IMediaSession getSession() {
      return this.session;
   }

   public Identifier getArtwork() {
      return this.artworkId;
   }

   public int getArtW() {
      return this.artW;
   }

   public int getArtH() {
      return this.artH;
   }

   private static String safe(String s) {
      return s == null ? "" : s;
   }
}
