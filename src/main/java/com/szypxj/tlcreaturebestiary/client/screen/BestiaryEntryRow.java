package com.szypxj.tlcreaturebestiary.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record BestiaryEntryRow(ResourceLocation entityTypeId, boolean unlocked, Component label) {
}
