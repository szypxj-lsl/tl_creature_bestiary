package com.szypxj.tlcreaturebestiary.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BestiarySearchPolicyTest {
    @Test
    void lockedRowsRemainSearchableBecauseTheirNamesAreVisible() {
        ResourceLocation unlocked = new ResourceLocation("minecraft", "cow");
        ResourceLocation locked = new ResourceLocation("examplemod", "secret_boss");
        assertTrue(BestiarySearchPolicy.matches("", false, "Secret Boss", locked));
        assertTrue(BestiarySearchPolicy.matches("cow", true, "Cow", unlocked));
        assertTrue(BestiarySearchPolicy.matches("minecraft:cow", true, "Cow", unlocked));
        assertTrue(BestiarySearchPolicy.matches("secret", false, "Secret Boss", locked));
        assertTrue(BestiarySearchPolicy.matches("examplemod", false, "Secret Boss", locked));
    }
}
