package com.szypxj.tlcreaturebestiary.network;

import com.szypxj.tlcreaturebestiary.TlCreatureBestiary;
import com.szypxj.tlcreaturebestiary.data.BestiaryData;
import com.szypxj.tlcreaturebestiary.data.BestiaryEntryProgress;
import com.szypxj.tlcreaturebestiary.data.BestiaryInvestigationService;
import com.szypxj.tlcreaturebestiary.network.packet.C2SBestiaryDetailRequest;
import com.szypxj.tlcreaturebestiary.network.packet.C2SBestiaryNotesSave;
import com.szypxj.tlcreaturebestiary.network.packet.S2CBestiaryDetail;
import com.szypxj.tlcreaturebestiary.network.packet.S2CBestiaryFullSync;
import com.szypxj.tlcreaturebestiary.network.packet.S2CBestiaryInvestigationProgress;
import com.szypxj.tlcreaturebestiary.network.packet.S2CBestiaryUnlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Set;

public final class BestiaryNetwork {
    public static final String PROTOCOL = "7";
    public static final int S2C_FULL_SYNC_ID = 0;
    public static final int S2C_UNLOCK_ID = 1;
    public static final int C2S_DETAIL_REQUEST_ID = 2;
    public static final int S2C_DETAIL_ID = 3;
    public static final int S2C_INVESTIGATION_PROGRESS_ID = 4;
    public static final int C2S_NOTES_SAVE_ID = 5;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Objects.requireNonNull(ResourceLocation.tryBuild(TlCreatureBestiary.MOD_ID, "main")),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static boolean registered;

    private BestiaryNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(S2CBestiaryFullSync.class, S2C_FULL_SYNC_ID, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBestiaryFullSync::encode)
                .decoder(S2CBestiaryFullSync::decode)
                .consumerMainThread(S2CBestiaryFullSync::handle)
                .add();
        CHANNEL.messageBuilder(S2CBestiaryUnlock.class, S2C_UNLOCK_ID, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBestiaryUnlock::encode)
                .decoder(S2CBestiaryUnlock::decode)
                .consumerMainThread(S2CBestiaryUnlock::handle)
                .add();
        CHANNEL.messageBuilder(C2SBestiaryDetailRequest.class, C2S_DETAIL_REQUEST_ID, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SBestiaryDetailRequest::encode)
                .decoder(C2SBestiaryDetailRequest::decode)
                .consumerMainThread(C2SBestiaryDetailRequest::handle)
                .add();
        CHANNEL.messageBuilder(S2CBestiaryDetail.class, S2C_DETAIL_ID, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBestiaryDetail::encode)
                .decoder(S2CBestiaryDetail::decode)
                .consumerMainThread(S2CBestiaryDetail::handle)
                .add();
        CHANNEL.messageBuilder(S2CBestiaryInvestigationProgress.class, S2C_INVESTIGATION_PROGRESS_ID, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBestiaryInvestigationProgress::encode)
                .decoder(S2CBestiaryInvestigationProgress::decode)
                .consumerMainThread(S2CBestiaryInvestigationProgress::handle)
                .add();
        CHANNEL.messageBuilder(C2SBestiaryNotesSave.class, C2S_NOTES_SAVE_ID, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SBestiaryNotesSave::encode)
                .decoder(C2SBestiaryNotesSave::decode)
                .consumerMainThread(C2SBestiaryNotesSave::handle)
                .add();
    }

    public static void sendFull(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Set<ResourceLocation> unlocked = BestiaryData.unlocked(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CBestiaryFullSync(unlocked));
        for (ResourceLocation id : unlocked) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type == null || !BestiaryInvestigationService.isHighRisk(type)) {
                continue;
            }
            BestiaryEntryProgress progress = BestiaryInvestigationService.progress(player, type);
            sendProgress(player, id, progress, false);
        }
    }

    public static void sendUnlock(ServerPlayer player, ResourceLocation id) {
        if (player != null && id != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CBestiaryUnlock(id));
        }
    }

    public static void requestDetail(ResourceLocation id) {
        if (id != null) {
            CHANNEL.sendToServer(new C2SBestiaryDetailRequest(id));
        }
    }

    public static void saveNotes(ResourceLocation id, String species, String description) {
        if (id != null) {
            CHANNEL.sendToServer(new C2SBestiaryNotesSave(id, species, description));
        }
    }

    public static void sendDetail(ServerPlayer player, S2CBestiaryDetail detail) {
        if (player != null && detail != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), detail);
        }
    }

    public static void sendProgress(ServerPlayer player, ResourceLocation id, BestiaryEntryProgress progress) {
        sendProgress(player, id, progress, true);
    }

    public static void sendProgress(
            ServerPlayer player,
            ResourceLocation id,
            BestiaryEntryProgress progress,
            boolean refreshDetail
    ) {
        if (player != null && id != null && progress != null) {
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new S2CBestiaryInvestigationProgress(id, progress, refreshDetail)
            );
        }
    }
}
