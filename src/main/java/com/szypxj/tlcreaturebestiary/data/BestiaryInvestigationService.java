package com.szypxj.tlcreaturebestiary.data;

import com.szypxj.tlcreaturebestiary.config.TCBConfig;
import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import com.szypxj.tldomesticatemorecreatures.api.progress.PlayerProgressApi;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class BestiaryInvestigationService {
    public static final int MAX_SPECIES_LENGTH = 128;
    public static final int MAX_DESCRIPTION_LENGTH = 4096;

    private BestiaryInvestigationService() {
    }

    public static boolean isHighRisk(LivingEntity target) {
        return BestiaryInvestigationRules.isHighRisk(target);
    }

    public static boolean isHighRisk(EntityType<?> type) {
        return BestiaryInvestigationRules.isHighRisk(type);
    }

    public static boolean isHighRisk(ResourceLocation entityTypeId) {
        return BestiaryInvestigationRules.isHighRisk(entityTypeId);
    }

    public static int baseDangerStars(LivingEntity target) {
        return target == null
                ? CreatureInfoApi.DANGER_MIN_STARS
                : BestiaryInvestigationRules.baseDangerStars(target.getType());
    }

    public static BestiaryEntryProgress progress(ServerPlayer player, EntityType<?> type) {
        ResourceLocation entityTypeId = type == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (player == null || entityTypeId == null) {
            return BestiaryEntryProgress.empty();
        }
        migrateLegacyIfNeeded(player, type, entityTypeId);
        return BestiaryData.progress(player, entityTypeId);
    }

    public static boolean recordObservation(ServerPlayer player, LivingEntity target, ResourceLocation entityTypeId) {
        if (player == null || target == null || entityTypeId == null || !isHighRisk(target)) {
            return false;
        }
        ResourceLocation targetEntityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (targetEntityTypeId == null || !entityTypeId.equals(targetEntityTypeId)) {
            return false;
        }
        migrateLegacyIfNeeded(player, target.getType(), entityTypeId);
        BestiaryEntryProgress current = BestiaryData.progress(player, entityTypeId);
        BestiaryEntryProgress updated;
        String promptKey;
        if (!current.observed()) {
            updated = current.withObserved(true);
            promptKey = "msg.tl_creature_bestiary.investigation.first_observation";
        } else if (current.combatRecorded() && !current.tamingRecorded()) {
            updated = current.withTamingRecorded(true);
            promptKey = "msg.tl_creature_bestiary.investigation.second_observation";
        } else {
            return false;
        }
        ensureVisible(player, entityTypeId);
        persistAndFinalize(player, entityTypeId, updated);
        player.displayClientMessage(Component.translatable(promptKey), true);
        return true;
    }

    public static boolean recordCombat(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null || !isHighRisk(target)) {
            return false;
        }
        ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (entityTypeId == null) {
            return false;
        }
        migrateLegacyIfNeeded(player, target.getType(), entityTypeId);
        BestiaryEntryProgress current = BestiaryData.progress(player, entityTypeId);
        if (current.combatRecorded()) {
            return false;
        }
        persistAndFinalize(player, entityTypeId, current.withCombatRecorded(true));
        return true;
    }


    public static boolean savePlayerNotes(
            ServerPlayer player,
            ResourceLocation entityTypeId,
            String species,
            String description
    ) {
        if (player == null || entityTypeId == null || !isHighRisk(entityTypeId)) {
            return false;
        }
        if (!BestiaryData.isUnlocked(player, entityTypeId)) {
            return false;
        }
        if (!isValidEditableText(species, MAX_SPECIES_LENGTH, false)
                || !isValidEditableText(description, MAX_DESCRIPTION_LENGTH, true)) {
            return false;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityTypeId);
        if (type == null) {
            return false;
        }
        migrateLegacyIfNeeded(player, type, entityTypeId);

        BestiaryEntryProgress current = BestiaryData.progress(player, entityTypeId);
        BestiaryEntryProgress updated = current.withPlayerNotes(species, description);
        BestiaryData.setProgress(player, entityTypeId, updated);
        finalizeCompletionReward(player, entityTypeId, updated);
        BestiaryNetwork.sendProgress(player, entityTypeId, BestiaryData.progress(player, entityTypeId), false);
        return true;
    }

    /**
     * Kept as a public idempotent entry point for UI/network callers. Normal investigation flow
     * also invokes the same finalizer automatically on every progress mutation.
     */
    public static boolean claimCompletionReward(ServerPlayer player, ResourceLocation entityTypeId) {
        if (player == null || entityTypeId == null || !isHighRisk(entityTypeId)) {
            return false;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityTypeId);
        if (type == null) {
            return false;
        }
        migrateLegacyIfNeeded(player, type, entityTypeId);
        boolean claimed = finalizeCompletionReward(player, entityTypeId, BestiaryData.progress(player, entityTypeId));
        if (claimed) {
            BestiaryNetwork.sendProgress(player, entityTypeId, BestiaryData.progress(player, entityTypeId));
        }
        return claimed;
    }

    public static void migrateLegacyIfNeeded(ServerPlayer player, EntityType<?> type, ResourceLocation entityTypeId) {
        if (player == null || type == null || entityTypeId == null || !isHighRisk(type)) {
            return;
        }
        if (BestiaryData.isUnlocked(player, entityTypeId) && !BestiaryData.hasProgress(player, entityTypeId)) {
            BestiaryData.setProgress(player, entityTypeId, BestiaryEntryProgress.empty().withObserved(true));
        }
    }

    private static void persistAndFinalize(
            ServerPlayer player,
            ResourceLocation entityTypeId,
            BestiaryEntryProgress updated
    ) {
        BestiaryData.setProgress(player, entityTypeId, updated);
        finalizeCompletionReward(player, entityTypeId, updated);
        BestiaryNetwork.sendProgress(player, entityTypeId, BestiaryData.progress(player, entityTypeId));
    }

    private static boolean finalizeCompletionReward(
            ServerPlayer player,
            ResourceLocation entityTypeId,
            BestiaryEntryProgress current
    ) {
        if (player == null || entityTypeId == null || current == null
                || !current.completed() || current.rewardClaimed()) {
            return false;
        }

        int rewardPoints = TCBConfig.HIGH_RISK_COMPLETION_REWARD_POINTS.get();
        if (rewardPoints <= 0) {
            BestiaryData.setProgress(player, entityTypeId, current.withRewardClaimed(true));
            player.sendSystemMessage(Component.translatable("msg.tl_creature_bestiary.investigation.completed"));
            return true;
        }

        if (!PlayerProgressApi.addUnspentAttributePoints(player, rewardPoints)) {
            return false;
        }

        BestiaryData.setProgress(player, entityTypeId, current.withRewardClaimed(true));
        player.sendSystemMessage(Component.translatable(
                "msg.tl_creature_bestiary.investigation.completed_reward",
                rewardPoints
        ));
        return true;
    }

    private static boolean isValidEditableText(String value, int maxLength, boolean multiline) {
        if (value == null || value.length() > maxLength) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isISOControl(c)) {
                continue;
            }
            if (c == '\t' || (multiline && (c == '\n' || c == '\r'))) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static void ensureVisible(ServerPlayer player, ResourceLocation entityTypeId) {
        if (BestiaryData.unlock(player, entityTypeId)) {
            BestiaryNetwork.sendUnlock(player, entityTypeId);
        }
    }
}
