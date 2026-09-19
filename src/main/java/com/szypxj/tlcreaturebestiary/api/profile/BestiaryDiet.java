package com.szypxj.tlcreaturebestiary.api.profile;

public enum BestiaryDiet {
    UNKNOWN("gui.tl_creature_bestiary.profile.diet.unknown"),
    CARNIVORE("gui.tl_creature_bestiary.profile.diet.carnivore"),
    HERBIVORE("gui.tl_creature_bestiary.profile.diet.herbivore"),
    OMNIVORE("gui.tl_creature_bestiary.profile.diet.omnivore"),
    SCAVENGER("gui.tl_creature_bestiary.profile.diet.scavenger");

    private final String translationKey;

    BestiaryDiet(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
