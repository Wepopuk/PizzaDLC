package pizza.dlc.api.resource;

import java.util.*;
import net.minecraft.resources.Identifier;

/** The armor subset of OptiFine CIT; unsupported conditions must never match silently. */
public record ArmorCitRule(Identifier source, int packPriority, int weight, Set<String> items,
                           Map<String, Identifier> textures, Set<String> enchantments,
                           IntRanges levels, IntRanges damage, boolean percentDamage) {
    public static ArmorCitRule parse(Identifier source, Properties p, int priority) {
        if (!"armor".equals(p.getProperty("type"))) return null;
        Set<String> supported = Set.of("type", "items", "matchItems", "weight", "enchantments",
            "enchantmentLevels", "damage", "texture");
        for (String key : p.stringPropertyNames()) {
            if (!supported.contains(key) && !key.startsWith("texture.")) return null;
        }
        Set<String> items = ids(p.getProperty("items", p.getProperty("matchItems", "")));
        if (items.isEmpty()) return null;
        Map<String, Identifier> textures = new HashMap<>();
        for (String key : p.stringPropertyNames()) {
            if (key.equals("texture") || key.startsWith("texture.")) {
                Identifier texture = resolveTexture(source, p.getProperty(key));
                if (texture == null) throw new IllegalArgumentException("Invalid texture: " + key);
                textures.put(key.equals("texture") ? "*" : key.substring(8), texture);
            }
        }
        if (textures.isEmpty()) return null;
        String damage = p.getProperty("damage", "").trim();
        boolean percent = damage.endsWith("%");
        return new ArmorCitRule(source, priority, Integer.parseInt(p.getProperty("weight", "0").trim()),
            Set.copyOf(items), Map.copyOf(textures), Set.copyOf(ids(p.getProperty("enchantments", ""))),
            IntRanges.parse(p.getProperty("enchantmentLevels", "")),
            IntRanges.parse(percent ? damage.substring(0, damage.length() - 1) : damage), percent);
    }

    public boolean matches(String item, int usedDamage, int maxDamage, Map<String, Integer> enchants) {
        if (!items.contains(item)) return false;
        if (damage != null) {
            if (maxDamage <= 0) return false;
            int value = percentDamage ? (int) ((long) usedDamage * 100 / maxDamage) : usedDamage;
            if (!damage.contains(value)) return false;
        }
        if (enchantments.isEmpty() && levels == null) return true;
        for (var enchant : enchants.entrySet()) {
            if ((enchantments.isEmpty() || enchantments.contains(enchant.getKey()))
                && (levels == null || levels.contains(enchant.getValue()))) return true;
        }
        return false;
    }

    public Identifier textureFor(String legacyLayer) {
        return textures.getOrDefault(legacyLayer, textures.get("*"));
    }

    private static Set<String> ids(String value) {
        Set<String> result = new HashSet<>();
        for (String token : value.trim().split("\\s+")) {
            if (token.isEmpty()) continue;
            Identifier id = Identifier.tryParse(token);
            if (id == null) throw new IllegalArgumentException("Invalid identifier: " + token);
            result.add(id.toString());
        }
        return result;
    }

    private static Identifier resolveTexture(Identifier source, String value) {
        String path = value.trim();
        if (path.contains(":")) return Identifier.tryParse(path.endsWith(".png") ? path : path + ".png");
        if (path.startsWith("~/")) path = "optifine/" + path.substring(2);
        else if (path.startsWith("/")) path = path.substring(1);
        else if (!path.startsWith("textures/") && !path.startsWith("optifine/") && !path.startsWith("mcpatcher/")) {
            if (path.startsWith("./")) path = path.substring(2);
            path = source.getPath().substring(0, source.getPath().lastIndexOf('/') + 1) + path;
        }
        if (!path.endsWith(".png")) path += ".png";
        return Identifier.tryParse(source.getNamespace() + ":" + path);
    }

    public record IntRanges(List<int[]> intervals) {
        public static IntRanges parse(String text) {
            if (text.isBlank()) return null;
            List<int[]> ranges = new ArrayList<>();
            for (String token : text.trim().split("\\s+")) {
                if (!token.matches("\\d+(-\\d*)?|-\\d+")) throw new IllegalArgumentException("Invalid range: " + token);
                int dash = token.indexOf('-');
                int min = dash < 0 ? Integer.parseInt(token) : dash == 0 ? 0 : Integer.parseInt(token.substring(0, dash));
                int max = dash < 0 ? min : dash == token.length() - 1 ? Integer.MAX_VALUE : Integer.parseInt(token.substring(dash + 1));
                if (min > max) throw new IllegalArgumentException("Reversed range: " + token);
                ranges.add(new int[]{min, max});
            }
            return new IntRanges(List.copyOf(ranges));
        }

        public boolean contains(int value) {
            for (int[] range : intervals) if (value >= range[0] && value <= range[1]) return true;
            return false;
        }
    }
}
