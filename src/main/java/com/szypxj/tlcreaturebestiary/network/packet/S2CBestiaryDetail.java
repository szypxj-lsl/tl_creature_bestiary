package com.szypxj.tlcreaturebestiary.network.packet;

import com.szypxj.tlcreaturebestiary.client.ClientBestiaryState;
import com.szypxj.tlcreaturebestiary.info.DropInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStatsSource;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingFoodInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2CBestiaryDetail(
        ResourceLocation entityTypeId,
        BaseStats baseStats,
        TamingInfo tamingInfo,
        boolean rideable,
        List<DropInfo> drops,
        List<ResourceLocation> biomeIds
) {
    private static final int MAX_DROPS = 128;
    private static final int MAX_BIOMES = 1024;

    public S2CBestiaryDetail {
        baseStats = baseStats == null ? BaseStats.NONE : baseStats;
        tamingInfo = tamingInfo == null ? TamingInfo.NOT_TAMEABLE : tamingInfo;
        drops = drops == null ? List.of() : List.copyOf(drops.stream().limit(MAX_DROPS).toList());
        biomeIds = biomeIds == null ? List.of() : List.copyOf(biomeIds.stream().limit(MAX_BIOMES).toList());
    }

    public static void encode(S2CBestiaryDetail packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.entityTypeId());
        BaseStats stats = packet.baseStats();
        buffer.writeDouble(stats.maxHealth());
        buffer.writeDouble(stats.attackDamage());
        buffer.writeDouble(stats.movementSpeed());
        buffer.writeEnum(stats.source());
        TamingInfo taming = packet.tamingInfo();
        buffer.writeBoolean(taming.tameable());
        buffer.writeUtf(taming.methodName());
        buffer.writeVarInt(taming.requiredPlayerLevel());
        buffer.writeVarInt(taming.foods().size());
        for (TamingFoodInfo food : taming.foods()) {
            buffer.writeResourceLocation(food.itemId());
            buffer.writeVarInt(food.amount());
            buffer.writeBoolean(food.configured());
        }
        buffer.writeBoolean(packet.rideable());
        buffer.writeVarInt(packet.drops().size());
        for (DropInfo drop : packet.drops()) {
            buffer.writeResourceLocation(drop.itemId());
            buffer.writeVarInt(drop.minCount());
            buffer.writeInt(drop.maxCount());
            buffer.writeDouble(drop.chancePercent());
            buffer.writeBoolean(drop.killedByPlayer());
            buffer.writeBoolean(drop.lootingAffected());
            buffer.writeBoolean(drop.specialCondition());
        }
        buffer.writeVarInt(packet.biomeIds().size());
        for (ResourceLocation biomeId : packet.biomeIds()) {
            buffer.writeResourceLocation(biomeId);
        }
    }

    public static S2CBestiaryDetail decode(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        BaseStats stats = new BaseStats(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readEnum(BaseStatsSource.class)
        );
        boolean tameable = buffer.readBoolean();
        String method = buffer.readUtf();
        int requiredLevel = buffer.readVarInt();
        int foodCount = Math.max(0, Math.min(256, buffer.readVarInt()));
        List<TamingFoodInfo> foods = new ArrayList<>(foodCount);
        for (int i = 0; i < foodCount; i++) {
            foods.add(new TamingFoodInfo(
                    buffer.readResourceLocation(),
                    buffer.readVarInt(),
                    buffer.readBoolean()
            ));
        }
        boolean rideable = buffer.readBoolean();
        int dropCount = Math.max(0, Math.min(MAX_DROPS, buffer.readVarInt()));
        List<DropInfo> drops = new ArrayList<>(dropCount);
        for (int i = 0; i < dropCount; i++) {
            drops.add(new DropInfo(
                    buffer.readResourceLocation(),
                    buffer.readVarInt(),
                    buffer.readInt(),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            ));
        }
        int biomeCount = Math.max(0, Math.min(MAX_BIOMES, buffer.readVarInt()));
        List<ResourceLocation> biomeIds = new ArrayList<>(biomeCount);
        for (int i = 0; i < biomeCount; i++) {
            biomeIds.add(buffer.readResourceLocation());
        }
        return new S2CBestiaryDetail(
                id,
                stats,
                new TamingInfo(tameable, method, requiredLevel, foods),
                rideable,
                drops,
                biomeIds
        );
    }

    public static void handle(S2CBestiaryDetail packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientBestiaryState.putDetail(
                        packet.entityTypeId(),
                        packet.baseStats(),
                        packet.tamingInfo(),
                        packet.rideable(),
                        packet.drops(),
                        packet.biomeIds()
                )
        ));
        context.setPacketHandled(true);
    }
}
