package pizza.dlc.api.resource;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import pizza.dlc.PizzaDLC;

/** Reloaded with the active resource packs, including F3+T and pack removal. */
public final class ArmorCitRegistry implements PreparableReloadListener {
    public static final ArmorCitRegistry INSTANCE = new ArmorCitRegistry();
    private volatile Map<String, List<ArmorCitRule>> rules = Map.of();

    @Override
    public CompletableFuture<Void> reload(SharedState state, Executor prepare, PreparationBarrier barrier, Executor apply) {
        return CompletableFuture.supplyAsync(() -> load(state.resourceManager()), prepare)
            .thenCompose(barrier::wait).thenAcceptAsync(this::install, apply);
    }

    public void install(List<ArmorCitRule> loaded) {
        Map<String, List<ArmorCitRule>> index = new HashMap<>();
        for (ArmorCitRule rule : loaded) {
            for (String item : rule.items()) index.computeIfAbsent(item, k -> new ArrayList<>()).add(rule);
        }
        index.replaceAll((item, entries) -> List.copyOf(entries));
        rules = Map.copyOf(index);
    }

    public static List<ArmorCitRule> load(ResourceManager resources) {
        Map<String, Integer> priorities = new HashMap<>();
        List<PackResources> packs = resources.listPacks().toList();
        for (int i = 0; i < packs.size(); i++) priorities.put(packs.get(i).packId(), i);
        List<ArmorCitRule> result = new ArrayList<>();
        for (String prefix : List.of("mcpatcher/cit", "optifine/cit")) {
            for (var entry : resources.listResources(prefix, id -> id.getPath().endsWith(".properties")).entrySet()) {
                if (prefix.equals("mcpatcher/cit")) {
                    Identifier mirrored = Identifier.fromNamespaceAndPath(entry.getKey().getNamespace(),
                        entry.getKey().getPath().replaceFirst("^mcpatcher/cit/", "optifine/cit/"));
                    if (resources.getResource(mirrored).filter(r -> priorities.getOrDefault(r.sourcePackId(), 0)
                        >= priorities.getOrDefault(entry.getValue().sourcePackId(), 0)).isPresent()) continue;
                }
                try (var stream = entry.getValue().open()) {
                    Properties p = new Properties();
                    p.load(stream);
                    ArmorCitRule rule = ArmorCitRule.parse(entry.getKey(), p,
                        priorities.getOrDefault(entry.getValue().sourcePackId(), 0));
                    if (rule != null && rule.textures().values().stream().allMatch(id -> resources.getResource(id).isPresent())) result.add(rule);
                } catch (IOException | IllegalArgumentException ex) {
                    PizzaDLC.LOGGER.warn("Skipping invalid armor CIT rule {}: {}", entry.getKey(), ex.getMessage());
                }
            }
        }
        result.sort(Comparator.comparingInt(ArmorCitRule::packPriority).reversed()
            .thenComparing(Comparator.comparingInt(ArmorCitRule::weight).reversed())
            .thenComparing(rule -> rule.source().toString()));
        return List.copyOf(result);
    }

    public Identifier texture(ItemStack stack, LayerType layer, Identifier original) {
        if (stack.isEmpty() || (layer != LayerType.HUMANOID && layer != LayerType.HUMANOID_LEGGINGS)) return original;
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!rules.containsKey(item)) return original;
        Map<String, Integer> enchants = new HashMap<>();
        for (var entry : stack.getEnchantments().entrySet()) {
            entry.getKey().unwrapKey().ifPresent(key -> enchants.put(key.identifier().toString(), entry.getIntValue()));
        }
        String path = original.getPath();
        String material = path.substring(path.lastIndexOf('/') + 1).replaceFirst("\\.png$", "");
        boolean overlay = material.endsWith("_overlay");
        if (overlay) material = material.substring(0, material.length() - "_overlay".length());
        return select(item, stack.getDamageValue(), stack.getMaxDamage(), enchants,
            material + (layer == LayerType.HUMANOID_LEGGINGS ? "_layer_2" : "_layer_1") + (overlay ? "_overlay" : ""), original);
    }

    public Identifier select(String item, int damage, int maxDamage, Map<String, Integer> enchants,
                             String legacyLayer, Identifier original) {
        for (ArmorCitRule rule : rules.getOrDefault(item, List.of())) {
            if (rule.matches(item, damage, maxDamage, enchants)) {
                Identifier texture = rule.textureFor(legacyLayer);
                if (texture != null) return texture;
            }
        }
        return original;
    }
}
