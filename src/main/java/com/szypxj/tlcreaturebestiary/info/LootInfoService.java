package com.szypxj.tlcreaturebestiary.info;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Optional;
import java.util.Set;

public final class LootInfoService {
    private static final int MAX_RECURSION = 8;
    private static final int MAX_RESULTS = 128;
    private static final Map<MinecraftServer, Map<ResourceLocation, List<DropInfo>>> CACHE = new WeakHashMap<>();

    private LootInfoService() {
    }

    public static List<DropInfo> describe(MinecraftServer server, EntityType<?> type) {
        if (server == null || type == null || type == EntityType.PLAYER) {
            return List.of();
        }
        ResourceLocation tableId = type.getDefaultLootTable();
        if (tableId == null) {
            return List.of();
        }
        synchronized (CACHE) {
            Map<ResourceLocation, List<DropInfo>> byTable = CACHE.computeIfAbsent(server, ignored -> new LinkedHashMap<>());
            List<DropInfo> cached = byTable.get(tableId);
            if (cached != null) {
                return cached;
            }
            List<Candidate> candidates = new ArrayList<>();
            parseTable(server, tableId, candidates, new HashSet<>(), 0, Context.DEFAULT);
            List<DropInfo> result = merge(candidates);
            byTable.put(tableId, result);
            return result;
        }
    }

    public static void clearCache(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (CACHE) {
            CACHE.remove(server);
        }
    }

    private static void parseTable(
            MinecraftServer server,
            ResourceLocation tableId,
            List<Candidate> out,
            Set<ResourceLocation> stack,
            int depth,
            Context inherited
    ) {
        if (depth > MAX_RECURSION || out.size() >= MAX_RESULTS || !stack.add(tableId)) {
            return;
        }
        try {
            JsonObject root = loadTable(server, tableId);
            if (root == null) {
                return;
            }
            JsonArray pools = array(root, "pools");
            if (pools == null) {
                return;
            }
            for (JsonElement poolElement : pools) {
                if (!poolElement.isJsonObject() || out.size() >= MAX_RESULTS) {
                    continue;
                }
                parsePool(server, poolElement.getAsJsonObject(), out, stack, depth, inherited);
            }
        } finally {
            stack.remove(tableId);
        }
    }

    private static JsonObject loadTable(MinecraftServer server, ResourceLocation tableId) {
        ResourceLocation resourceId = ResourceLocation.tryBuild(
                tableId.getNamespace(),
                "loot_tables/" + tableId.getPath() + ".json"
        );
        if (resourceId == null) {
            return null;
        }
        Optional<Resource> resource = server.getResourceManager().getResource(resourceId);
        if (resource.isEmpty()) {
            return null;
        }
        try (BufferedReader reader = resource.get().openAsReader()) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static void parsePool(
            MinecraftServer server,
            JsonObject pool,
            List<Candidate> out,
            Set<ResourceLocation> stack,
            int depth,
            Context inherited
    ) {
        JsonArray entries = array(pool, "entries");
        if (entries == null || entries.size() == 0) {
            return;
        }
        ConditionInfo poolConditions = parseConditions(array(pool, "conditions"));
        CountRange poolCount = parseFunctions(array(pool, "functions"), CountRange.ONE);
        int rolls = parseConstantInt(pool.get("rolls"));
        boolean rollsKnown = rolls >= 0;
        if (!rollsKnown) {
            rolls = 1;
        }

        int totalWeight = 0;
        boolean simpleWeightedPool = true;
        for (JsonElement entry : entries) {
            if (!entry.isJsonObject() || !isSimpleSelectable(entry.getAsJsonObject())) {
                simpleWeightedPool = false;
                break;
            }
            totalWeight += Math.max(1, integer(entry.getAsJsonObject(), "weight", 1));
        }
        if (totalWeight <= 0) {
            simpleWeightedPool = false;
        }

        for (JsonElement entryElement : entries) {
            if (!entryElement.isJsonObject() || out.size() >= MAX_RESULTS) {
                continue;
            }
            JsonObject entry = entryElement.getAsJsonObject();
            double weightedChance = -1.0D;
            if (simpleWeightedPool && rollsKnown) {
                int weight = Math.max(1, integer(entry, "weight", 1));
                double perRoll = weight / (double) totalWeight;
                weightedChance = 1.0D - Math.pow(1.0D - perRoll, Math.max(0, rolls));
            }
            Context poolContext = inherited.combine(
                    poolConditions,
                    poolCount,
                    weightedChance,
                    !simpleWeightedPool || !rollsKnown
            );
            parseEntry(server, entry, out, stack, depth, poolContext);
        }
    }

    private static void parseEntry(
            MinecraftServer server,
            JsonObject entry,
            List<Candidate> out,
            Set<ResourceLocation> stack,
            int depth,
            Context inherited
    ) {
        String type = typePath(string(entry, "type", ""));
        ConditionInfo conditions = parseConditions(array(entry, "conditions"));
        CountRange count = parseFunctions(array(entry, "functions"), inherited.count());
        Context context = inherited.combine(conditions, count, 1.0D, false);

        switch (type) {
            case "item" -> addItem(entry, out, context);
            case "tag" -> addTag(entry, out, context.markSpecial());
            case "loot_table" -> {
                ResourceLocation nested = resourceLocation(string(entry, "name", ""));
                if (nested != null) {
                    parseTable(server, nested, out, stack, depth + 1, context.markSpecial());
                }
            }
            case "alternatives", "sequence", "group" -> {
                JsonArray children = array(entry, "children");
                if (children != null) {
                    for (JsonElement child : children) {
                        if (child.isJsonObject()) {
                            parseEntry(server, child.getAsJsonObject(), out, stack, depth, context.markSpecial());
                        }
                    }
                }
            }
            case "empty" -> {
            }
            default -> {
                JsonArray children = array(entry, "children");
                if (children != null) {
                    for (JsonElement child : children) {
                        if (child.isJsonObject()) {
                            parseEntry(server, child.getAsJsonObject(), out, stack, depth, context.markSpecial());
                        }
                    }
                }
            }
        }
    }

    private static void addItem(JsonObject entry, List<Candidate> out, Context context) {
        ResourceLocation itemId = resourceLocation(string(entry, "name", ""));
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return;
        }
        out.add(new Candidate(itemId, context));
    }

