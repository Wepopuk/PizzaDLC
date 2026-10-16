"""Preserve Coffee and add resource paths for Java 1.16.5 through 26.2.

Atlas conversions use Mojang Slicer releases (hash checked, cached under build).
Only the output ZIP is written; the supplied Coffee.zip is never changed.
Custom worn armor requires OptiFine on older versions or this client on 26.2.
"""
import argparse
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
SLICERS = [
    ("v1.0", "slicer-1.0-SNAPSHOT.jar", "81ed9998dd638c97f7c172f68e94f12011547fa64a11937978d445bcc6abbfdf"),
    ("v1.1.3", "slicer-1.20.2-1.1.3-SNAPSHOT.jar", "c3dc87dd010923c06682a12e1637c4539ca25ee75b35bc80c007b05576479ca2"),
    ("v1.1.5", "slicer-1.20.5-1.1.5-SNAPSHOT.jar", "a8077407980a11427c4781a69bc0e86f29a848b7dd261f6249c659463447c760"),
    ("v1.2.1", "slicer-26.2-1.2.1-SNAPSHOT.jar", "3603ec15fb48c11d1486c1f4436d708c921fbfaa8513cfdf212418bd1175a5c5"),
]

BLOCK_ALIASES = dict(pair.split(":") for pair in """
grass:short_grass
anvil_base:anvil anvil_top_damaged_0:anvil_top anvil_top_damaged_1:chipped_anvil_top anvil_top_damaged_2:damaged_anvil_top
brick:bricks cobblestone_mossy:mossy_cobblestone comparator_off:comparator deadbush:dead_bush
dirt_podzol_side:podzol_side dirt_podzol_top:podzol_top dispenser_front_horizontal:dispenser_front
dropper_front_horizontal:dropper_front endframe_eye:end_portal_frame_eye endframe_side:end_portal_frame_side
endframe_top:end_portal_frame_top end_bricks:end_stone_bricks farmland_dry:farmland farmland_wet:farmland_moist
fire_layer_0:fire_0 fire_layer_1:fire_1 furnace_front_off:furnace_front grass_side:grass_block_side
grass_side_overlay:grass_block_side_overlay grass_side_snowed:grass_block_snow grass_top:grass_block_top
hardened_clay:terracotta ice_packed:packed_ice itemframe_background:item_frame mushroom_block_skin_brown:brown_mushroom_block
mushroom_block_skin_red:red_mushroom_block mushroom_block_skin_stem:mushroom_stem mushroom_brown:brown_mushroom
mushroom_red:red_mushroom nether_brick:nether_bricks noteblock:note_block observer_back_lit:observer_back_on
piston_top_normal:piston_top portal:nether_portal prismarine_dark:dark_prismarine prismarine_rough:prismarine
pumpkin_face_off:carved_pumpkin pumpkin_face_on:jack_o_lantern quartz_block_chiseled:chiseled_quartz_block
quartz_block_chiseled_top:chiseled_quartz_block_top quartz_block_lines:quartz_pillar quartz_block_lines_top:quartz_pillar_top
quartz_ore:nether_quartz_ore rail_activator:activator_rail rail_activator_powered:activator_rail_on
rail_detector:detector_rail rail_detector_powered:detector_rail_on rail_golden:powered_rail
rail_golden_powered:powered_rail_on rail_normal:rail rail_normal_turned:rail_corner redstone_lamp_off:redstone_lamp
redstone_torch_on:redstone_torch red_nether_brick:red_nether_bricks red_sandstone_carved:chiseled_red_sandstone
red_sandstone_normal:red_sandstone red_sandstone_smooth:cut_red_sandstone reeds:sugar_cane repeater_off:repeater
sandstone_carved:chiseled_sandstone sandstone_normal:sandstone sandstone_smooth:cut_sandstone slime:slime_block
sponge_wet:wet_sponge stonebrick:stone_bricks stonebrick_carved:chiseled_stone_bricks
stonebrick_cracked:cracked_stone_bricks stonebrick_mossy:mossy_stone_bricks stone_andesite:andesite
stone_andesite_smooth:polished_andesite stone_diorite:diorite stone_diorite_smooth:polished_diorite
stone_granite:granite stone_granite_smooth:polished_granite stone_slab_side:smooth_stone_slab_side
stone_slab_top:smooth_stone torch_on:torch trip_wire:tripwire trip_wire_source:tripwire_hook web:cobweb
""".split())
ITEM_ALIASES = dict(pair.split(":") for pair in """
beef_cooked:cooked_beef beef_raw:beef book_enchanted:enchanted_book book_normal:book book_writable:writable_book
book_written:written_book bow_standby:bow broken_elytra:elytra_broken bucket_empty:bucket bucket_lava:lava_bucket
bucket_milk:milk_bucket bucket_water:water_bucket carrot_golden:golden_carrot chicken_cooked:cooked_chicken
chicken_raw:chicken chorus_fruit_popped:popped_chorus_fruit door_wood:oak_door fireworks:firework_rocket
fireworks_charge:firework_star fireworks_charge_overlay:firework_star_overlay fishing_rod_uncast:fishing_rod
fish_clownfish_raw:tropical_fish fish_cod_cooked:cooked_cod fish_cod_raw:cod fish_pufferfish_raw:pufferfish
fish_salmon_cooked:cooked_salmon fish_salmon_raw:salmon map_empty:map map_filled:filled_map
map_filled_markings:filled_map_markings melon:melon_slice melon_speckled:glistering_melon_slice
minecart_chest:chest_minecart minecart_command_block:command_block_minecart minecart_furnace:furnace_minecart
minecart_hopper:hopper_minecart minecart_normal:minecart minecart_tnt:tnt_minecart mutton_cooked:cooked_mutton
mutton_raw:mutton netherbrick:nether_brick porkchop_cooked:cooked_porkchop porkchop_raw:porkchop
potato_baked:baked_potato potato_poisonous:poisonous_potato potion_bottle_drinkable:potion
potion_bottle_empty:glass_bottle potion_bottle_lingering:lingering_potion potion_bottle_splash:splash_potion
rabbit_cooked:cooked_rabbit rabbit_raw:rabbit redstone_dust:redstone reeds:sugar_cane seeds_melon:melon_seeds
seeds_pumpkin:pumpkin_seeds seeds_wheat:wheat_seeds sign:oak_sign slimeball:slime_ball
spider_eye_fermented:fermented_spider_eye wooden_armorstand:armor_stand
""".split())
COLORS = "black blue brown cyan gray green light_blue lime magenta orange pink purple red silver white yellow".split()
for color in COLORS:
    modern = "light_gray" if color == "silver" else color
    for old, new in [("concrete_", "_concrete"), ("concrete_powder_", "_concrete_powder"),
                     ("glass_", "_stained_glass"), ("glass_pane_top_", "_stained_glass_pane_top"),
                     ("glazed_terracotta_", "_glazed_terracotta"), ("hardened_clay_stained_", "_terracotta")]:
        BLOCK_ALIASES[old + color] = modern + new
    ITEM_ALIASES["dye_powder_" + color] = {"black": "ink_sac", "blue": "lapis_lazuli", "brown": "cocoa_beans",
                                           "white": "bone_meal"}.get(color, modern + "_dye")
