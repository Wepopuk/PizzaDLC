package pizza.dlc.client.modules.impl.visuals.animatedhands.lua;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class LuaRegistry {
   public static final class Items {
      private final Map<String, Item> cache = new HashMap<>();

      public Item get(String id) {
         return this.cache.computeIfAbsent(id, key -> {
            Identifier parsed = Identifier.tryParse(key);
            return parsed == null ? null : BuiltInRegistries.ITEM.getValue(parsed);
         });
      }

      public String checkItemName(ItemStack stack) {
         return stack.getCustomName() == null ? "" : stack.getCustomName().getString();
      }
   }

   public static final class Tags {
      private final Map<String, TagKey<Item>> vanilla = new HashMap<>();
      private final Map<String, TagKey<Item>> conventional = new HashMap<>();

      public TagKey<Item> getVanillaTag(String path) {
         return this.vanilla.computeIfAbsent(path, p -> TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(p)));
      }

      public TagKey<Item> getFabricTag(String path) {
         return this.conventional.computeIfAbsent(path, p -> TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", p)));
      }
   }

   public static final class Texture {
      private final Map<String, Identifier> cache = new HashMap<>();

      public Identifier of(String namespace, String path) {
         return this.cache.computeIfAbsent(namespace + ":" + path, key -> Identifier.fromNamespaceAndPath(namespace, path));
      }
   }

   private LuaRegistry() {
   }
}
