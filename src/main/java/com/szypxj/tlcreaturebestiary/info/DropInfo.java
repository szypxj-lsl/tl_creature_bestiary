package com.szypxj.tlcreaturebestiary.info;

import net.minecraft.resources.ResourceLocation;

public record DropInfo(
        ResourceLocation itemId,
        int minCount,
        int maxCount,
        double chancePercent,
        boolean killedByPlayer,
        boolean lootingAffected,
        boolean specialCondition
) {
    public DropInfo {
        minCount = Math.max(0, minCount);
        maxCount = maxCount < 0 ? -1 : Math.max(minCount, maxCount);
        chancePercent = chancePercent < 0.0D ? -1.0D : Math.min(100.0D, chancePercent);
    }

    public boolean exactChance() {
        return chancePercent >= 0.0D;
    }
}
