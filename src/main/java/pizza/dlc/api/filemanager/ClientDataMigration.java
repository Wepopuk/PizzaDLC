package pizza.dlc.api.filemanager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Data-format compatibility only: the previous identifier is not used as branding. */
public final class ClientDataMigration {
   private static final String PREVIOUS_ID = "calcite";

   public static File directory(String id) {
      return migrate(Path.of(""), id).toFile();
   }

   public static Path migrate(Path workingDirectory, String id) {
      Path base = workingDirectory.toAbsolutePath().normalize();
      Path target = base.resolve(id).normalize();
      if (!target.getParent().equals(base)) throw new IllegalArgumentException("Invalid data directory");
      Path previous = base.resolve(PREVIOUS_ID);
      try {
         // A same-directory rename preserves checkpoints, encrypted accounts and all configs.
         if (!Files.exists(target) && Files.isDirectory(previous)) Files.move(previous, target);
         if (Files.isDirectory(target)) {
            try (var paths = Files.walk(target)) {
               for (Path file : paths.filter(Files::isRegularFile).toList()) {
                  String name = file.getFileName().toString();
                  String suffix = "." + PREVIOUS_ID;
                  if (name.endsWith(suffix)) {
                     Path renamed = file.resolveSibling(name.substring(0, name.length() - suffix.length()) + "." + id);
                     if (!Files.exists(renamed)) Files.move(file, renamed);
                  }
               }
            }
         }
         return target;
      } catch (IOException e) {
         throw new UncheckedIOException("Cannot migrate PizzaDLC data; original files have been preserved", e);
      }
   }

   /** Historical encryption passwords must remain byte-for-byte stable after a brand change. */
   public static String credentialKey(String store) {
      return PREVIOUS_ID + "-" + store + "|";
   }

   private ClientDataMigration() {}
}
