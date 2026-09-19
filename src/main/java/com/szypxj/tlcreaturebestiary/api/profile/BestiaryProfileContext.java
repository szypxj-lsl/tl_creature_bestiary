package com.szypxj.tlcreaturebestiary.api.profile;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record BestiaryProfileContext(
        ServerLevel level,
        ResourceLocation entityTypeId,
        EntityType<?> type,
        BaseStats baseStats,
        TamingInfo tamingInfo,
        boolean rideable,
        List<ResourceLocation> biomeIds,
        String languageCode
) {
    public BestiaryProfileContext {
        level = Objects.requireNonNull(level, "level");
        entityTypeId = Objects.requireNonNull(entityTypeId, "entityTypeId");
        type = Objects.requireNonNull(type, "type");
        baseStats = baseStats == null ? BaseStats.NONE : baseStats;
        tamingInfo = tamingInfo == null ? TamingInfo.NOT_TAMEABLE : tamingInfo;
        biomeIds = biomeIds == null ? List.of() : List.copyOf(biomeIds);
        languageCode = normalizeLanguage(languageCode);
    }

    private static String normalizeLanguage(String value) {
        if (value == null || value.isBlank()) {
            return "en_us";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.matches("[a-z0-9_]+") ? normalized : "en_us";
    }
}