for wood in "acacia birch jungle oak spruce big_oak roofed_oak".split():
    modern = "dark_oak" if wood in ("big_oak", "roofed_oak") else wood
    for old, new in [("log_", "_log"), ("planks_", "_planks"), ("sapling_", "_sapling")]:
        BLOCK_ALIASES[old + wood] = modern + new
    BLOCK_ALIASES["log_" + wood + "_top"] = modern + "_log_top"
    ITEM_ALIASES["door_" + modern] = modern + "_door"
for plant, count in [("beetroots", 4), ("carrots", 4), ("cocoa", 3), ("nether_wart", 3), ("potatoes", 4), ("wheat", 8)]:
    for stage in range(count):
        BLOCK_ALIASES[f"{plant}_stage_{stage}"] = f"{plant}_stage{stage}"
for tool in "axe hoe pickaxe shovel sword".split():
    ITEM_ALIASES["gold_" + tool] = "golden_" + tool
    ITEM_ALIASES["wood_" + tool] = "wooden_" + tool
for part in "boots chestplate helmet horse_armor leggings".split():
    ITEM_ALIASES["gold_" + part] = "golden_" + part
for disc in "11 13 blocks cat chirp far mall mellohi stal strad wait ward".split():
    ITEM_ALIASES["record_" + disc] = "music_disc_" + disc


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def valid_path(name):
    path = PurePosixPath(name)
    if path.is_absolute() or ".." in path.parts or "\\" in name:
        raise ValueError(f"Unsafe archive path: {name}")
    return name


