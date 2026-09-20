package com.szypxj.tlcreaturebestiary.data;

import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import net.minecraft.resources.ResourceLocation;

public final class BestiaryCanonicalization {
    private BestiaryCanonicalization() {
    }

    public static ResourceLocation canonical(ResourceLocation entityTypeId) {
        return entityTypeId == null ? null : CreatureInfoApi.canonicalEntityTypeId(entityTypeId);
    }
}
