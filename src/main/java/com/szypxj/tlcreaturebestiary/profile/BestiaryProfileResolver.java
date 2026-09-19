package com.szypxj.tlcreaturebestiary.profile;

import com.mojang.logging.LogUtils;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryDiet;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfile;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileConfidence;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContext;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContribution;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileField;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProviderRegistry;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileSource;
import com.szypxj.tlcreaturebestiary.info.EcologicalNiche;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class BestiaryProfileResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BestiaryProfileResolver() {
    }

    public static BestiaryProfile resolve(BestiaryProfileContext context) {
        if (context == null) {
            return BestiaryProfile.unavailable();
        }

        BestiaryProfileField<Component> species = null;
        BestiaryProfileField<BestiaryDiet> diet = null;
        BestiaryProfileField<Component> description = null;

        for (BestiaryProfileProvider provider : BestiaryProfileProviderRegistry.providers()) {
            BestiaryProfileContribution contribution;
            try {
                if (!provider.supports(context)) {
                    continue;
                }
                contribution = provider.provide(context);
            } catch (RuntimeException | LinkageError error) {
                LOGGER.warn("Bestiary profile provider {} failed for {}", provider.id(), context.entityTypeId(), error);
                continue;
            }
            if (contribution == null) {
                continue;
            }
            if (species == null && contribution.species() != null) {
                species = contribution.species();
            }
            if (diet == null && contribution.diet() != null) {
                diet = contribution.diet();
            }
            if (description == null && contribution.description() != null) {
                description = contribution.description();
            }
            if (species != null && diet != null && description != null) {
                break;
            }
        }

        if (species == null) {
            species = BestiaryProfile.unknownSpecies();
        }
        if (diet == null) {
            diet = BestiaryProfile.unknownDiet();
        }
        if (description == null) {
            description = generateDescription(context, species);
        }
        return new BestiaryProfile(species, diet, description);
    }

    private static BestiaryProfileField<Component> generateDescription(
            BestiaryProfileContext context,
            BestiaryProfileField<Component> species
    ) {
        int stars = CreatureInfoApi.dangerStars(context.baseStats());
        EcologicalNiche niche = EcologicalNiche.fromDangerStars(stars);
        Component tamingState = Component.translatable(context.tamingInfo().tameable()
                ? "gui.tl_creature_bestiary.profile.description.tameable"
                : "gui.tl_creature_bestiary.profile.description.not_tameable");
        Component text;
        if (context.biomeIds().isEmpty()) {
            text = Component.translatable(
                    "gui.tl_creature_bestiary.profile.description.generic_no_biomes",
                    species.value(),
                    stars,
                    Component.translatable(niche.translationKey()),
                    tamingState
            );
        } else {
            text = Component.translatable(
                    "gui.tl_creature_bestiary.profile.description.generic",
                    species.value(),
                    stars,
                    Component.translatable(niche.translationKey()),
                    tamingState,
                    context.biomeIds().size()
            );
        }
        return new BestiaryProfileField<>(
                text,
                BestiaryProfileSource.GENERIC_FALLBACK,
                BestiaryProfileConfidence.FALLBACK
        );
    }
}