def convert(source, output, java):
    if source.resolve() == output.resolve():
        raise ValueError("Original and output paths must differ")
    original_hash = hashlib.sha256(source.read_bytes()).hexdigest()
    with zipfile.ZipFile(source) as archive:
        original = {valid_path(n): archive.read(n) for n in archive.namelist() if not n.endswith("/")}
    result = dict(original)
    changes = []

    def copy(old, new, replace=False):
        if old in original and (new not in result or replace):
            result[new] = original[old]
            changes.append({"source": old, "target": new})

    # Copy modern names first; legacy aliases never replace a pack's explicit modern texture.
    for name in sorted(original):
        if name.startswith("assets/minecraft/textures/blocks/"):
            copy(name, name.replace("/blocks/", "/block/"))
    for old, new in BLOCK_ALIASES.items():
        for suffix in (".png", ".png.mcmeta"):
            copy(f"assets/minecraft/textures/blocks/{old}{suffix}", f"assets/minecraft/textures/block/{new}{suffix}")
    for old, new in ITEM_ALIASES.items():
        for suffix in (".png", ".png.mcmeta"):
            copy(f"assets/minecraft/textures/item/{old}{suffix}", f"assets/minecraft/textures/item/{new}{suffix}")
    for material in "chainmail diamond gold iron leather netherite".split():
        for layer, folder in [(1, "humanoid"), (2, "humanoid_leggings")]:
            for overlay in ("", "_overlay") if material == "leather" else ("",):
                for suffix in (".png", ".png.mcmeta"):
                    copy(f"assets/minecraft/textures/models/armor/{material}_layer_{layer}{overlay}{suffix}",
                         f"assets/minecraft/textures/entity/equipment/{folder}/{material}{overlay}{suffix}")
    for target in ("enchanted_glint_item", "enchanted_glint_entity", "enchanted_glint_armor"):
        for suffix in (".png", ".png.mcmeta"):
            copy("assets/minecraft/textures/misc/enchanted_item_glint" + suffix,
                 "assets/minecraft/textures/misc/" + target + suffix)
    for wood in "acacia birch jungle oak spruce darkoak".split():
        modern = "dark_oak" if wood == "darkoak" else wood
        copy(f"assets/minecraft/textures/entity/boat/boat_{wood}.png", f"assets/minecraft/textures/entity/boat/{modern}.png")
    # Keep custom model textures in the item directory included by modern atlases.
    for name in sorted(original):
        if name.startswith("assets/minecraft/textures/3d/"):
            copy(name, name.replace("/textures/3d/", "/textures/item/coffee/"))
        if name.startswith("assets/minecraft/models/item/") and name.endswith(".json"):
            model = json.loads(original[name])
            for key, texture in model.get("textures", {}).items():
                if isinstance(texture, str):
                    model["textures"][key] = texture.replace("blocks/", "block/").replace("3d/", "item/coffee/")
            result[name] = json_bytes(model)

    # Modern resource identifiers reject spaces. Mirror MCPatcher CIT into the OptiFine tree,
    # which is the directory current CIT implementations use when both roots are present.
    for name in sorted(original):
        if "/cit/" not in name:
            continue
        target = name.replace("/mcpatcher/cit/", "/optifine/cit/").replace(" ", "_")
        copy(name, target)
        if " " in name:
            del result[name]
    for name in list(result):
        if "/cit/" in name and name.endswith(".properties") and " " not in name:
            text = result[name].decode("utf-8-sig").replace("\r", "")
            # Explicit priority keeps the protection IV skin visible when the
            # helmet also matches a durability indicator rule.
            if re.search(r"^type=armor$", text, re.M) and "enchantments=protection" in text:
                text += "\nweight=100\n"
            result[name] = text.encode("utf-8")

    cache = ROOT / "build" / "coffee-slicer"
    cache.mkdir(parents=True, exist_ok=True)
    slicer_counts = {}
    with tempfile.TemporaryDirectory(prefix="coffee-", dir=cache) as temporary:
        work = Path(temporary)
        source_dir = work / "input"
        for name, data in original.items():
            target = source_dir / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        for tag, filename, digest in SLICERS:
            jar = cache / filename
            if not jar.exists():
                with urllib.request.urlopen(f"https://github.com/Mojang/slicer/releases/download/{tag}/{filename}") as response:
                    jar.write_bytes(response.read())
            if hashlib.sha256(jar.read_bytes()).hexdigest() != digest:
                raise ValueError(f"Slicer checksum mismatch: {filename}")
            out_dir = work / tag
            # The 26.2 release uses namespace-root input/output; earlier releases use pack-root.
            namespace_root = tag == "v1.2.1"
            subprocess.run([str(java), "-jar", str(jar), str(source_dir / "assets" if namespace_root else source_dir),
                            str(out_dir)], check=True, capture_output=True)
            count = 0
            for path in sorted(out_dir.rglob("*")):
                if path.is_file():
                    name = path.relative_to(out_dir).as_posix()
                    if namespace_root:
                        name = "assets/" + name
                    valid_path(name)
                    if name not in result:
                        result[name] = path.read_bytes()
                        changes.append({"source": "Mojang Slicer " + tag, "target": name})
                        count += 1
            slicer_counts[tag] = count
            if tag in ("v1.1.3", "v1.2.1") and count == 0:
                raise ValueError(f"No GUI/bed resources created by {filename}")

    # In 1.21.4+ shields use special item renderers. Coffee's shield is an ordinary
    # element model; keep its geometry and blocking pose through the modern item definition.
    def shield_model(blocking):
        suffix = "_blocking" if blocking else ""
        return {"type": "minecraft:select", "property": "minecraft:main_hand",
                "cases": [{"when": "left", "model": {"type": "minecraft:model", "model": "minecraft:item/shield_lh" + suffix}}],
                "fallback": {"type": "minecraft:model", "model": "minecraft:item/shield" + suffix}}

    result["assets/minecraft/items/shield.json"] = json_bytes({"model": {
        "type": "minecraft:condition", "property": "minecraft:using_item",
        "on_true": shield_model(True), "on_false": shield_model(False)}})
    result["assets/minecraft/items/end_crystal.json"] = json_bytes({"model": {
        "type": "minecraft:model", "model": "minecraft:item/end_crystal"}})
    metadata = json.loads(original["pack.mcmeta"])
    metadata["pack"].update(pack_format=15, supported_formats={"min_inclusive": 15, "max_inclusive": 88},
                            min_format=15, max_format=[88, 0])
    original_description = metadata["pack"]["description"]
    metadata["pack"]["description"] += "\nCoffee compatibility | 1.20 - 26.2"
    result["pack.mcmeta"] = json_bytes(metadata)
    result["COFFEE-COMPATIBILITY.txt"] = (
        "Coffee compatibility: Java 1.16.5 through 26.2\n"
        "Original artwork and credit: shame / shamemik1337.\n"
        "Enable this pack above other texture packs; disable the original Coffee.zip.\n"
        "Worn custom armor: OptiFine/CIT armor support on older versions; updated PizzaDLC client on 26.2.\n"
        "Without CIT support, Minecraft uses Coffee's ordinary armor textures.\n"
        "Use Coffee-1.20-26.2.zip for 1.20 and newer. Older versions need their ZIP from the Coffee-versions directory.\n"
        "OptiFine skies, CTM, animated textures and custom item rules still need their corresponding mod support.\n"
        "Converted atlas textures with Mojang Slicer: https://github.com/Mojang/slicer\n"
        "Tested: archive integrity, real 26.2 resource loading and all armor-rule boundaries.\n"
        "In-game visuals on every intermediate release have not been tested.\n"
    ).encode("utf-8")
    def write_pack(target):
        target.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for name, data in sorted(result.items()):
                info = zipfile.ZipInfo(name, date_time=(2026, 10, 9, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                archive.writestr(info, data)

    write_pack(output)
    with zipfile.ZipFile(output) as archive:
        assert archive.testzip() is None, "Output ZIP is corrupt"
        assert len(archive.namelist()) == len(set(archive.namelist())), "Duplicate entries"
        for name in original:
            if name != "pack.mcmeta" and not name.endswith(".properties") and not name.startswith("assets/minecraft/models/item/"):
                retained = name.replace(" ", "_") if "/cit/" in name else name
                assert archive.read(retained) == original[name], f"Changed original asset: {name}"
    assert hashlib.sha256(source.read_bytes()).hexdigest() == original_hash, "Original was changed"
    report = {"source": str(source), "source_sha256": original_hash, "output": str(output),
              "output_sha256": hashlib.sha256(output.read_bytes()).hexdigest(), "entries": len(result),
              "slicer_outputs": slicer_counts, "added_resources": len(changes), "changes": changes, "legacy_packs": []}
    legacy_dir = output.parent / "Coffee-versions"
    for label, pack_format in [("1.16.5", 6), ("1.17.x", 7), ("1.18.x", 8), ("1.19-1.19.2", 9), ("1.19.3", 12), ("1.19.4", 13)]:
        result["pack.mcmeta"] = json_bytes({"pack": {"pack_format": pack_format,
            "description": original_description + "\nCoffee compatibility | " + label}})
        target = legacy_dir / ("Coffee-" + label + ".zip")
        write_pack(target)
        with zipfile.ZipFile(target) as archive:
            assert archive.testzip() is None
            assert json.loads(archive.read("pack.mcmeta"))["pack"]["pack_format"] == pack_format
        report["legacy_packs"].append({"version": label, "pack_format": pack_format, "path": str(target)})
        print("Created " + target.name, flush=True)
    (ROOT / "build" / "coffee-conversion.json").write_bytes(json_bytes(report))
    print(json.dumps({k: v for k, v in report.items() if k != "changes"}, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=ROOT / "run/resourcepacks/Coffee.zip")
    parser.add_argument("--output", type=Path, default=ROOT / "run/resourcepacks/Coffee-1.20-26.2.zip")
    parser.add_argument("--java", type=Path, default=Path(shutil.which("java") or "java"))
    args = parser.parse_args()
    convert(args.source, args.output, args.java)
