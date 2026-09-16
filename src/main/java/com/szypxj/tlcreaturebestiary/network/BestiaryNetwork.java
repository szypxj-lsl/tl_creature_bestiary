package com.szypxj.tlcreaturebestiary.network;

import com.szypxj.tlcreaturebestiary.TlCreatureBestiary;
import com.szypxj.tlcreaturebestiary.data.BestiaryData;
import com.szypxj.tlcreaturebestiary.network.packet.S2CBestiaryFullSync;
import com.szypxj.tlcreaturebestiary.network.packet.S2CBestiaryUnlock;
import com.szypxj.tlcreaturebestiary.network.packet.C2SBestiaryDetailRequest;
import com.szypxj.tlcreaturebestiary.network.packet.S2CBestiaryDetail;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Objects;

public final class BestiaryNetwork {
    public static final String PROTOCOL = "2";
    public static final int S2C_FULL_SYNC_ID = 0;
    public static final int S2C_UNLOCK_ID = 1;
    public static final int C2S_DETAIL_REQUEST_ID = 2;
    public static final int S2C_DETAIL_ID = 3;

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
    }

    public static void sendFull(ServerPlayer player) {
        if (player != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CBestiaryFullSync(BestiaryData.unlocked(player)));
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

    public static void sendDetail(ServerPlayer player, S2CBestiaryDetail detail) {
        if (player != null && detail != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), detail);
        }
    }

}
