package com.szypxj.tlcreaturebestiary.info;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class SpawnBiomeInfoService {
    private static final Map<MinecraftServer, Map<ResourceLocation, List<ResourceLocation>>> CACHE = new WeakHashMap<>();
    private SpawnBiomeInfoService() {
    }

    public static List<ResourceLocation> findNaturalSpawnBiomes(MinecraftServer server, EntityType<?> type) {
        if (server == null || type == null || type == EntityType.PLAYER) {
            return List.of();
        }
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (typeId == null) {
            return List.of();
        }
        synchronized (CACHE) {
            Map<ResourceLocation, List<ResourceLocation>> byType = CACHE.computeIfAbsent(server, ignored -> new LinkedHashMap<>());
            List<ResourceLocation> cached = byType.get(typeId);
            if (cached != null) {
                return cached;
            }
            Registry<Biome> registry = server.registryAccess().registryOrThrow(Registries.BIOME);
            List<ResourceLocation> result = new ArrayList<>();
            for (ResourceLocation biomeId : registry.keySet()) {
                Biome biome = registry.get(biomeId);
                if (biome == null) {
                    continue;
                }
                boolean present = biome.getMobSettings()
                        .getMobs(type.getCategory())
                        .unwrap()
                        .stream()
                        .map(entry -> entry.type)
                        .anyMatch(type::equals);
                if (present) {
                    result.add(biomeId);
                }
            }
            result.sort(Comparator.comparing(ResourceLocation::toString));
            List<ResourceLocation> immutable = List.copyOf(result);
            byType.put(typeId, immutable);
            return immutable;
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
}
