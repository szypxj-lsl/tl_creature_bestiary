package com.szypxj.tlcreaturebestiary.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record BestiaryEntryProgress(
        boolean observed,
        boolean combatRecorded,
        boolean tamingRecorded,
        String species,
        String description,
        boolean rewardClaimed
) {
    public static final int MIN_DESCRIPTION_NON_WHITESPACE = 10;

    private static final String OBSERVED_KEY = "observed";
    private static final String COMBAT_RECORDED_KEY = "combatRecorded";
    private static final String TAMING_RECORDED_KEY = "tamingRecorded";
    private static final String SPECIES_KEY = "species";
    private static final String DESCRIPTION_KEY = "description";
    private static final String REWARD_CLAIMED_KEY = "rewardClaimed";

    // CP04-CP07 compatibility. These booleans were introduced by an incorrect interim
    // interpretation of the design. They are read only to avoid losing genuine combat data;
    // they must never manufacture player-authored species/description text.
    private static final String LEGACY_DROP_RECORDED_KEY = "dropRecorded";

    public BestiaryEntryProgress {
        species = species == null ? "" : species;
        description = description == null ? "" : description;
    }

    public static BestiaryEntryProgress empty() {
        return new BestiaryEntryProgress(false, false, false, "", "", false);
    }

    public boolean validSpecies() {
        return nonWhitespaceLength(species) >= 1;
    }

    public boolean validDescription() {
        return descriptionNonWhitespaceLength() >= MIN_DESCRIPTION_NON_WHITESPACE;
    }

    public int descriptionNonWhitespaceLength() {
        return nonWhitespaceLength(description);
    }

    public static int countNonWhitespace(String value) {
        return nonWhitespaceLength(value);
    }

    public int completionPercent() {
        int completed = 0;
        if (observed) {
            completed++;
        }
        if (combatRecorded) {
            completed++;
        }
        if (tamingRecorded) {
            completed++;
        }
        if (validSpecies()) {
            completed++;
        }
        if (validDescription()) {
            completed++;
        }
        return completed * 20;
    }

    public boolean completed() {
        return completionPercent() == 100;
    }

    public BestiaryEntryProgress withObserved(boolean value) {
        return new BestiaryEntryProgress(value, combatRecorded, tamingRecorded, species, description, rewardClaimed);
    }

    public BestiaryEntryProgress withCombatRecorded(boolean value) {
        return new BestiaryEntryProgress(observed, value, tamingRecorded, species, description, rewardClaimed);
    }

    public BestiaryEntryProgress withTamingRecorded(boolean value) {
        return new BestiaryEntryProgress(observed, combatRecorded, value, species, description, rewardClaimed);
    }

    public BestiaryEntryProgress withPlayerNotes(String newSpecies, String newDescription) {
        return new BestiaryEntryProgress(observed, combatRecorded, tamingRecorded, newSpecies, newDescription, rewardClaimed);
    }

    public BestiaryEntryProgress withRewardClaimed(boolean value) {
        return new BestiaryEntryProgress(observed, combatRecorded, tamingRecorded, species, description, value);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(OBSERVED_KEY, observed);
        tag.putBoolean(COMBAT_RECORDED_KEY, combatRecorded);
        tag.putBoolean(TAMING_RECORDED_KEY, tamingRecorded);
        tag.putString(SPECIES_KEY, species);
        tag.putString(DESCRIPTION_KEY, description);
        tag.putBoolean(REWARD_CLAIMED_KEY, rewardClaimed);
        return tag;
    }

    public static BestiaryEntryProgress load(CompoundTag tag) {
        if (tag == null) {
            return empty();
        }

        // The interim CP04-CP07 drop stage was always recorded together with combat.
        // Folding it into combat preserves real kill evidence without preserving the wrong sixth field.
        boolean combatRecorded = tag.getBoolean(COMBAT_RECORDED_KEY)
                || (tag.contains(LEGACY_DROP_RECORDED_KEY, Tag.TAG_BYTE) && tag.getBoolean(LEGACY_DROP_RECORDED_KEY));

        return new BestiaryEntryProgress(
                tag.getBoolean(OBSERVED_KEY),
                combatRecorded,
                tag.getBoolean(TAMING_RECORDED_KEY),
                tag.getString(SPECIES_KEY),
                tag.getString(DESCRIPTION_KEY),
                tag.getBoolean(REWARD_CLAIMED_KEY)
        );
    }

    private static int nonWhitespaceLength(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                count++;
            }
        }
        return count;
    }

}
