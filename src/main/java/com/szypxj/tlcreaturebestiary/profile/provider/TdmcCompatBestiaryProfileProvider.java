package com.szypxj.tlcreaturebestiary.profile.provider;

import com.szypxj.tlcreaturebestiary.api.profile.BestiaryDiet;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileConfidence;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContext;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileContribution;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileField;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileProvider;
import com.szypxj.tlcreaturebestiary.api.profile.BestiaryProfileSource;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureDiet;
import net.minecraft.network.chat.Component;

/** Generic bridge from TDMC's authoritative third-party compatibility metadata into TCB. */
public final class TdmcCompatBestiaryProfileProvider implements BestiaryProfileProvider {
    @Override
    public String id() {
        return "tl_creature_bestiary:tdmc_compat";
    }

    @Override
    public int priority() {
        return 900;
    }

    @Override
    public boolean supports(BestiaryProfileContext context) {
        if (context == null) {
            return false;
        }
        CreatureCompatProfile profile = CreatureInfoApi.getCompatProfile(
                context.level(), context.type(), context.languageCode());
        CreatureDiet resolvedDiet = CreatureInfoApi.getResolvedDiet(context.level(), context.type());
        return hasContribution(profile, context, resolvedDiet);
    }

    @Override
    public BestiaryProfileContribution provide(BestiaryProfileContext context) {
        CreatureCompatProfile profile = CreatureInfoApi.getCompatProfile(
                context.level(), context.type(), context.languageCode());
        CreatureDiet resolvedDiet = CreatureInfoApi.getResolvedDiet(context.level(), context.type());
        BestiaryProfileField<Component> species = hasText(profile.species())
                ? new BestiaryProfileField<>(profile.species(), BestiaryProfileSource.SHARED_API, BestiaryProfileConfidence.HIGH)
                : null;
        BestiaryProfileField<BestiaryDiet> diet = resolvedDiet != CreatureDiet.UNKNOWN
                ? new BestiaryProfileField<>(
                        mapDiet(resolvedDiet),
                        BestiaryProfileSource.SHARED_API,
                        profile.diet() != CreatureDiet.UNKNOWN
                                ? BestiaryProfileConfidence.AUTHORITATIVE
                                : BestiaryProfileConfidence.MEDIUM
                )
                : null;
        BestiaryProfileField<Component> description = hasText(profile.description())
                ? new BestiaryProfileField<>(profile.description(), BestiaryProfileSource.SHARED_API, BestiaryProfileConfidence.AUTHORITATIVE)
                : null;
        return new BestiaryProfileContribution(species, diet, description);
    }

    private static boolean hasContribution(CreatureCompatProfile profile, BestiaryProfileContext context, CreatureDiet resolvedDiet) {
        if (profile == null) {
            return false;
        }
        return resolvedDiet != CreatureDiet.UNKNOWN
                || hasText(profile.species())
                || hasText(profile.description())
                || profile.nativeTamingInfo().tameable()
                || !context.entityTypeId().equals(profile.canonicalEntityTypeId())
                || !context.entityTypeId().equals(profile.representativeEntityTypeId());
    }

    private static boolean hasText(Component component) {
        return component != null && !component.getString().isBlank();
    }

    private static BestiaryDiet mapDiet(CreatureDiet diet) {
        return switch (diet) {
            case CARNIVORE -> BestiaryDiet.CARNIVORE;
            case HERBIVORE -> BestiaryDiet.HERBIVORE;
            case OMNIVORE -> BestiaryDiet.OMNIVORE;
            case SCAVENGER -> BestiaryDiet.SCAVENGER;
            case UNKNOWN -> BestiaryDiet.UNKNOWN;
        };
    }
}
