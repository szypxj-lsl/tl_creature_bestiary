package com.szypxj.tlcreaturebestiary.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class BestiarySearchPolicy {
    private BestiarySearchPolicy() {
    }

    public static boolean matches(String query, boolean unlocked, String displayName, ResourceLocation id) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return true;
        }
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        String registryId = id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
        return name.contains(normalized) || registryId.contains(normalized);
    }
}
