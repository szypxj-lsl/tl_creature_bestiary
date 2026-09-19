package com.szypxj.tlcreaturebestiary.info;

public enum EcologicalNiche {
    SMALL("gui.tl_creature_bestiary.investigation.niche.small"),
    LARGE("gui.tl_creature_bestiary.investigation.niche.large"),
    KING("gui.tl_creature_bestiary.investigation.niche.king"),
    ELDER_DRAGON("gui.tl_creature_bestiary.investigation.niche.elder_dragon"),
    UNSEALED("gui.tl_creature_bestiary.investigation.niche.unsealed"),
    FORBIDDEN("gui.tl_creature_bestiary.investigation.niche.forbidden");

    private final String translationKey;

    EcologicalNiche(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public static EcologicalNiche fromDangerStars(int stars) {
        if (stars >= 9) {
            return FORBIDDEN;
        }
        if (stars == 8) {
            return UNSEALED;
        }
        if (stars == 7) {
            return ELDER_DRAGON;
        }
        if (stars >= 5) {
            return KING;
        }
        if (stars >= 3) {
            return LARGE;
        }
        return SMALL;
    }
}
