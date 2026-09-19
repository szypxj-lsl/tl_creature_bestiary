package com.szypxj.tlcreaturebestiary.data;

import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class BestiaryUnlockService {
    private BestiaryUnlockService() {
    }

    public static boolean tryUnlock(ServerPlayer player, Entity entity) {
        if (player == null || entity == null || entity.level().isClientSide()) {
            return false;
        }
        if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
            return false;
        }
        CreatureInfoApi.getDangerRatingStats(living);
        if (BestiaryInvestigationService.isHighRisk(living)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return tryUnlock(player, id);
    }

    public static boolean tryUnlock(ServerPlayer player, ResourceLocation entityTypeId) {
        if (player == null || entityTypeId == null) {
            return false;
        }
        if (BestiaryData.unlock(player, entityTypeId)) {
            BestiaryNetwork.sendUnlock(player, entityTypeId);
            return true;
        }
        return false;
    }
}
