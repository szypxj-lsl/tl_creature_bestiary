package com.szypxj.tlcreaturebestiary.network.packet;

import com.szypxj.tlcreaturebestiary.data.BestiaryInvestigationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SBestiaryNotesSave(
        ResourceLocation entityTypeId,
        String species,
        String description
) {
    public C2SBestiaryNotesSave {
        species = species == null ? "" : species;
        description = description == null ? "" : description;
    }

    public static void encode(C2SBestiaryNotesSave packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.entityTypeId());
        buffer.writeUtf(packet.species(), BestiaryInvestigationService.MAX_SPECIES_LENGTH);
        buffer.writeUtf(packet.description(), BestiaryInvestigationService.MAX_DESCRIPTION_LENGTH);
    }

    public static C2SBestiaryNotesSave decode(FriendlyByteBuf buffer) {
        return new C2SBestiaryNotesSave(
                buffer.readResourceLocation(),
                buffer.readUtf(BestiaryInvestigationService.MAX_SPECIES_LENGTH),
                buffer.readUtf(BestiaryInvestigationService.MAX_DESCRIPTION_LENGTH)
        );
    }

    public static void handle(C2SBestiaryNotesSave packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> BestiaryInvestigationService.savePlayerNotes(
                context.getSender(),
                packet.entityTypeId(),
                packet.species(),
                packet.description()
        ));
        context.setPacketHandled(true);
    }
}
