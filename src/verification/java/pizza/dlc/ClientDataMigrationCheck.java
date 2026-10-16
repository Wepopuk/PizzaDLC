package pizza.dlc;

import pizza.dlc.api.filemanager.ClientDataMigration;
import java.nio.file.Files;
import java.nio.file.Path;

/** Uses only temporary fixture data; never touches the user's actual client directory. */
public final class ClientDataMigrationCheck {
   public static void main(String[] args) throws Exception {
      Path base=Files.createTempDirectory("pizzadlc-migration-");
      String oldId=ClientDataMigration.credentialKey("test").split("-")[0];
      Path old=Files.createDirectories(base.resolve(oldId));
      Files.createDirectories(old.resolve("configs"));Files.createDirectories(old.resolve("neuro/models/test"));
      Files.writeString(old.resolve("configs/autosave."+oldId),"config-fixture");
      Files.writeString(old.resolve("macros."+oldId),"encrypted-macro-fixture");
      Files.writeString(old.resolve("neuro/models/test/current.json"),"model-fixture");
      Files.writeString(old.resolve("server-logins.json"),"encrypted-fixture");
      Path target=ClientDataMigration.migrate(base,"pizzadlc");
      check(!Files.exists(old)&&target.equals(base.resolve("pizzadlc")),"Data directory was not migrated");
      check(Files.readString(target.resolve("configs/autosave.pizzadlc")).equals("config-fixture"),"Config extension migration");
      check(Files.readString(target.resolve("macros.pizzadlc")).equals("encrypted-macro-fixture"),"Macro migration");
      check(Files.readString(target.resolve("neuro/models/test/current.json")).equals("model-fixture"),"Model bytes changed");
      check(Files.readString(target.resolve("server-logins.json")).equals("encrypted-fixture"),"Encrypted data changed");
      check(ClientDataMigration.migrate(base,"pizzadlc").equals(target),"Migration is not repeatable");
      Files.createDirectories(old);Files.writeString(old.resolve("server-logins.json"),"other-data");
      ClientDataMigration.migrate(base,"pizzadlc");
      check(Files.readString(target.resolve("server-logins.json")).equals("encrypted-fixture"),"Existing new data was overwritten");
      check(Files.exists(old.resolve("server-logins.json")),"Conflicting old data was removed");
      try {ClientDataMigration.migrate(base,"../outside");throw new AssertionError("Traversal accepted");}catch(IllegalArgumentException expected){}
      check(ClientDataMigration.credentialKey("msa").equals(oldId+"-msa|"),"Historical credential key changed");
      System.out.println("PASS: PizzaDLC data/config/macro/model migration, stable credential keys, idempotence, conflicts and path bounds");
   }
   private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
