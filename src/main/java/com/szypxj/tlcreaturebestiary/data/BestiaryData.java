package com.szypxj.tlcreaturebestiary.data;

import com.szypxj.tlcreaturebestiary.TlCreatureBestiary;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class BestiaryData {
    private static final String ROOT_KEY = TlCreatureBestiary.MOD_ID;
    private static final String UNLOCKED_KEY = "unlocked";
    private static final String INVESTIGATION_KEY = "investigation";

    private BestiaryData() {
    }

    public static Set<ResourceLocation> unlocked(Player player) {
        if (player == null) return Set.of();
        migrateCanonicalEntries(player);
        return unlockedRaw(player);
    }

    public static boolean isUnlocked(Player player, ResourceLocation id) {
        ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
        return canonical != null && unlocked(player).contains(canonical);
    }

    public static boolean unlock(Player player, ResourceLocation id) {
        if (player == null || id == null) return false;
        ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
        Set<ResourceLocation> current = new LinkedHashSet<>(unlocked(player));
        if (!current.add(canonical)) return false;
        CompoundTag root = writableRoot(player);
        writeUnlocked(root, current);
        player.getPersistentData().put(ROOT_KEY, root);
        return true;
    }

    public static boolean hasProgress(Player player, ResourceLocation id) {
        if (player == null || id == null) return false;
        migrateCanonicalEntries(player);
        ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
        CompoundTag root = root(player);
        return root != null
                && root.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)
                && root.getCompound(INVESTIGATION_KEY).contains(canonical.toString(), Tag.TAG_COMPOUND);
    }

    public static BestiaryEntryProgress progress(Player player, ResourceLocation id) {
        if (player == null || id == null) return BestiaryEntryProgress.empty();
        migrateCanonicalEntries(player);
        ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
        CompoundTag root = root(player);
        if (root == null || !root.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)) return BestiaryEntryProgress.empty();
        CompoundTag investigation = root.getCompound(INVESTIGATION_KEY);
        return investigation.contains(canonical.toString(), Tag.TAG_COMPOUND)
                ? BestiaryEntryProgress.load(investigation.getCompound(canonical.toString()))
                : BestiaryEntryProgress.empty();
    }

    public static void setProgress(Player player, ResourceLocation id, BestiaryEntryProgress progress) {
        if (player == null || id == null || progress == null) return;
        migrateCanonicalEntries(player);
        ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
        CompoundTag root = writableRoot(player);
        CompoundTag investigation = root.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)
                ? root.getCompound(INVESTIGATION_KEY)
                : new CompoundTag();
        investigation.put(canonical.toString(), progress.save());
        root.put(INVESTIGATION_KEY, investigation);
        player.getPersistentData().put(ROOT_KEY, root);
    }

    public static void clearProgress(Player player, ResourceLocation id) {
        if (player == null || id == null) return;
        migrateCanonicalEntries(player);
        ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
        CompoundTag root = root(player);
        if (root == null || !root.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)) return;
        CompoundTag writable = root.copy();
        CompoundTag investigation = writable.getCompound(INVESTIGATION_KEY);
        investigation.remove(canonical.toString());
        if (investigation.isEmpty()) writable.remove(INVESTIGATION_KEY); else writable.put(INVESTIGATION_KEY, investigation);
        if (writable.isEmpty()) player.getPersistentData().remove(ROOT_KEY); else player.getPersistentData().put(ROOT_KEY, writable);
    }

    public static void copy(Player from, Player to) {
        if (from == null || to == null) return;
        CompoundTag source = root(from);
        if (source == null || source.isEmpty()) to.getPersistentData().remove(ROOT_KEY); else to.getPersistentData().put(ROOT_KEY, source.copy());
        migrateCanonicalEntries(to);
    }

    public static void migrateCanonicalEntries(Player player) {
        if (player == null) return;
        CompoundTag source = root(player);
        if (source == null) return;
        CompoundTag migrated = source.copy();
        boolean changed = false;

        Set<ResourceLocation> canonicalUnlocked = new LinkedHashSet<>();
        for (ResourceLocation id : decode(source)) {
            ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
            canonicalUnlocked.add(canonical);
            changed |= !canonical.equals(id);
        }
        writeUnlocked(migrated, canonicalUnlocked);

        if (source.contains(INVESTIGATION_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag original = source.getCompound(INVESTIGATION_KEY);
            CompoundTag rewritten = new CompoundTag();
            Map<ResourceLocation, BestiaryEntryProgress> merged = new LinkedHashMap<>();

            for (String key : original.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null || !original.contains(key, Tag.TAG_COMPOUND)) {
                    rewritten.put(key, original.get(key).copy());
                    continue;
                }
                ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
                if (canonical.equals(id)) {
                    merged.put(canonical, BestiaryEntryProgress.load(original.getCompound(key)));
                }
            }
            for (String key : original.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null || !original.contains(key, Tag.TAG_COMPOUND)) continue;
                ResourceLocation canonical = BestiaryCanonicalization.canonical(id);
                if (canonical.equals(id)) continue;
                BestiaryEntryProgress legacy = BestiaryEntryProgress.load(original.getCompound(key));
                BestiaryEntryProgress current = merged.getOrDefault(canonical, BestiaryEntryProgress.empty());
                merged.put(canonical, mergeCanonical(current, legacy));
                changed = true;
            }
            merged.forEach((id, progress) -> rewritten.put(id.toString(), progress.save()));
            migrated.put(INVESTIGATION_KEY, rewritten);
        }
        if (changed) player.getPersistentData().put(ROOT_KEY, migrated);
    }

    private static BestiaryEntryProgress mergeCanonical(BestiaryEntryProgress canonical, BestiaryEntryProgress legacy) {
        String species = !canonical.species().isBlank() ? canonical.species() : legacy.species();
        String description = !canonical.description().isBlank() ? canonical.description() : legacy.description();
        return new BestiaryEntryProgress(
                canonical.observed() || legacy.observed(),
                canonical.combatRecorded() || legacy.combatRecorded(),
                canonical.tamingRecorded() || legacy.tamingRecorded(),
                species,
                description,
                canonical.rewardClaimed() || legacy.rewardClaimed()
        );
    }

    public static CompoundTag encode(Set<ResourceLocation> ids) {
        CompoundTag root = new CompoundTag();
        writeUnlocked(root, ids);
        return root;
    }

    public static Set<ResourceLocation> decode(CompoundTag root) {
        if (root == null || !root.contains(UNLOCKED_KEY, Tag.TAG_LIST)) return Set.of();
        ListTag list = root.getList(UNLOCKED_KEY, Tag.TAG_STRING);
        Set<ResourceLocation> result = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) result.add(id);
        }
        return Set.copyOf(result);
    }

    private static Set<ResourceLocation> unlockedRaw(Player player) {
        CompoundTag root = root(player);
        return root == null ? Set.of() : decode(root);
    }

    private static CompoundTag root(Player player) {
        CompoundTag persistent = player.getPersistentData();
        return persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND) ? persistent.getCompound(ROOT_KEY) : null;
    }

    private static CompoundTag writableRoot(Player player) {
        CompoundTag root = root(player);
        return root == null ? new CompoundTag() : root.copy();
    }

    private static void writeUnlocked(CompoundTag root, Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        if (ids != null) ids.stream().filter(java.util.Objects::nonNull).sorted(java.util.Comparator.comparing(ResourceLocation::toString)).forEach(id -> list.add(StringTag.valueOf(id.toString())));
        root.put(UNLOCKED_KEY, list);
    }
}
