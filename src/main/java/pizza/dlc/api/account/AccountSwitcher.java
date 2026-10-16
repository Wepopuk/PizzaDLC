package pizza.dlc.api.account;

import pizza.dlc.api.translation.Translations;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.Util;
import pizza.dlc.mixin.client.MinecraftSessionAccessor;

public final class AccountSwitcher {
   private static final long TOKEN_MARGIN_MS = 600000L;

   private AccountSwitcher() {
   }

   public static void applyOffline(String name) {
      Minecraft mc = Minecraft.getInstance();
      mc.user = new User(name, UUIDUtil.createOfflinePlayerUUID(name), "0", Optional.empty(), Optional.empty());
      mc.profileFuture = CompletableFuture.completedFuture(null);
      MinecraftSessionAccessor acc = (MinecraftSessionAccessor)mc;
      acc.pizzadlc$setUserApiService(UserApiService.OFFLINE);
      acc.pizzadlc$setUserPropertiesFuture(CompletableFuture.completedFuture(UserApiService.OFFLINE_PROPERTIES));
      acc.pizzadlc$setProfileKeyPairManager(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
   }

   public static CompletableFuture<MicrosoftAuth.Session> loginPremium(String name) {
      Minecraft mc = Minecraft.getInstance();
      return CompletableFuture.supplyAsync(() -> {
         MicrosoftAccountStore.Entry e = MicrosoftAccountStore.get().find(name);
         if (e == null || e.refreshToken() == null) {
            throw new CompletionException(new MicrosoftAuth.AuthException(Translations.text("Вход не сохранён — войди через Microsoft заново")));
         }

         try {
            MicrosoftAuth.Session s;
            if (e.accessToken() != null && e.expiresAt() - System.currentTimeMillis() > TOKEN_MARGIN_MS) {
               s = new MicrosoftAuth.Session(e.name(), UUID.fromString(e.uuid()), e.accessToken(), e.expiresAt(), e.refreshToken());
            } else {
               s = MicrosoftAuth.minecraftLogin(MicrosoftAuth.refresh(e.refreshToken()));
               MicrosoftAccountStore.get().put(s);
            }

            return prepare(mc, s);
         } catch (MicrosoftAuth.AuthException ex) {
            throw new CompletionException(ex);
         }
      }, Util.nonCriticalIoPool()).thenApplyAsync(p -> {
         apply(mc, p);
         return p.session();
      }, mc);
   }

   public static CompletableFuture<MicrosoftAuth.Session> applyPremium(MicrosoftAuth.Session session) {
      Minecraft mc = Minecraft.getInstance();
      return CompletableFuture.supplyAsync(() -> prepare(mc, session), Util.nonCriticalIoPool()).thenApplyAsync(p -> {
         apply(mc, p);
         return p.session();
      }, mc);
   }

   private static Prepared prepare(Minecraft mc, MicrosoftAuth.Session s) {
      UserApiService api;
      try {
         api = new YggdrasilAuthenticationService(mc.getProxy()).createUserApiService(s.accessToken());
      } catch (Exception ignored) {
         api = UserApiService.OFFLINE;
      }

      return new Prepared(s, api);
   }

   private static void apply(Minecraft mc, Prepared p) {
      MicrosoftAuth.Session s = p.session();
      UserApiService api = p.api();
      User user = new User(s.name(), s.uuid(), s.accessToken(), Optional.empty(), Optional.empty());
      mc.user = user;
      mc.profileFuture = CompletableFuture.supplyAsync(() -> mc.services().sessionService().fetchProfile(user.getProfileId(), true), Util.nonCriticalIoPool());
      MinecraftSessionAccessor acc = (MinecraftSessionAccessor)mc;
      acc.pizzadlc$setUserApiService(api);
      acc.pizzadlc$setUserPropertiesFuture(CompletableFuture.supplyAsync(() -> {
         try {
            return api.fetchProperties();
         } catch (AuthenticationException ignored) {
            return UserApiService.OFFLINE_PROPERTIES;
         }
      }, Util.nonCriticalIoPool()));
      acc.pizzadlc$setProfileKeyPairManager(ProfileKeyPairManager.create(api, user, mc.gameDirectory.toPath()));
   }

   public static String errorText(Throwable t) {
      Throwable c = t;

      while (c instanceof CompletionException && c.getCause() != null) {
         c = c.getCause();
      }

      return c instanceof MicrosoftAuth.AuthException ? c.getMessage() : Translations.text("Ошибка входа: ") + c.getClass().getSimpleName();
   }

   private record Prepared(MicrosoftAuth.Session session, UserApiService api) {
   }
}
