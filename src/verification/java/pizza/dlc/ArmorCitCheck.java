package pizza.dlc;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.metadata.pack.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import pizza.dlc.api.resource.*;

/** Actual ZIP loading, enchantment/percentage boundaries, pack priority and armor render hook. */
public final class ArmorCitCheck {
    private static final Identifier BASE = Identifier.parse("minecraft:textures/entity/equipment/humanoid/diamond.png");

    public static void main(String[] args) throws Exception {
        Properties p = properties("type=armor\nitems=diamond_helmet\ntexture.diamond_layer_1=skin\ndamage=50-59%\n");
        var rule = ArmorCitRule.parse(Identifier.parse("minecraft:optifine/cit/test/rule.properties"), p, 0);
        check(rule.textureFor("diamond_layer_1").equals(Identifier.parse("minecraft:optifine/cit/test/skin.png")), "Relative texture path");
        check(rule.matches("minecraft:diamond_helmet", 50, 100, Map.of()), "Range start");
        check(rule.matches("minecraft:diamond_helmet", 59, 100, Map.of()), "Range end");
        check(!rule.matches("minecraft:diamond_helmet", 49, 100, Map.of()), "Below range");
        check(!rule.matches("minecraft:diamond_helmet", 60, 100, Map.of()), "Above range");
        check(!rule.matches("minecraft:diamond_helmet", 50, 0, Map.of()), "Non-damageable item");
        check(!rule.matches("minecraft:iron_helmet", 50, 100, Map.of()), "Wrong material");
        p.setProperty("nbt.display.Name", "Coffee");
        check(ArmorCitRule.parse(rule.source(), p, 0) == null, "Unsupported name condition matched unconditionally");
        p.remove("nbt.display.Name");
        p.remove("damage");
        p.setProperty("enchantments", "protection");
        p.setProperty("enchantmentLevels", "4");
        rule = ArmorCitRule.parse(rule.source(), p, 0);
        check(rule.matches("minecraft:diamond_helmet", 0, 363, Map.of("minecraft:protection", 4)), "Protection IV");
        check(!rule.matches("minecraft:diamond_helmet", 0, 363, Map.of("minecraft:protection", 3, "minecraft:unbreaking", 4)), "Unrelated enchantment level matched");
        check(!rule.matches("minecraft:diamond_helmet", 0, 363, Map.of("minecraft:fire_protection", 4)), "Wrong enchantment");
        check(ArmorCitRule.IntRanges.parse("-3 5 7-9 12-").contains(999), "Open upper range");
        check(!ArmorCitRule.IntRanges.parse("-3 5 7-9 12-").contains(6), "Disjoint range gap");
        checkHook();

        Path coffee = Path.of("run/resourcepacks/Coffee-1.20-26.2.zip");
        if (Files.exists(coffee)) checkCoffee(coffee);
        else System.out.println("Coffee integration skipped: converted local pack is absent");
        System.out.println("Armor CIT verification passed");
    }

