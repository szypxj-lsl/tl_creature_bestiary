package com.szypxj.tlcreaturebestiary.profile.provider;

import com.szypxj.tlcreaturebestiary.api.profile.BestiaryDiet;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileConfidence;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContext;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContribution;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileField;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MobCategory;

public final class GenericBestiaryProfileProvider implements BestiaryProfileProvider {
    @Override
    public String id() {
        return "tl_creature_bestiary:generic";
    }

    @Override
    public int priority() {
        return -1000;
    }

    @Override
    public boolean supports(BestiaryProfileContext context) {
        return context != null;
    }

    @Override
    public BestiaryProfileContribution provide(BestiaryProfileContext context) {
        String speciesKey = speciesKey(context.type().getCategory());
        return new BestiaryProfileContribution(
                new BestiaryProfileField<>(
                        Component.translatable(speciesKey),
                        BestiaryProfileSource.GENERIC_FALLBACK,
                        BestiaryProfileConfidence.FALLBACK
                ),
                new BestiaryProfileField<>(
                        BestiaryDiet.UNKNOWN,
                        BestiaryProfileSource.GENERIC_FALLBACK,
                        BestiaryProfileConfidence.UNKNOWN
                ),
                null
        );
    }

    private static String speciesKey(MobCategory category) {
        if (category == null) {
            return "gui.tl_creature_bestiary.profile.species.other";
        }
        return switch (category) {
            case MONSTER -> "gui.tl_creature_bestiary.profile.species.hostile";
            case CREATURE -> "gui.tl_creature_bestiary.profile.species.land_animal";
            case AMBIENT -> "gui.tl_creature_bestiary.profile.species.ambient";
            case AXOLOTLS, UNDERGROUND_WATER_CREATURE, WATER_CREATURE, WATER_AMBIENT ->
                    "gui.tl_creature_bestiary.profile.species.aquatic";
            case MISC -> "gui.tl_creature_bestiary.profile.species.other";
        };
    }
}
