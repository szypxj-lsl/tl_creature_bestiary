package com.szypxj.tlcreaturebestiary.client;

import com.szypxj.tlcreaturebestiary.info.DropInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClientBestiaryState {
    private static final Set<ResourceLocation> UNLOCKED = new LinkedHashSet<>();
    private static final Map<ResourceLocation, Detail> DETAILS = new LinkedHashMap<>();

    private ClientBestiaryState() {
    }

    public static synchronized void replace(Set<ResourceLocation> ids) {
        UNLOCKED.clear();
        DETAILS.clear();
        if (ids != null) {
            UNLOCKED.addAll(ids);
        }
    }

    public static synchronized boolean addUnlocked(ResourceLocation id) {
        return id != null && UNLOCKED.add(id);
    }

    public static synchronized boolean isUnlocked(ResourceLocation id) {
        return id != null && UNLOCKED.contains(id);
    }

    public static synchronized Set<ResourceLocation> unlocked() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(UNLOCKED));
    }

    public static synchronized void clear() {
        UNLOCKED.clear();
        clearDetails();
    }

    public static synchronized void putDetail(
            ResourceLocation id,
            BaseStats stats,
            TamingInfo taming,
            boolean rideable,
            List<DropInfo> drops,
            List<ResourceLocation> biomeIds
    ) {
        if (id != null) {
            DETAILS.put(id, new Detail(
                    stats == null ? BaseStats.NONE : stats,
                    taming == null ? TamingInfo.NOT_TAMEABLE : taming,
                    rideable,
                    drops == null ? List.of() : List.copyOf(drops),
                    biomeIds == null ? List.of() : List.copyOf(biomeIds)
            ));
        }
    }

    public static synchronized Detail detail(ResourceLocation id) {
        return id == null ? null : DETAILS.get(id);
    }

    public static synchronized void removeDetail(ResourceLocation id) {
        if (id != null) {
            DETAILS.remove(id);
        }
    }

    public static synchronized void clearDetails() {
        DETAILS.clear();
    }

    public record Detail(
            BaseStats baseStats,
            TamingInfo tamingInfo,
            boolean rideable,
            List<DropInfo> drops,
            List<ResourceLocation> biomeIds
    ) {
        public Detail {
            drops = drops == null ? List.of() : List.copyOf(drops);
            biomeIds = biomeIds == null ? List.of() : List.copyOf(biomeIds);
        }
    }
}
