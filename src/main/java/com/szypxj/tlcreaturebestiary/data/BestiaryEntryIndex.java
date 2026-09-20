package com.szypxj.tlcreaturebestiary.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BestiaryEntryIndex {
    private BestiaryEntryIndex() {
    }

    public static List<ResourceLocation> allLivingTypeIds(Set<ResourceLocation> discovered) {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(type -> type != EntityType.PLAYER)
                .filter(DefaultAttributes::hasSupplier)
                .map(ForgeRegistries.ENTITY_TYPES::getKey)
                .filter(java.util.Objects::nonNull)
                .map(BestiaryCanonicalization::canonical)
                .filter(java.util.Objects::nonNull)
                .forEach(result::add);
        if (discovered != null) {
            discovered.stream().map(BestiaryCanonicalization::canonical).filter(java.util.Objects::nonNull).forEach(result::add);
        }
        result.removeIf(id -> {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            return type == null || type == EntityType.PLAYER;
        });
        return result.stream()
                .sorted(java.util.Comparator.comparing(ResourceLocation::toString))
                .toList();
    }
}
