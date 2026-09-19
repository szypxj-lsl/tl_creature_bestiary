package com.szypxj.tlcreaturebestiary.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BestiaryDataTest {
    @Test
    void encodeDecodeDeduplicatesAndIgnoresMalformedIds() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        list.add(StringTag.valueOf("minecraft:zombie"));
        list.add(StringTag.valueOf("minecraft:zombie"));
        list.add(StringTag.valueOf("not an id"));
        root.put("unlocked", list);
        Set<ResourceLocation> decoded = BestiaryData.decode(root);
        assertEquals(Set.of(id("minecraft:zombie")), decoded);
    }

    @Test
    void encodeRoundTripPreservesSet() {
        Set<ResourceLocation> source = Set.of(
                id("minecraft:cow"),
                id("minecraft:zombie")
        );
        assertEquals(source, BestiaryData.decode(BestiaryData.encode(source)));
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
