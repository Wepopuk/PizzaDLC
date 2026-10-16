package pizza.dlc.client.modules.impl.visuals;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.color.ColorRGBA;
import pizza.dlc.api.util.render.AdditiveTextureType;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Particles",
   category = ModuleCategory.VISUALS
)
public class Particles extends Module {
   private static final Identifier TEX_BLOOM = Identifier.fromNamespaceAndPath("pizzadlc", "textures/images/particles/bloom.png");
   private static final Identifier TEX_STAR = Identifier.fromNamespaceAndPath("pizzadlc", "textures/images/particles/star.png");
   private static final String AROUND_PLAYER = "Вокруг игрока";
   private static final String ON_WALK = "При ходьбе";
   private static final String ON_HIT = "При ударе";
   private static final String TOTEM_POP = "Тотем";
   private static final String BLOOM = "Bloom";
   private static final String STAR = "Star";
   private static final String GLOW_NORMAL = "Обычное";
   private static final String GLOW_ADDITIVE = "Аддитивное";
   private static final float LAYER_TWIST = 41.0F;
   private static final String COLOR_THEME = "Тема";
   private static final String COLOR_RAINBOW = "Радуга";
   private static final String COLOR_ACCENT = "Акцент";
   private static final ThreadLocal<Dir3> DIR = ThreadLocal.withInitial(Dir3::new);
   private static final String G_PARTICLES = "Партиклы";
   private static final String G_PHYSICS = "Физика";
   private static final String G_HIT = "Удар";
   private static final String G_WALK = "Бег";
   private static final String G_AROUND = "Вокруг";
   public final ModeSetting texture = new ModeSetting("Текстура", this, BLOOM, STAR).group(G_PARTICLES);
   public final MultiModeSetting spawn = new MultiModeSetting("Спавн", this, AROUND_PLAYER, ON_WALK, ON_HIT, TOTEM_POP).group(G_PARTICLES);
   public final ModeSetting glow = new ModeSetting("Свечение", this, GLOW_NORMAL, GLOW_ADDITIVE).group(G_PARTICLES);
   public final SliderSetting glowLayers = new SliderSetting("Слои", this, 2.0F, 1.0F, 3.0F, 1.0F)
      .<Setting>setVisible(() -> this.glow.is(GLOW_ADDITIVE))
      .group(G_PARTICLES);
   public final ModeSetting colorMode = new ModeSetting("Цвет", this, COLOR_THEME, COLOR_RAINBOW, COLOR_ACCENT).group(G_PARTICLES);
   public final SliderSetting colorSpread = new SliderSetting("Разброс цвета", this, 1.0F, 0.0F, 1.0F, 0.05F)
      .<Setting>setVisible(() -> !this.colorMode.is(COLOR_ACCENT))
      .group(G_PARTICLES);
   public final SliderSetting maxParticles = new SliderSetting("Лимит частиц", this, 180.0F, 40.0F, 500.0F, 10.0F).group(G_PARTICLES);
   public final SliderSetting maxDistance = new SliderSetting("Дистанция", this, 64.0F, 8.0F, 192.0F, 1.0F).group(G_PARTICLES);
   public final SliderSetting size = new SliderSetting("Размер", this, 4.0F, 1.0F, 10.0F, 0.25F).group(G_PHYSICS);
   public final SliderSetting sizeRandom = new SliderSetting("Разброс размера", this, 1.5F, 0.0F, 5.0F, 0.25F).group(G_PHYSICS);
   public final SliderSetting lifeMs = new SliderSetting("Время жизни", this, 850.0F, 150.0F, 2500.0F, 25.0F).group(G_PHYSICS);
   public final SliderSetting speed = new SliderSetting("Скорость", this, 0.085F, 0.01F, 0.28F, 0.005F).group(G_PHYSICS);
   public final SliderSetting gravity = new SliderSetting("Гравитация", this, 0.006F, 0.0F, 0.04F, 0.001F).group(G_PHYSICS);
   public final SliderSetting drag = new SliderSetting("Сопротивление", this, 0.93F, 0.75F, 1.0F, 0.01F).group(G_PHYSICS);
   public final SliderSetting rotationSpeed = new SliderSetting("Скорость вращения", this, 120.0F, 0.0F, 720.0F, 5.0F).group(G_PHYSICS);
   public final SliderSetting hitCount = new SliderSetting("Количество", this, 10.0F, 0.0F, 45.0F, 1.0F)
      .<Setting>setVisible(() -> this.spawn.get(ON_HIT).isEnabled())
      .group(G_HIT);
   public final SliderSetting walkEvery = new SliderSetting("Шаг", this, 0.28F, 0.08F, 1.5F, 0.02F)
      .<Setting>setVisible(() -> this.spawn.get(ON_WALK).isEnabled())
      .group(G_WALK);
   public final SliderSetting aroundCount = new SliderSetting("Количество", this, 45.0F, 0.0F, 160.0F, 5.0F)
      .<Setting>setVisible(() -> this.spawn.get(AROUND_PLAYER).isEnabled())
      .group(G_AROUND);
   public final SliderSetting aroundRadius = new SliderSetting("Радиус", this, 4.5F, 1.0F, 12.0F, 0.5F)
      .<Setting>setVisible(() -> this.spawn.get(AROUND_PLAYER).isEnabled())
      .group(G_AROUND);
   public final SliderSetting totemCount = new SliderSetting("Количество", this, 28.0F, 0.0F, 100.0F, 2.0F)
      .<Setting>setVisible(() -> this.spawn.get(TOTEM_POP).isEnabled())
      .group(TOTEM_POP);
   private int count;
   private float[] x = new float[0];
   private float[] y = new float[0];
   private float[] z = new float[0];
   private float[] prevX = new float[0];
   private float[] prevY = new float[0];
   private float[] prevZ = new float[0];
   private float[] velocityX = new float[0];
   private float[] velocityY = new float[0];
   private float[] velocityZ = new float[0];
   private float[] sizeArr = new float[0];
   private float[] rotationArr = new float[0];
   private float[] rotationSpeedArr = new float[0];
   private int[] colorArr = new int[0];
   private boolean[] ambientArr = new boolean[0];
   private long[] startMsArr = new long[0];
   private long[] lifeMsArr = new long[0];
   private boolean hasLastWalkPos;
   private double lastWalkX;
   private double lastWalkZ;
   private float walkCarry;

