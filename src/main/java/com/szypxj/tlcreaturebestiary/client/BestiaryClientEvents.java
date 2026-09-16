package com.szypxj.tlcreaturebestiary.client;

import com.szypxj.tlcreaturebestiary.TlCreatureBestiary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlCreatureBestiary.MOD_ID, value = Dist.CLIENT)
public final class BestiaryClientEvents {
    private BestiaryClientEvents() {
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBestiaryState.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientBestiaryState.clearDetails();
        }
    }
}