    private static void addTag(JsonObject entry, List<Candidate> out, Context context) {
        ResourceLocation tagId = resourceLocation(string(entry, "name", ""));
        if (tagId == null) {
            return;
        }
        TagKey<Item> key = TagKey.create(Registries.ITEM, tagId);
        BuiltInRegistries.ITEM.getTag(key).ifPresent(named -> named.forEach(holder -> {
            if (out.size() >= MAX_RESULTS) {
                return;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(holder.value());
            if (itemId != null) {
                out.add(new Candidate(itemId, context));
            }
        }));
    }

    private static ConditionInfo parseConditions(JsonArray conditions) {
        if (conditions == null || conditions.size() == 0) {
            return ConditionInfo.NONE;
        }
        double chance = 1.0D;
        boolean chanceKnown = true;
        boolean killedByPlayer = false;
        boolean looting = false;
        boolean special = false;
        for (JsonElement element : conditions) {
            if (!element.isJsonObject()) {
                special = true;
                chanceKnown = false;
                continue;
            }
            JsonObject condition = element.getAsJsonObject();
            String type = typePath(string(condition, "condition", ""));
            switch (type) {
                case "random_chance" -> chance *= decimal(condition, "chance", 1.0D);
                case "random_chance_with_looting" -> {
                    chance *= decimal(condition, "chance", 1.0D);
                    looting = true;
                }
                case "killed_by_player" -> killedByPlayer = true;
                case "survives_explosion" -> {
                }
                default -> {
                    special = true;
                    chanceKnown = false;
                }
            }
        }
        return new ConditionInfo(chanceKnown ? clamp01(chance) : -1.0D, killedByPlayer, looting, special);
    }

    private static CountRange parseFunctions(JsonArray functions, CountRange initial) {
        if (functions == null || functions.size() == 0) {
            return initial;
        }
        CountRange count = initial;
        boolean looting = count.lootingAffected();
        boolean special = count.special();
        for (JsonElement element : functions) {
            if (!element.isJsonObject()) {
                special = true;
                continue;
            }
            JsonObject function = element.getAsJsonObject();
            String type = typePath(string(function, "function", ""));
            switch (type) {
                case "set_count" -> {
                    CountRange parsed = parseNumberProvider(function.get("count"));
                    if (parsed == null) {
                        special = true;
                    } else if (booleanValue(function, "add", false)) {
                        count = new CountRange(
                                count.min() + parsed.min(),
                                addMax(count.max(), parsed.max()),
                                looting || parsed.lootingAffected(),
                                special || parsed.special()
                        );
                    } else {
                        count = parsed;
                    }
                }
                case "looting_enchant" -> {
                    looting = true;
                    count = new CountRange(count.min(), -1, true, count.special());
                }
                case "limit_count", "set_damage", "set_nbt", "set_name", "set_lore", "enchant_randomly", "enchant_with_levels" -> special = true;
                default -> special = true;
            }
        }
        return new CountRange(count.min(), count.max(), looting || count.lootingAffected(), special || count.special());
    }

    private static CountRange parseNumberProvider(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            int value = Math.max(0, (int) Math.floor(element.getAsDouble()));
            return new CountRange(value, value, false, false);
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        String type = typePath(string(object, "type", ""));
        if ("constant".equals(type)) {
            int value = Math.max(0, (int) Math.floor(decimal(object, "value", 0.0D)));
            return new CountRange(value, value, false, false);
        }
        if ("uniform".equals(type)) {
            Integer min = numberAsInt(object.get("min"));
            Integer max = numberAsInt(object.get("max"));
            if (min != null && max != null) {
                return new CountRange(Math.max(0, min), Math.max(Math.max(0, min), max), false, false);
            }
        }
        return null;
    }

    private static int parseConstantInt(JsonElement element) {
        CountRange range = parseNumberProvider(element);
        if (range == null || range.max() < 0 || range.min() != range.max()) {
            return -1;
        }
        return range.min();
    }

    private static List<DropInfo> merge(List<Candidate> candidates) {
        Map<MergeKey, DropInfo> merged = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            Context context = candidate.context();
            double chancePercent = context.chance() < 0.0D ? -1.0D : clamp01(context.chance()) * 100.0D;
            DropInfo next = new DropInfo(
                    candidate.itemId(),
                    context.count().min(),
                    context.count().max(),
                    chancePercent,
                    context.killedByPlayer(),
                    context.lootingAffected() || context.count().lootingAffected(),
                    context.special() || context.count().special()
            );
            MergeKey key = new MergeKey(
                    next.itemId(),
                    next.minCount(),
                    next.maxCount(),
                    next.killedByPlayer(),
                    next.lootingAffected(),
                    next.specialCondition()
            );
            DropInfo previous = merged.get(key);
            if (previous == null) {
                merged.put(key, next);
            } else {
                double mergedChance = mergeChance(previous.chancePercent(), next.chancePercent());
                merged.put(key, new DropInfo(
                        next.itemId(),
                        next.minCount(),
                        next.maxCount(),
                        mergedChance,
                        next.killedByPlayer(),
                        next.lootingAffected(),
                        next.specialCondition()
                ));
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(drop -> drop.itemId().toString()))
                .limit(MAX_RESULTS)
                .toList();
    }

    private static double mergeChance(double firstPercent, double secondPercent) {
        if (firstPercent < 0.0D || secondPercent < 0.0D) {
            return -1.0D;
        }
        double first = clamp01(firstPercent / 100.0D);
        double second = clamp01(secondPercent / 100.0D);
        return (1.0D - (1.0D - first) * (1.0D - second)) * 100.0D;
    }

    private static boolean isSimpleSelectable(JsonObject entry) {
        String type = typePath(string(entry, "type", ""));
        return "item".equals(type) || "tag".equals(type) || "loot_table".equals(type) || "empty".equals(type);
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double decimal(JsonObject object, String key, double fallback) {
        JsonElement element = object.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static Integer numberAsInt(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        try {
            return (int) Math.floor(element.getAsDouble());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String typePath(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static ResourceLocation resourceLocation(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(id);
    }

    private static int addMax(int first, int second) {
        return first < 0 || second < 0 ? -1 : first + second;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private record Candidate(ResourceLocation itemId, Context context) {
    }

    private record MergeKey(
            ResourceLocation itemId,
            int minCount,
            int maxCount,
            boolean killedByPlayer,
            boolean lootingAffected,
            boolean specialCondition
    ) {
    }

    private record ConditionInfo(
            double chance,
            boolean killedByPlayer,
            boolean lootingAffected,
            boolean special
    ) {
        private static final ConditionInfo NONE = new ConditionInfo(1.0D, false, false, false);
    }

    private record CountRange(int min, int max, boolean lootingAffected, boolean special) {
        private static final CountRange ONE = new CountRange(1, 1, false, false);
    }

    private record Context(
            double chance,
            boolean killedByPlayer,
            boolean lootingAffected,
            boolean special,
            CountRange count
    ) {
        private static final Context DEFAULT = new Context(1.0D, false, false, false, CountRange.ONE);

        private Context combine(ConditionInfo conditions, CountRange nextCount, double weightedChance, boolean weightedUnknown) {
            double nextChance = chance;
            if (nextChance < 0.0D || conditions.chance() < 0.0D || weightedUnknown || weightedChance < 0.0D) {
                nextChance = -1.0D;
            } else {
                nextChance = clamp01(nextChance * conditions.chance() * weightedChance);
            }
            return new Context(
                    nextChance,
                    killedByPlayer || conditions.killedByPlayer(),
                    lootingAffected || conditions.lootingAffected(),
                    special || conditions.special() || weightedUnknown,
                    nextCount
            );
        }

        private Context markSpecial() {
            return new Context(-1.0D, killedByPlayer, lootingAffected, true, count);
        }
    }
}
