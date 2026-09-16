package com.szypxj.tlcreaturebestiary.network.packet;

import com.szypxj.tlcreaturebestiary.client.ClientBestiaryState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public record S2CBestiaryUnlock(ResourceLocation entityTypeId) {
    public static void encode(S2CBestiaryUnlock packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.entityTypeId());
    }

    public static S2CBestiaryUnlock decode(FriendlyByteBuf buffer) {
        return new S2CBestiaryUnlock(buffer.readResourceLocation());
    }

    public static void handle(S2CBestiaryUnlock packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    if (ClientBestiaryState.addUnlocked(packet.entityTypeId())) {
                        com.szypxj.tlcreaturebestiary.client.BestiaryClientBootstrap.onFirstUnlock(packet.entityTypeId());
                    }
                }
        ));
        context.setPacketHandled(true);
    }
}
