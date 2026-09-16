package com.szypxj.tlcreaturebestiary.client;

import com.szypxj.tlcreaturebestiary.danger.DangerRating;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class DangerStarRenderer {
    private static final int LIT_COLOR = 0xFFF4FBFF;
    private static final int DIM_COLOR = 0xFF314A63;
    private static final int STAR_COUNT = 9;

    private DangerStarRenderer() {
    }

    public static int width(Font font) {
        return font == null ? 0 : font.width(star()) * STAR_COUNT;
    }

    public static void draw(GuiGraphics graphics, Font font, int x, int y, int rating) {
        if (graphics == null || font == null) {
            return;
        }
        int lit = Math.max(DangerRating.MIN_STARS, Math.min(DangerRating.MAX_STARS, rating));
        Component star = star();
        int cursor = x;
        int step = font.width(star);
        for (int i = 0; i < STAR_COUNT; i++) {
            graphics.drawString(font, star, cursor, y, i < lit ? LIT_COLOR : DIM_COLOR, true);
            cursor += step;
        }
    }

    private static Component star() {
        return Component.translatable("gui.tl_creature_bestiary.star");
    }
}
