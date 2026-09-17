package com.szypxj.tlcreaturebestiary.network.packet;

import com.szypxj.tlcreaturebestiary.data.BestiaryData;
import com.szypxj.tlcreaturebestiary.info.DropInfo;
import com.szypxj.tlcreaturebestiary.info.LootInfoService;
import com.szypxj.tlcreaturebestiary.info.SpawnBiomeInfoService;
import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public record C2SBestiaryDetailRequest(ResourceLocation entityTypeId) {
    public static void encode(C2SBestiaryDetailRequest packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.entityTypeId());
    }

    public static C2SBestiaryDetailRequest decode(FriendlyByteBuf buffer) {
        return new C2SBestiaryDetailRequest(buffer.readResourceLocation());
    }

    public static void handle(C2SBestiaryDetailRequest packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            ResourceLocation id = packet.entityTypeId();
            if (sender == null || id == null || !BestiaryData.isUnlocked(sender, id)) {
                return;
            }
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type == null || type == EntityType.PLAYER) {
                return;
            }
            BaseStats stats = CreatureInfoApi.getDangerRatingStats(type);
            TamingInfo taming = CreatureInfoApi.getTamingInfo(sender.serverLevel(), type);
            boolean rideable = CreatureInfoApi.isRideable(sender.serverLevel(), type);
            List<DropInfo> drops = LootInfoService.describe(sender.serverLevel().getServer(), type);
            List<ResourceLocation> biomes = SpawnBiomeInfoService.findNaturalSpawnBiomes(sender.serverLevel().getServer(), type);
            BestiaryNetwork.sendDetail(sender, new S2CBestiaryDetail(id, stats, taming, rideable, drops, biomes));
        });
        context.setPacketHandled(true);
    }
}
