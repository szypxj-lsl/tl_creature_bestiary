package com.szypxj.tlcreaturebestiary.data;

import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class BestiaryInvestigationRules {
    public static final int HIGH_RISK_MIN_STARS = 7;

    private BestiaryInvestigationRules() {
    }

    public static boolean isHighRisk(LivingEntity target) {
        return target != null && isHighRisk(target.getType());
    }

    public static boolean isHighRisk(EntityType<?> type) {
        return type != null && baseDangerStars(type) >= HIGH_RISK_MIN_STARS;
    }

    public static boolean isHighRisk(ResourceLocation entityTypeId) {
        return entityTypeId != null && isHighRisk(ForgeRegistries.ENTITY_TYPES.getValue(entityTypeId));
    }

    public static int baseDangerStars(EntityType<?> type) {
        if (type == null) {
            return CreatureInfoApi.DANGER_MIN_STARS;
        }
        return CreatureInfoApi.dangerStars(CreatureInfoApi.getDangerRatingStats(type));
    }
}
