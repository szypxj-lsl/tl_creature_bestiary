package com.szypxj.tlcreaturebestiary;

import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tlcreaturebestiary.data.BestiaryUnlockService;
import com.szypxj.tldomesticatemorecreatures.api.spyglass.SpyglassInspectionApi;
import net.minecraftforge.fml.common.Mod;

@Mod(TlCreatureBestiary.MOD_ID)
public final class TlCreatureBestiary {
    public static final String MOD_ID = "tl_creature_bestiary";

    public TlCreatureBestiary() {
        BestiaryNetwork.register();
        SpyglassInspectionApi.registerScanListener(MOD_ID + ":unlock", event ->
                BestiaryUnlockService.tryUnlock(event.player(), event.entityTypeId()));
    }
}
