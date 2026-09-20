package com.szypxj.tlcreaturebestiary;

import com.szypxj.tlcreaturebestiary.config.TCBConfig;
import com.szypxj.tlcreaturebestiary.data.BestiaryInvestigationService;
import com.szypxj.tlcreaturebestiary.data.BestiaryUnlockService;
import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tlcreaturebestiary.profile.BuiltinBestiaryProfiles;
import com.szypxj.tldomesticatemorecreatures.api.spyglass.SpyglassInspectionApi;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TlCreatureBestiary.MOD_ID)
public final class TlCreatureBestiary {
    public static final String MOD_ID = "tl_creature_bestiary";

    public TlCreatureBestiary(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, TCBConfig.SPEC);
        BuiltinBestiaryProfiles.register();
        BestiaryNetwork.register();
        SpyglassInspectionApi.registerScanListener(MOD_ID + ":investigation", event -> {
            if (BestiaryInvestigationService.isHighRisk(event.target())) {
                BestiaryInvestigationService.recordObservation(event.player(), event.target(), event.entityTypeId());
            } else {
                BestiaryUnlockService.tryUnlock(event.player(), event.target());
            }
        });
    }
}
