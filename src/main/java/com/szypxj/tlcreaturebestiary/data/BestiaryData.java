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
    private static final String INVESTIGATION_KEY = "investigation";

    private BestiaryData() {
    }

    public static Set<ResourceLocation> unlocked(Player player) {
        if (player == null) {
            return Set.of();
        }
        CompoundTag root = root(player);
        if (root == null) {
            return Set.of();
        }
        return decode(root);
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
        CompoundTag root = writableRoot(player);
        writeUnlocked(root, current);
        player.getPersistentData().put(ROOT_KEY, root);
        return true;
    }

    public static boolean hasProgress(Player player, ResourceLocation id) {
        if (player == null || id == null) {
            return false;
        }
        CompoundTag root = root(player);
        if (root == null || !root.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)) {
            return false;
        }
        return root.getCompound(INVESTIGATION_KEY).contains(id.toString(), Tag.TAG_COMPOUND);
    }

    public static BestiaryEntryProgress progress(Player player, ResourceLocation id) {
        if (player == null || id == null) {
            return BestiaryEntryProgress.empty();
        }
        CompoundTag root = root(player);
        if (root == null || !root.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)) {
            return BestiaryEntryProgress.empty();
        }
        CompoundTag investigation = root.getCompound(INVESTIGATION_KEY);
        String key = id.toString();
        if (!investigation.contains(key, Tag.TAG_COMPOUND)) {
            return BestiaryEntryProgress.empty();
        }
        return BestiaryEntryProgress.load(investigation.getCompound(key));
    }

    public static void setProgress(Player player, ResourceLocation id, BestiaryEntryProgress progress) {
        if (player == null || id == null || progress == null) {
            return;
        }
        CompoundTag root = writableRoot(player);
        CompoundTag investigation = root.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)
                ? root.getCompound(INVESTIGATION_KEY)
                : new CompoundTag();
        investigation.put(id.toString(), progress.save());
        root.put(INVESTIGATION_KEY, investigation);
        player.getPersistentData().put(ROOT_KEY, root);
    }

    public static void clearProgress(Player player, ResourceLocation id) {
        if (player == null || id == null) {
            return;
        }
        CompoundTag root = root(player);
        if (root == null || !root.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag writable = root.copy();
        CompoundTag investigation = writable.getCompound(INVESTIGATION_KEY);
        investigation.remove(id.toString());
        if (investigation.isEmpty()) {
            writable.remove(INVESTIGATION_KEY);
        } else {
            writable.put(INVESTIGATION_KEY, investigation);
        }
        if (writable.isEmpty()) {
            player.getPersistentData().remove(ROOT_KEY);
        } else {
            player.getPersistentData().put(ROOT_KEY, writable);
        }
    }

    public static void copy(Player from, Player to) {
        if (from == null || to == null) {
            return;
        }
        CompoundTag source = root(from);
        if (source == null || source.isEmpty()) {
            to.getPersistentData().remove(ROOT_KEY);
        } else {
            to.getPersistentData().put(ROOT_KEY, source.copy());
        }
    }

    public static CompoundTag encode(Set<ResourceLocation> ids) {
        CompoundTag root = new CompoundTag();
        writeUnlocked(root, ids);
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

    private static CompoundTag root(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        return persistent.getCompound(ROOT_KEY);
    }

    private static CompoundTag writableRoot(Player player) {
        CompoundTag root = root(player);
        return root == null ? new CompoundTag() : root.copy();
    }

    private static void writeUnlocked(CompoundTag root, Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        if (ids != null) {
            ids.stream()
                    .filter(java.util.Objects::nonNull)
                    .sorted(java.util.Comparator.comparing(ResourceLocation::toString))
                    .forEach(id -> list.add(StringTag.valueOf(id.toString())));
        }
        root.put(UNLOCKED_KEY, list);
    }
}
