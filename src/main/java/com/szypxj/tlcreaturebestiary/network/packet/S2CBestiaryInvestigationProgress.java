package com.szypxj.tlcreaturebestiary.network.packet;

import com.szypxj.tlcreaturebestiary.client.ClientBestiaryState;
import com.szypxj.tlcreaturebestiary.data.BestiaryEntryProgress;
import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CBestiaryInvestigationProgress(
        ResourceLocation entityTypeId,
        BestiaryEntryProgress progress,
        boolean refreshDetail
) {
    private static final int MAX_SPECIES_LENGTH = 128;
    private static final int MAX_DESCRIPTION_LENGTH = 4096;

    public S2CBestiaryInvestigationProgress {
        progress = progress == null ? BestiaryEntryProgress.empty() : progress;
    }

    public static void encode(S2CBestiaryInvestigationProgress packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.entityTypeId());
        buffer.writeBoolean(packet.progress().observed());
        buffer.writeBoolean(packet.progress().combatRecorded());
        buffer.writeBoolean(packet.progress().tamingRecorded());
        buffer.writeUtf(packet.progress().species(), MAX_SPECIES_LENGTH);
        buffer.writeUtf(packet.progress().description(), MAX_DESCRIPTION_LENGTH);
        buffer.writeBoolean(packet.progress().rewardClaimed());
        buffer.writeBoolean(packet.refreshDetail());
    }

    public static S2CBestiaryInvestigationProgress decode(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        BestiaryEntryProgress progress = new BestiaryEntryProgress(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_SPECIES_LENGTH),
                buffer.readUtf(MAX_DESCRIPTION_LENGTH),
                buffer.readBoolean()
        );
        return new S2CBestiaryInvestigationProgress(id, progress, buffer.readBoolean());
    }

    public static void handle(
            S2CBestiaryInvestigationProgress packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    ClientBestiaryState.putProgress(packet.entityTypeId(), packet.progress());
                    if (!packet.refreshDetail()) {
                        return;
                    }
                    ClientBestiaryState.removeDetail(packet.entityTypeId());
                    if (ClientBestiaryState.isUnlocked(packet.entityTypeId())) {
                        BestiaryNetwork.requestDetail(packet.entityTypeId());
                    }
                }
        ));
        context.setPacketHandled(true);
    }
}
