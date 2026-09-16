package com.szypxj.tlcreaturebestiary.data;

import com.szypxj.tlcreaturebestiary.TlCreatureBestiary;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.Set;

public final class BestiaryData {
    private static final String ROOT_KEY = TlCreatureBestiary.MOD_ID;
    private static final String UNLOCKED_KEY = "unlocked";

    private BestiaryData() {
    }

    public static Set<ResourceLocation> unlocked(Player player) {
        if (player == null) {
            return Set.of();
        }
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Set.of();
        }
        return decode(persistent.getCompound(ROOT_KEY));
    }

    public static boolean isUnlocked(Player player, ResourceLocation id) {
        return id != null && unlocked(player).contains(id);
    }

    public static boolean unlock(Player player, ResourceLocation id) {
        if (player == null || id == null) {
            return false;
        }
        Set<ResourceLocation> current = new LinkedHashSet<>(unlocked(player));
        if (!current.add(id)) {
            return false;
        }
        player.getPersistentData().put(ROOT_KEY, encode(current));
        return true;
    }

    public static void copy(Player from, Player to) {
        if (from == null || to == null) {
            return;
        }
        Set<ResourceLocation> source = unlocked(from);
        if (source.isEmpty()) {
            to.getPersistentData().remove(ROOT_KEY);
        } else {
            to.getPersistentData().put(ROOT_KEY, encode(source));
        }
    }

    public static CompoundTag encode(Set<ResourceLocation> ids) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        if (ids != null) {
            ids.stream()
                    .filter(java.util.Objects::nonNull)
                    .sorted(java.util.Comparator.comparing(ResourceLocation::toString))
                    .forEach(id -> list.add(StringTag.valueOf(id.toString())));
        }
        root.put(UNLOCKED_KEY, list);
        return root;
    }

    public static Set<ResourceLocation> decode(CompoundTag root) {
        if (root == null || !root.contains(UNLOCKED_KEY, Tag.TAG_LIST)) {
            return Set.of();
        }
        ListTag list = root.getList(UNLOCKED_KEY, Tag.TAG_STRING);
        Set<ResourceLocation> result = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                result.add(id);
            }
        }
        return Set.copyOf(result);
    }
}
