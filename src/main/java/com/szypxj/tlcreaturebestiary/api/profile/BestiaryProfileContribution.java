package com.szypxj.tlcreaturebestiary.api.profile;

import net.minecraft.network.chat.Component;

public record BestiaryProfileContribution(
        BestiaryProfileField<Component> species,
        BestiaryProfileField<BestiaryDiet> diet,
        BestiaryProfileField<Component> description
) {
    public static BestiaryProfileContribution empty() {
        return new BestiaryProfileContribution(null, null, null);
    }

    public static BestiaryProfileContribution ofSpecies(BestiaryProfileField<Component> species) {
        return new BestiaryProfileContribution(species, null, null);
    }
}
