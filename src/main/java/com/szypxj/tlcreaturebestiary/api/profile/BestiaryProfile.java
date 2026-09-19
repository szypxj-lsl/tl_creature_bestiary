package com.szypxj.tlcreaturebestiary.api.profile;

import net.minecraft.network.chat.Component;

import java.util.Objects;

public record BestiaryProfile(
        BestiaryProfileField<Component> species,
        BestiaryProfileField<BestiaryDiet> diet,
        BestiaryProfileField<Component> description
) {
    public BestiaryProfile {
        species = Objects.requireNonNull(species, "species");
        diet = Objects.requireNonNull(diet, "diet");
        description = Objects.requireNonNull(description, "description");
    }

    public static BestiaryProfile unavailable() {
        return new BestiaryProfile(
                unknownSpecies(),
                unknownDiet(),
                unavailableDescription()
        );
    }

    public BestiaryProfile redactPlayerAuthoredFields() {
        return new BestiaryProfile(unknownSpecies(), diet, unavailableDescription());
    }

    public static BestiaryProfileField<Component> unknownSpecies() {
        return new BestiaryProfileField<>(
                Component.translatable("gui.tl_creature_bestiary.profile.species.unknown"),
                BestiaryProfileSource.UNKNOWN,
                BestiaryProfileConfidence.UNKNOWN
        );
    }

    public static BestiaryProfileField<BestiaryDiet> unknownDiet() {
        return new BestiaryProfileField<>(
                BestiaryDiet.UNKNOWN,
                BestiaryProfileSource.UNKNOWN,
                BestiaryProfileConfidence.UNKNOWN
        );
    }

    public static BestiaryProfileField<Component> unavailableDescription() {
        return new BestiaryProfileField<>(
                Component.translatable("gui.tl_creature_bestiary.profile.description.unavailable"),
                BestiaryProfileSource.UNKNOWN,
                BestiaryProfileConfidence.UNKNOWN
        );
    }
}
