package com.szypxj.tlcreaturebestiary.client;

import com.szypxj.tlcreaturebestiary.TlCreatureBestiary;
import com.szypxj.tlcreaturebestiary.client.screen.BestiaryScreen;
import com.szypxj.tldomesticatemorecreatures.api.client.PanelAction;
import com.szypxj.tldomesticatemorecreatures.api.client.PanelActionRegistry;
import com.szypxj.tldomesticatemorecreatures.api.client.SpyglassTitleExtensionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TlCreatureBestiary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BestiaryClientBootstrap {
    private static boolean registered;

    private BestiaryClientBootstrap() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(BestiaryClientBootstrap::registerClientExtensions);
    }

    private static synchronized void registerClientExtensions() {
        if (registered) {
            return;
        }
        registered = true;
        SpyglassTitleExtensionRegistry.register(
                TlCreatureBestiary.MOD_ID + ":danger_rating",
                new BestiarySpyglassTitleExtension()
        );
        PanelActionRegistry.register(new PanelAction(
                TlCreatureBestiary.MOD_ID + ":bestiary",
                "gui.tl_creature_bestiary.open",
                72,
                () -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.setScreen(new BestiaryScreen(minecraft.screen));
                }
        ));
    }

    public static void onFirstUnlock(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || id == null) {
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) {
            return;
        }
        minecraft.gui.setOverlayMessage(
                Component.translatable("msg.tl_creature_bestiary.unlocked", type.getDescription()),
                false
        );
        minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.35F, 1.15F);
    }
}
