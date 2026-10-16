package pizza.dlc.api.account;

import pizza.dlc.api.translation.Translations;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.ui.account.ServerAuthPromptScreen;

public final class ServerAutoLogin {
   private ClientPacketListener connection;
   private String account;
   private String server;
   private ServerAuthFlow flow;
   private ServerAuthFlow.Attempt offer;
   private boolean offerSave;
   private boolean proposed;
   private boolean sending;
   private long offeredAt;

   public static void init() {
      ServerAutoLogin login = new ServerAutoLogin();
      ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
         login.connection = handler;
         // The connected profile stays correct even when the menu changes the next session's account.
         login.account = handler.getLocalGameProfile().name();
         login.server = mc.getCurrentServer() == null ? "" : mc.getCurrentServer().ip;
         login.flow = new ServerAuthFlow();
         login.offer = null;
         login.proposed = false;
      });
      ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
         login.connection = null;
         login.account = null;
         login.server = null;
         login.flow = null;
         login.offer = null;
      });
      ClientReceiveMessageEvents.GAME.register((message, overlay) -> login.onMessage(message.getString()));
      ClientSendMessageEvents.COMMAND.register(command -> {
         if (login.connected() && !login.sending && ServerAuthFlow.parseManual(command) != null) {
            login.offer = null;
            login.flow.manual(command, System.currentTimeMillis());
         }
      });
      ClientTickEvents.END_CLIENT_TICK.register(mc -> login.tick(mc));
   }

   private boolean connected() {
      Minecraft mc = Minecraft.getInstance();
      return flow != null && connection != null && mc.getConnection() == connection && mc.player != null
         && !mc.hasSingleplayerServer() && !ServerLoginStore.normalizeAddress(server).isEmpty();
   }

   private void onMessage(String message) {
      if (!connected()) return;
      long now = System.currentTimeMillis();
      ServerLoginStore store = ServerLoginStore.get();
      ServerLoginStore.Entry saved = store.find(account, server);
      ServerLoginStore.Entry credential = store.resolveCredential(saved);
      ServerAuthFlow.Attempt success = flow.complete(message, now);
      reportFailure();
      if (success != null) {
         if (success.manual()) {
            if (credential == null || !credential.password().equals(success.password()) || !credential.command().equals(success.loginCommand())) {
               offer = success;
               offerSave = true;
               offeredAt = now;
            }
         } else {
            String command = success.kind() == ServerAuthFlow.Kind.REGISTER && saved != null ? saved.command() : success.loginCommand();
            if (saved != null && saved.useSharedPassword()) {
               if (store.defaultForAccount(account) != null) store.putSharedServer(account, server, command, saved.enabled());
            } else {
               store.put(account, server, success.password(), command, true);
            }
            NotificationManager.add(Translations.text("Аккаунты"), success.kind() == ServerAuthFlow.Kind.REGISTER ? Translations.text("Регистрация завершена · сервер сохранён") : Translations.text("Вход выполнен"), NotificationType.SUCCESS, 2500);
         }
         return;
      }
      ServerAuthFlow.Attempt corrected = flow.correctRegistrationFormat(message, connection.getCommands().getRoot(), now);
      if (corrected != null) { send(corrected); return; }
      ServerAuthFlow.Attempt automatic = flow.automatic(message, credential, now, connection.getCommands().getRoot());
      if (automatic != null) {
         send(automatic);
      } else if (saved == null && !proposed && offer == null) {
         // A shared password only produces a destination-specific approval prompt.
         ServerAuthFlow.Attempt candidate = flow.propose(message, store.defaultForAccount(account), now, connection.getCommands().getRoot());
         if (candidate != null) {
            proposed = true;
            offer = candidate;
            offerSave = false;
            offeredAt = now;
         }
      }
   }

   private void send(ServerAuthFlow.Attempt attempt) {
      sending = true;
      try {
         connection.sendCommand(attempt.wireCommand());
      } finally {
         sending = false;
      }
   }

   private void tick(Minecraft mc) {
      if (!connected()) { offer = null; return; }
      long now = System.currentTimeMillis();
      flow.expire(now);
      reportFailure();
      if (offer == null) return;
      if (now - offeredAt > 60000) { offer = null; return; }
      if (mc.gui.screen() != null) return;
      ServerAuthFlow.Attempt candidate = offer;
      offer = null;
      boolean saveOnly = offerSave;
      ClientPacketListener destination = connection;
      String targetAccount = account;
      String targetServer = ServerLoginStore.normalizeAddress(server);
      boolean register = candidate.kind() == ServerAuthFlow.Kind.REGISTER;
      mc.setScreenAndShow(new ServerAuthPromptScreen(targetAccount, targetServer,
         saveOnly ? Translations.text("Сохранить пароль сервера?") : register ? Translations.text("Регистрация на сервере") : Translations.text("Вход на новый сервер"),
         saveOnly ? Translations.text("Запомнить введённый пароль для следующих входов?") : Translations.text("Использовать общий пароль аккаунта на этом сервере?"),
         saveOnly ? Translations.text("Сохранить") : register ? Translations.text("Разрешить и создать") : Translations.text("Разрешить и войти"), () -> {
            if (!connected() || connection != destination || !targetAccount.equals(account)
               || !targetServer.equals(ServerLoginStore.normalizeAddress(server))) return;
            if (!saveOnly) {
               ServerLoginStore.Entry shared = ServerLoginStore.get().defaultForAccount(targetAccount);
               if (shared == null || !shared.enabled() || !shared.password().equals(candidate.password())) return;
               if (!flow.approve(candidate, System.currentTimeMillis())) return;
               ServerLoginStore.get().putSharedServer(targetAccount, targetServer, candidate.loginCommand(), true);
            } else {
               ServerLoginStore.get().put(targetAccount, targetServer, candidate.password(), candidate.loginCommand(), true);
            }
            if (saveOnly) {
               NotificationManager.add(Translations.text("Аккаунты"), Translations.text("Пароль сервера сохранён для ") + targetAccount, NotificationType.SUCCESS, 2500);
            } else {
               send(candidate);
            }
         }, () -> {}));
   }

   private void reportFailure() {
      ServerAuthFlow.Kind failure = flow.takeFailure();
      if (failure == null) return;
      offer = null;
      proposed = true;
      NotificationManager.add(Translations.text("Аккаунты"), failure == ServerAuthFlow.Kind.LOGIN
         ? Translations.text("Автовход не прошёл — войди вручную через /login")
         : Translations.text("Регистрация не прошла — зарегистрируйся вручную"), NotificationType.WARNING, 6000);
   }

   public static boolean isLoginPrompt(String message) {
      return ServerAuthFlow.isLoginPrompt(message);
   }
}
