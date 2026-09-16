package com.szypxj.tlcreaturebestiary.event;

import com.szypxj.tlcreaturebestiary.TlCreatureBestiary;
import com.szypxj.tlcreaturebestiary.data.BestiaryData;
import com.szypxj.tlcreaturebestiary.data.BestiaryUnlockService;
import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tlcreaturebestiary.info.LootInfoService;
import com.szypxj.tlcreaturebestiary.info.SpawnBiomeInfoService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlCreatureBestiary.MOD_ID)
public final class BestiaryServerEvents {
    private BestiaryServerEvents() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        LootInfoService.clearCache(event.getPlayerList().getServer());
        SpawnBiomeInfoService.clearCache(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        BestiaryData.copy(event.getOriginal(), event.getEntity());
    }


    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BestiaryUnlockService.tryUnlock(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onEntityTouch(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getHand() == InteractionHand.MAIN_HAND) {
            BestiaryUnlockService.tryUnlock(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onEntityTouchSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getHand() == InteractionHand.MAIN_HAND) {
            BestiaryUnlockService.tryUnlock(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onPlayerHurtByCreature(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSource().getEntity() != null) {
            BestiaryUnlockService.tryUnlock(player, event.getSource().getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerKilledByCreature(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSource().getEntity() != null) {
            BestiaryUnlockService.tryUnlock(player, event.getSource().getEntity());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BestiaryNetwork.sendFull(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BestiaryNetwork.sendFull(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BestiaryNetwork.sendFull(player);
        }
    }
}
