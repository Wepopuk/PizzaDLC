package pizza.dlc;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pizza.dlc.api.account.ServerAutoLogin;
import pizza.dlc.api.filemanager.impl.AltAccountManager;
import pizza.dlc.api.filemanager.impl.FriendManager;
import pizza.dlc.sound.PizzaDLCSounds;

public class PizzaDLC implements ModInitializer {
   public static final String CLIENT_NAME = "PizzaDLC";
   public static final String MOD_ID = "pizzadlc";
   public static final Logger LOGGER = LoggerFactory.getLogger("PizzaDLC");
   public static final File DIRECTORY = pizza.dlc.api.filemanager.ClientDataMigration.directory(MOD_ID);
   private static PizzaDLC instance;
   public final IEventBus bus = new EventBus();
   public FriendManager friendManager;
   public AltAccountManager altAccountManager;

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }

   public void onInitialize() {
      instance = this;
      if (!DIRECTORY.exists()) {
         DIRECTORY.mkdirs();
      }

      PizzaDLCSounds.init();
      this.bus.registerLambdaFactory("pizza.dlc", (lookupInMethod, klass) -> (Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
      this.bus.subscribe(this);
      this.friendManager = new FriendManager();
      this.altAccountManager = new AltAccountManager();
      ServerAutoLogin.init();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (this.friendManager != null) {
            this.friendManager.save();
         }

         if (this.altAccountManager != null) {
            this.altAccountManager.save();
         }
      }));
   }

   public IEventBus getBus() {
      return this.bus;
   }

   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   public AltAccountManager getAltAccountManager() {
      return this.altAccountManager;
   }

   public static PizzaDLC getInstance() {
      return instance;
   }
}
