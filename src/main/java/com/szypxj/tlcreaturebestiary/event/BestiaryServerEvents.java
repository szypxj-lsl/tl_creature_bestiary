package com.szypxj.tlcreaturebestiary.event;

import com.mojang.brigadier.Command;
import com.szypxj.tlcreaturebestiary.TlCreatureBestiary;
import com.szypxj.tlcreaturebestiary.data.BestiaryData;
import com.szypxj.tlcreaturebestiary.data.BestiaryUnlockService;
import com.szypxj.tlcreaturebestiary.info.LootInfoService;
import com.szypxj.tlcreaturebestiary.info.SpawnBiomeInfoService;
import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlCreatureBestiary.MOD_ID)
public final class BestiaryServerEvents {
    private BestiaryServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        refreshDangerRatingData(event.getServer());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tcb")
                        .then(Commands.literal("danger")
                                .then(Commands.literal("refresh")
                                        .executes(context -> refreshDangerRating(
                                                context.getSource().getServer(),
                                                context.getSource()
                                        ))))
        );
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        MinecraftServer server = event.getPlayerList().getServer();
        LootInfoService.clearCache(server);
        SpawnBiomeInfoService.clearCache(server);
        refreshDangerRatingData(server);
        syncAllPlayers(server);
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

    private static int refreshDangerRating(MinecraftServer server, CommandSourceStack source) {
        refreshDangerRatingData(server);
        syncAllPlayers(server);
        source.sendSuccess(
                () -> Component.translatable("command.tl_creature_bestiary.danger_refresh.success"),
                true
        );
        return Command.SINGLE_SUCCESS;
    }


    private static void refreshDangerRatingData(MinecraftServer server) {
        CreatureInfoApi.refreshDangerRatingData();
        if (server == null) {
            return;
        }
        for (var level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                    CreatureInfoApi.getDangerRatingStats(living);
                }
            }
        }
    }

    private static void syncAllPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BestiaryNetwork.sendFull(player);
        }
    }
}
