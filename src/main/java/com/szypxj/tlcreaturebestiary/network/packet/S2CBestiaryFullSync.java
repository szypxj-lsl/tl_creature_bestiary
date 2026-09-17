package com.szypxj.tlcreaturebestiary.network.packet;

import com.szypxj.tlcreaturebestiary.client.ClientBestiaryState;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public record S2CBestiaryFullSync(Set<ResourceLocation> unlocked) {
    public S2CBestiaryFullSync {
        unlocked = unlocked == null ? Set.of() : Set.copyOf(unlocked);
    }

    public static void encode(S2CBestiaryFullSync packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.unlocked().size());
        packet.unlocked().stream()
                .sorted(java.util.Comparator.comparing(ResourceLocation::toString))
                .forEach(buffer::writeResourceLocation);
    }

    public static S2CBestiaryFullSync decode(FriendlyByteBuf buffer) {
        int size = Math.max(0, buffer.readVarInt());
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            ids.add(buffer.readResourceLocation());
        }
        return new S2CBestiaryFullSync(ids);
    }

    public static void handle(S2CBestiaryFullSync packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    CreatureInfoApi.refreshDangerRatingData();
                    ClientBestiaryState.replace(packet.unlocked());
                }
        ));
        context.setPacketHandled(true);
    }
}
