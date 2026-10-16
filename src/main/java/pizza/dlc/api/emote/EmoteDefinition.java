package pizza.dlc.api.emote;

import java.util.function.Supplier;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public final class EmoteDefinition {
   private final String id;
   private final String displayName;
   private final Identifier icon;
   private final PlayerSkin skinOverride;
   private final Supplier<EmoteAnimation> loader;
   private EmoteAnimation cached;

   EmoteDefinition(String id, String displayName) {
      this(id, displayName, null);
   }

   EmoteDefinition(String id, String displayName, String skinPath) {
      this.id = id;
      this.displayName = displayName;
      this.icon = Identifier.fromNamespaceAndPath("pizzadlc", "textures/emotes/" + id + ".png");
      this.skinOverride = skinPath == null ? null : PlayerSkin.insecure(
         new ClientAsset.ResourceTexture(
            Identifier.fromNamespaceAndPath("pizzadlc", "emotes/skins/" + id),
            Identifier.fromNamespaceAndPath("pizzadlc", skinPath)
         ),
         null, null, PlayerModelType.WIDE
      );
      this.loader = () -> EmoteJsonLoader.load(id, displayName, "/assets/pizzadlc/emotes/" + id + ".json");
   }

   public String id() {
      return this.id;
   }

   public String displayName() {
      return this.displayName;
   }

   public Identifier icon() {
      return this.icon;
   }

   public PlayerSkin skinOverride() {
      return this.skinOverride;
   }

   public synchronized EmoteAnimation animation() {
      if (this.cached == null) {
         this.cached = this.loader.get();
      }

      return this.cached;
   }
}