    private static void checkCoffee(Path coffee) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        net.minecraft.client.renderer.item.ItemModels.bootstrap();
        net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties.bootstrap();
        net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties.bootstrap();
        var registry = ArmorCitRegistry.INSTANCE;
        try (var resources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(open(coffee, "coffee")))) {
            var metadata = resources.listPacks().findFirst().orElseThrow().getMetadataSection(PackMetadataSection.CLIENT_TYPE);
            check(metadata != null && metadata.supportedFormats().isValueInRange(new PackFormat(88, 0)), "26.2 rejected pack metadata");
            check(metadata.supportedFormats().isValueInRange(new PackFormat(15, 0)), "Lost 1.20 metadata");
            for (String item : List.of("shield", "end_crystal")) {
                String definition = new String(read(resources, Identifier.parse("minecraft:items/" + item + ".json")), StandardCharsets.UTF_8);
                net.minecraft.client.renderer.item.ClientItem.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(definition)).getOrThrow();
            }
            var loaded = ArmorCitRegistry.load(resources);
            check(loaded.size() == 15, "Missing/duplicate Coffee armor rules: " + loaded.size());
            registry.install(loaded);
            for (int damage = 0; damage <= 100; damage++) {
                var selected = registry.select("minecraft:diamond_helmet", damage, 100, Map.of(), "diamond_layer_1", BASE);
                int indicator = damage >= 99 ? 1 : damage == 0 ? 0 : damage < 10 ? 99 : 100 - damage / 10 * 10;
                if (indicator == 0) check(selected.equals(BASE), "Undamaged helmet must retain base skin");
                else check(selected.getPath().endsWith("/" + indicator + "/" + indicator + ".png"), "Incorrect durability indicator at " + damage + "%: " + selected);
                var protectedSkin = registry.select("minecraft:diamond_helmet", damage, 100,
                    Map.of("minecraft:protection", 4), "diamond_layer_1", BASE);
                check(protectedSkin.getPath().contains("d_helmet_prot_4"), "Protection IV lost to durability rule");
            }
            // Real diamond helmet durability uses integer percentage rounding.
            for (int damage = 0; damage <= 363; damage++) {
                check(registry.select("minecraft:diamond_helmet", damage, 363, Map.of(), "diamond_layer_1", BASE)
                    .equals(registry.select("minecraft:diamond_helmet", damage * 100 / 363, 100, Map.of(), "diamond_layer_1", BASE)), "Percent rounding");
            }
            for (String piece : List.of("boots", "chestplate", "helmet", "leggings")) {
                String layer = piece.equals("leggings") ? "diamond_layer_2" : "diamond_layer_1";
                Identifier texture = registry.select("minecraft:diamond_" + piece, 0, 400,
                    Map.of("minecraft:protection", 4), layer, BASE);
                check(texture.getPath().contains("d_" + piece + "_prot_4"), "Missing armor slot " + piece);
                check(resources.getResource(texture).isPresent(), "Selected PNG is not loadable");
                check(!Arrays.equals(read(resources, texture), read(resources, BASE)), "Conditional skin is identical to ordinary armor");
            }
            check(resources.getResource(Identifier.parse("minecraft:textures/gui/sprites/hud/crosshair.png")).isPresent(), "Missing split HUD");
            check(resources.getResource(Identifier.parse("minecraft:textures/block/red_bed_head_up.png")).isPresent(), "Missing 26.2 bed");
            for (String material : List.of("diamond", "iron", "netherite", "gold", "leather", "chainmail")) {
                for (String folder : List.of("humanoid", "humanoid_leggings")) {
                    check(resources.getResource(Identifier.parse("minecraft:textures/entity/equipment/" + folder + "/" + material + ".png")).isPresent(), "Missing equipment " + material);
                }
            }
        }
        Path override = Path.of("build/coffee-check/override.zip");
        Files.createDirectories(override.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(override))) {
            put(zip, "assets/minecraft/optifine/cit/override/rule.properties", "type=armor\nitems=diamond_helmet\nweight=-100\ntexture.diamond_layer_1=skin\n");
            put(zip, "assets/minecraft/optifine/cit/override/skin.png", "fixture");
        }
        try (var resources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES,
            List.of(open(coffee, "coffee"), open(override, "override")))) {
            registry.install(ArmorCitRegistry.load(resources));
            check(registry.select("minecraft:diamond_helmet", 50, 100, Map.of("minecraft:protection", 4), "diamond_layer_1", BASE)
                .getPath().equals("optifine/cit/override/skin.png"), "Higher-priority resource pack must win before rule weight");
        }
        try (var empty = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of())) {
            registry.install(ArmorCitRegistry.load(empty));
            check(registry.select("minecraft:diamond_helmet", 50, 100, Map.of("minecraft:protection", 4), "diamond_layer_1", BASE).equals(BASE), "Removed pack left stale armor textures");
        }
    }

    private static PackResources open(Path path, String id) {
        return new FilePackResources.FileResourcesSupplier(path).openPrimary(
            new PackLocationInfo(id, Component.literal(id), PackSource.DEFAULT, Optional.empty()));
    }

    private static byte[] read(MultiPackResourceManager resources, Identifier texture) throws Exception {
        try (var stream = resources.getResource(texture).orElseThrow().open()) {
            return stream.readAllBytes();
        }
    }

    private static void checkHook() throws Exception {
        ClassNode renderer = new ClassNode();
        try (var stream = ArmorCitCheck.class.getResourceAsStream("/net/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer.class")) {
            new ClassReader(stream).accept(renderer, 0);
        }
        String desc = "(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";
        MethodNode method = renderer.methods.stream().filter(m -> m.name.equals("renderLayers") && m.desc.equals(desc)).findFirst().orElseThrow();
        int targets = 0;
        boolean glint = false, trim = false;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("armorCutoutNoCull") && call.desc.equals("(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")) targets++;
                if (call.name.equals("armorEntityGlint")) glint = true;
                if (call.name.equals("armorTrimsSheet")) trim = true;
            }
        }
        check(targets == 1 && glint && trim, "Armor texture hook no longer matches base layer independently of glint/trims");
        String mixins = Files.readString(Path.of("src/main/resources/pizzadlc.client.mixins.json"));
        check(mixins.contains("EquipmentLayerRendererCitMixin"), "CIT mixin is not registered");
    }

    private static Properties properties(String text) throws Exception {
        Properties result = new Properties();
        result.load(new java.io.StringReader(text));
        return result;
    }

    private static void put(ZipOutputStream zip, String name, String text) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
