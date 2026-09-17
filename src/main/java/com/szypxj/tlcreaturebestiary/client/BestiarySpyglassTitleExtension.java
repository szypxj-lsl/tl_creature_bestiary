package com.szypxj.tlcreaturebestiary.client;

import com.szypxj.tlcreaturebestiary.danger.DangerRating;
import com.szypxj.tldomesticatemorecreatures.api.client.SpyglassTitleContext;
import com.szypxj.tldomesticatemorecreatures.api.client.SpyglassTitleExtension;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public final class BestiarySpyglassTitleExtension implements SpyglassTitleExtension {
    @Override
    public int width(Font font, SpyglassTitleContext context) {
        if (!isValid(context)) {
            return 0;
        }
        return DangerStarRenderer.width(font);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, SpyglassTitleContext context, int x, int y) {
        if (!isValid(context)) {
            return;
        }
        DangerStarRenderer.draw(
                graphics,
                font,
                x,
                y,
                DangerRating.fromRadarScores(
                        context.radarPower(),
                        context.radarLife(),
                        context.radarSpeed(),
                        context.elite()
                )
        );
    }

    private static boolean isValid(SpyglassTitleContext context) {
        if (context == null || context.entityTypeId() == null) {
            return false;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(context.entityTypeId());
        return type != null && type != EntityType.PLAYER;
    }
}