   public Particles() {
      this.spawn.get(ON_HIT).setEnabled(true);
      this.spawn.get(TOTEM_POP).setEnabled(true);
   }

   @Override
   public void onEnable() {
      this.reset();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.reset();
      super.onDisable();
   }

   @EventHandler
   public void onTick(EventTick event) {
      if (mc.level != null && mc.player != null) {
         long now = System.currentTimeMillis();
         boolean aroundEnabled = this.spawn.get(AROUND_PLAYER).isEnabled();
         this.updateParticles(now, aroundEnabled);
         double playerX = mc.player.getX();
         double playerY = mc.player.getY();
         double playerZ = mc.player.getZ();
         float playerHeight = mc.player.getBbHeight();
         if (aroundEnabled) {
            this.maintainAmbient(now, playerX, playerY, playerZ, playerHeight);
         }

         if (this.spawn.get(ON_WALK).isEnabled()) {
            this.spawnWalk(playerX, playerY, playerZ);
         } else {
            this.hasLastWalkPos = false;
            this.walkCarry = 0.0F;
         }

         this.enforceCap();
      } else {
         this.reset();
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send event) {
      if (!this.spawn.get(ON_HIT).isEnabled() || !(event.getPacket() instanceof ServerboundAttackPacket packet)) return;

      int spawnCount = Math.round(this.hitCount.getValue());
      if (spawnCount > 0) {
         int entityId = packet.entityId();
         mc.execute(() -> {
            if (this.isEnabled() && mc.level != null && mc.player != null) {
               Entity target = mc.level.getEntity(entityId);
               if (target != null && this.withinDistance(target)) {
                  Vec3 pos = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
                  this.spawnBurst(pos, spawnCount, 1.2F);
               }
            }
         });
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!this.spawn.get(TOTEM_POP).isEnabled() || !(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;

      if (packet.getEventId() == 35) {
         int spawnCount = Math.round(this.totemCount.getValue());
         if (spawnCount > 0) {
            mc.execute(() -> {
               if (this.isEnabled() && mc.level != null) {
                  Entity entity = packet.getEntity(mc.level);
                  if (entity != null && (mc.player == null || this.withinDistance(entity))) {
                     Vec3 pos = entity.position().add(0.0, entity.getBbHeight() * 0.6, 0.0);
                     this.spawnBurst(pos, spawnCount, 1.65F);
                  }
               }
            });
         }
      }
   }

   @EventHandler
   public void onRender3D(EventRender3D event) {
      if (this.count > 0 && mc.player != null) {
         long now = System.currentTimeMillis();
         float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         double cameraX = event.getCamera().pos.x;
         double cameraY = event.getCamera().pos.y;
         double cameraZ = event.getCamera().pos.z;
         double maxDist = this.maxDistance.getValue();
         double maxDistSq = maxDist * maxDist;
         Identifier tex = this.texture.is(STAR) ? TEX_STAR : TEX_BLOOM;
         boolean additive = this.glow.is(GLOW_ADDITIVE);
         RenderType renderType = additive ? AdditiveTextureType.image(tex) : RenderTypes.textSeeThrough(tex);
         int layers = additive ? Math.max(1, Math.round(this.glowLayers.getValue())) : 1;
         Render3DUtil.drawTexturedBillboards(event, renderType, w -> {
            for (int i = 0; i < this.count; i++) {
               float progress = (float)(now - this.startMsArr[i]) / Math.max(1.0F, (float)this.lifeMsArr[i]);
               float alpha = 1.0F - Mth.clamp(progress, 0.0F, 1.0F);
               if (!(alpha <= 0.01F)) {
                  double renderX = this.prevX[i] + (this.x[i] - this.prevX[i]) * pt;
                  double renderY = this.prevY[i] + (this.y[i] - this.prevY[i]) * pt;
                  double renderZ = this.prevZ[i] + (this.z[i] - this.prevZ[i]) * pt;
                  double dx = renderX - cameraX;
                  double dy = renderY - cameraY;
                  double dz = renderZ - cameraZ;
                  if (!(dx * dx + dy * dy + dz * dz > maxDistSq)) {
                     float drawSize = Math.max(0.01F, this.sizeArr[i] * 0.0125F);
                     float rotation = this.rotationArr[i] + (float)(now - this.startMsArr[i]) * this.rotationSpeedArr[i] / 1000.0F;
                     int argb = mulAlpha(this.colorArr[i], alpha);

                     for (int layer = 0; layer < layers; layer++) {
                        w.quad(renderX, renderY, renderZ, drawSize, rotation + layer * LAYER_TWIST, argb);
                     }
                  }
               }
            }
         });
      }
   }

   private void updateParticles(long now, boolean aroundEnabled) {
      float gravityValue = this.gravity.getValue();
      float dragValue = this.drag.getValue();
      boolean hasPlayer = mc.player != null;
      double playerX = 0.0;
      double playerY = 0.0;
      double playerZ = 0.0;
      float playerHeight = 0.0F;
      double ambientRadiusSq = 0.0;
      double ambientMinY = 0.0;
      double ambientMaxY = 0.0;
      if (hasPlayer) {
         playerX = mc.player.getX();
         playerY = mc.player.getY();
         playerZ = mc.player.getZ();
         playerHeight = mc.player.getBbHeight();
         double radius = this.aroundRadius.getValue();
         ambientRadiusSq = radius * radius * 1.7;
         ambientMinY = playerY - 1.2;
         ambientMaxY = playerY + playerHeight + 4.5;
      }

      for (int i = 0; i < this.count; i++) {
         if (now - this.startMsArr[i] < this.lifeMsArr[i] && (!this.ambientArr[i] || aroundEnabled)) {
            this.prevX[i] = this.x[i];
            this.prevY[i] = this.y[i];
            this.prevZ[i] = this.z[i];
            this.velocityX[i] = this.velocityX[i] * dragValue;
            this.velocityY[i] = this.velocityY[i] * dragValue - gravityValue;
            this.velocityZ[i] = this.velocityZ[i] * dragValue;
            this.x[i] = this.x[i] + this.velocityX[i];
            this.y[i] = this.y[i] + this.velocityY[i];
            this.z[i] = this.z[i] + this.velocityZ[i];
            if (this.ambientArr[i] && hasPlayer) {
               double dx = this.x[i] - playerX;
               double dz = this.z[i] - playerZ;
               if (dx * dx + dz * dz > ambientRadiusSq || this.y[i] < ambientMinY || this.y[i] > ambientMaxY) {
                  this.respawnAmbient(i, now, playerX, playerY, playerZ, playerHeight);
               }
            }
         } else {
            this.removeAt(i--);
         }
      }
   }

   private void maintainAmbient(long now, double playerX, double playerY, double playerZ, float playerHeight) {
      int desired = Math.min(Math.round(this.aroundCount.getValue()), Math.round(this.maxParticles.getValue()));
      int ambient = 0;

      for (int i = 0; i < this.count; i++) {
         if (!this.ambientArr[i]) continue;

         ambient++;
      }

      while (ambient < desired) {
         this.spawnAmbient(now, playerX, playerY, playerZ, playerHeight);
         ambient++;
      }

      for (int ix = 0; ambient > desired && ix < this.count; ix++) {
         if (!this.ambientArr[ix]) continue;

         this.removeAt(ix--);
         ambient--;
      }
   }

   private void spawnWalk(double playerX, double playerY, double playerZ) {
      if (!this.hasLastWalkPos) {
         this.lastWalkX = playerX;
         this.lastWalkZ = playerZ;
         this.hasLastWalkPos = true;
      } else {
         double dx = playerX - this.lastWalkX;
         double dz = playerZ - this.lastWalkZ;
         this.lastWalkX = playerX;
         this.lastWalkZ = playerZ;
         float dist = (float)Math.sqrt(dx * dx + dz * dz);
         if (!(dist <= 0.001F)) {
            this.walkCarry += dist;
            float every = Math.max(0.02F, this.walkEvery.getValue());

            while (this.walkCarry >= every) {
               this.walkCarry -= every;
               this.spawnOne(
                  (float)(playerX + randomRange(-0.22, 0.22)),
                  (float)(playerY + 0.08),
                  (float)(playerZ + randomRange(-0.22, 0.22)),
                  (float)randomRange(-0.015, 0.015),
                  (float)randomRange(0.018, 0.052),
                  (float)randomRange(-0.015, 0.015),
                  false,
                  0.55F
               );
            }
         }
      }
   }

   private void spawnBurst(Vec3 pos, int amount, float speedMul) {
      for (int i = 0; i < amount; i++) {
         this.spawnOne((float)pos.x, (float)pos.y, (float)pos.z, 0.0F, 0.02F, 0.0F, false, speedMul);
      }
   }

   private void spawnAmbient(long now, double playerX, double playerY, double playerZ, float playerHeight) {
      double radius = this.aroundRadius.getValue();
      int index = this.spawnOne(
         (float)(playerX + randomRange(-radius, radius)),
         (float)(playerY + randomRange(playerHeight * 0.6, playerHeight + 3.7)),
         (float)(playerZ + randomRange(-radius, radius)),
         (float)randomRange(-0.008, 0.008),
         (float)(-randomRange(0.004, 0.016)),
         (float)randomRange(-0.008, 0.008),
         true,
         0.18F
      );
      if (index >= 0) {
         this.startMsArr[index] = now;
      }
   }

   private void respawnAmbient(int index, long now, double playerX, double playerY, double playerZ, float playerHeight) {
      double radius = this.aroundRadius.getValue();
      this.x[index] = (float)(playerX + randomRange(-radius, radius));
      this.y[index] = (float)(playerY + randomRange(playerHeight * 0.6, playerHeight + 3.7));
      this.z[index] = (float)(playerZ + randomRange(-radius, radius));
      this.prevX[index] = this.x[index];
      this.prevY[index] = this.y[index];
      this.prevZ[index] = this.z[index];
      this.velocityX[index] = (float)randomRange(-0.008, 0.008);
      this.velocityY[index] = (float)(-randomRange(0.004, 0.016));
      this.velocityZ[index] = (float)randomRange(-0.008, 0.008);
      this.rotationArr[index] = (float)randomRange(0.0, 360.0);
      this.rotationSpeedArr[index] = (float)randomRange(-this.rotationSpeed.getValue(), this.rotationSpeed.getValue());
      this.colorArr[index] = this.rollColor();
      this.startMsArr[index] = now;
      this.lifeMsArr[index] = Math.max(1, Math.round(this.lifeMs.getValue()));
   }

   private int rollColor() {
      if (this.colorMode.is(COLOR_ACCENT)) {
         return ThemeManager.accent() | 0xFF000000;
      }

      float spread = Mth.clamp(this.colorSpread.getValue(), 0.0F, 1.0F);
      float offset = ((float)randomRange(0.0, 1.0) - 0.5F) * spread;
      if (this.colorMode.is(COLOR_RAINBOW)) {
         float hue = new ColorRGBA(ThemeManager.accent()).getHue() + offset;
         return ColorRGBA.fromHSB(hue - (float)Math.floor(hue), 0.72F, 1.0F).getRGB() | 0xFF000000;
      }

      return Render2DUtil.lerpColor(ThemeManager.accent(), ThemeManager.secondary(), 0.5F + offset) | 0xFF000000;
   }

   private int spawnOne(float baseX, float baseY, float baseZ, float extraVx, float extraVy, float extraVz, boolean ambient, float speedMul) {
      int cap = Math.max(0, Math.round(this.maxParticles.getValue()));
      if (cap <= 0) {
         return -1;
      }

      if (this.count >= cap) {
         this.removeAt(0);
      }

      this.ensureCapacity(this.count + 1);
      Dir3 dir = DIR.get();
      randomUnit(dir);
      float baseSpeed = this.speed.getValue() * speedMul;
      float randomSize = this.sizeRandom.getValue();
      int index = this.count++;
      this.x[index] = baseX;
      this.y[index] = baseY;
      this.z[index] = baseZ;
      this.prevX[index] = baseX;
      this.prevY[index] = baseY;
      this.prevZ[index] = baseZ;
      this.velocityX[index] = (float)dir.x * baseSpeed + extraVx;
      this.velocityY[index] = (float)dir.y * baseSpeed + extraVy;
      this.velocityZ[index] = (float)dir.z * baseSpeed + extraVz;
      this.sizeArr[index] = Math.max(1.0F, this.size.getValue() + (float)randomRange(-randomSize, randomSize));
      this.rotationArr[index] = (float)randomRange(0.0, 360.0);
      this.rotationSpeedArr[index] = (float)randomRange(-this.rotationSpeed.getValue(), this.rotationSpeed.getValue());
      this.colorArr[index] = this.rollColor();
      this.ambientArr[index] = ambient;
      this.startMsArr[index] = System.currentTimeMillis();
      this.lifeMsArr[index] = Math.max(1, Math.round(this.lifeMs.getValue()));
      return index;
   }

   private boolean withinDistance(Entity entity) {
      if (mc.player == null) {
         return false;
      }

      double maxDist = this.maxDistance.getValue();
      return mc.player.distanceToSqr(entity) <= maxDist * maxDist;
   }

   private void enforceCap() {
      int cap = Math.max(0, Math.round(this.maxParticles.getValue()));

      while (this.count > cap) {
         this.removeAt(0);
      }
   }

   private void ensureCapacity(int needed) {
      int current = this.x.length;
      if (current < needed) {
         int next = Math.max(64, current == 0 ? 64 : current * 2);

         while (next < needed) {
            next *= 2;
         }

         this.x = Arrays.copyOf(this.x, next);
         this.y = Arrays.copyOf(this.y, next);
         this.z = Arrays.copyOf(this.z, next);
         this.prevX = Arrays.copyOf(this.prevX, next);
         this.prevY = Arrays.copyOf(this.prevY, next);
         this.prevZ = Arrays.copyOf(this.prevZ, next);
         this.velocityX = Arrays.copyOf(this.velocityX, next);
         this.velocityY = Arrays.copyOf(this.velocityY, next);
         this.velocityZ = Arrays.copyOf(this.velocityZ, next);
         this.sizeArr = Arrays.copyOf(this.sizeArr, next);
         this.rotationArr = Arrays.copyOf(this.rotationArr, next);
         this.rotationSpeedArr = Arrays.copyOf(this.rotationSpeedArr, next);
         this.colorArr = Arrays.copyOf(this.colorArr, next);
         this.ambientArr = Arrays.copyOf(this.ambientArr, next);
         this.startMsArr = Arrays.copyOf(this.startMsArr, next);
         this.lifeMsArr = Arrays.copyOf(this.lifeMsArr, next);
      }
   }

   private void removeAt(int index) {
      int last = this.count - 1;
      if (index >= 0 && index <= last) {
         if (index != last) {
            this.x[index] = this.x[last];
            this.y[index] = this.y[last];
            this.z[index] = this.z[last];
            this.prevX[index] = this.prevX[last];
            this.prevY[index] = this.prevY[last];
            this.prevZ[index] = this.prevZ[last];
            this.velocityX[index] = this.velocityX[last];
            this.velocityY[index] = this.velocityY[last];
            this.velocityZ[index] = this.velocityZ[last];
            this.sizeArr[index] = this.sizeArr[last];
            this.rotationArr[index] = this.rotationArr[last];
            this.rotationSpeedArr[index] = this.rotationSpeedArr[last];
            this.colorArr[index] = this.colorArr[last];
            this.ambientArr[index] = this.ambientArr[last];
            this.startMsArr[index] = this.startMsArr[last];
            this.lifeMsArr[index] = this.lifeMsArr[last];
         }

         this.count--;
      }
   }

   private void reset() {
      this.count = 0;
      this.hasLastWalkPos = false;
      this.walkCarry = 0.0F;
   }

   private static void randomUnit(Dir3 out) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      double rx = random.nextDouble(-1.0, 1.0);
      double ry = random.nextDouble(-1.0, 1.0);
      double rz = random.nextDouble(-1.0, 1.0);
      double len = Math.sqrt(rx * rx + ry * ry + rz * rz);
      if (len < 1.0E-6) {
         out.x = 0.0;
         out.y = 1.0;
         out.z = 0.0;
      } else {
         out.x = rx / len;
         out.y = ry / len;
         out.z = rz / len;
      }
   }

   private static double randomRange(double min, double max) {
      return max <= min ? min : ThreadLocalRandom.current().nextDouble(min, max);
   }

   private static int mulAlpha(int argb, float alpha01) {
      int alpha = argb >>> 24 & 0xFF;
      int newAlpha = Mth.clamp(Math.round(alpha * Mth.clamp(alpha01, 0.0F, 1.0F)), 0, 255);
      return newAlpha << 24 | argb & 0xFFFFFF;
   }

   private static final class Dir3 {
      double x;
      double y;
      double z;
   }
}
