package pizza.dlc.api.account;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

public final class SkinHeads {
   private static final Map<UUID, Identifier> LOADED = new ConcurrentHashMap<>();
   private static final Map<UUID, Boolean> REQUESTED = new ConcurrentHashMap<>();

   private SkinHeads() {
   }

   public static Identifier offline(String name) {
      return DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name)).body().texturePath();
   }

   public static Identifier premium(UUID uuid) {
      Identifier loaded = LOADED.get(uuid);
      if (loaded != null) {
         return loaded;
      }

      if (REQUESTED.putIfAbsent(uuid, Boolean.TRUE) == null) {
         Minecraft mc = Minecraft.getInstance();
         CompletableFuture.supplyAsync(() -> mc.services().sessionService().fetchProfile(uuid, true), Util.nonCriticalIoPool())
            .thenComposeAsync(r -> r == null ? CompletableFuture.<Optional<PlayerSkin>>completedFuture(Optional.empty()) : mc.getSkinManager().get(r.profile()), mc)
            .thenAcceptAsync(skin -> skin.ifPresent(s -> LOADED.put(uuid, s.body().texturePath())), mc)
            .exceptionally(t -> null);
      }

      return DefaultPlayerSkin.get(uuid).body().texturePath();
   }

   public static Identifier of(String name, boolean premium) {
      if (premium) {
         MicrosoftAccountStore.Entry e = MicrosoftAccountStore.get().find(name);
         if (e != null && e.uuid() != null) {
            try {
               return premium(UUID.fromString(e.uuid()));
            } catch (IllegalArgumentException ignored) {
            }
         }
      }

      return offline(name);
   }

   public static Identifier current() {
      Minecraft mc = Minecraft.getInstance();
      UUID id = mc.getUser().getProfileId();
      String name = mc.getUser().getName();
      return id.equals(UUIDUtil.createOfflinePlayerUUID(name)) ? offline(name) : premium(id);
   }
}
