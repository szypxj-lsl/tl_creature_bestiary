package com.szypxj.tlcreaturebestiary.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Objects;

public final class BestiaryRadarRenderer {
    private static final ResourceLocation RADAR_FRAME = Objects.requireNonNull(ResourceLocation.tryBuild(
            "tl_domesticate_more_creatures",
            "textures/gui/spyglass/radar_frame.png"
    ));
    private static final int RADAR_FRAME_WIDTH = 64;
    private static final int RADAR_FRAME_HEIGHT = 50;

    private BestiaryRadarRenderer() {
    }

    public static void render(GuiGraphics graphics, Font font, int x, int y, int width, int power, int life, int speed) {
        int centerX = x + width / 2;
        int centerY = y + 43;
        int radius = 31;
        int outerTopX = centerX;
        int outerTopY = centerY - radius;
        int outerLeftX = centerX - 27;
        int outerLeftY = centerY + 16;
        int outerRightX = centerX + 27;
        int outerRightY = centerY + 16;

        int powerX = axisPoint(centerX, outerTopX, power);
        int powerY = axisPoint(centerY, outerTopY, power);
        int lifeX = axisPoint(centerX, outerLeftX, life);
        int lifeY = axisPoint(centerY, outerLeftY, life);
        int speedX = axisPoint(centerX, outerRightX, speed);
        int speedY = axisPoint(centerY, outerRightY, speed);

        drawRadarFill(graphics.pose(), powerX, powerY, lifeX, lifeY, speedX, speedY);
        graphics.blit(
                RADAR_FRAME,
                centerX - RADAR_FRAME_WIDTH / 2,
                centerY - 31,
                0.0F,
                0.0F,
                RADAR_FRAME_WIDTH,
                RADAR_FRAME_HEIGHT,
                RADAR_FRAME_WIDTH,
                RADAR_FRAME_HEIGHT
        );
        drawRadarOutline(graphics.pose(), powerX, powerY, lifeX, lifeY, speedX, speedY);

        Component powerText = Component.translatable("gui.tl_creature_bestiary.power", power);
        Component lifeText = Component.translatable("gui.tl_creature_bestiary.life", life);
        Component speedText = Component.translatable("gui.tl_creature_bestiary.speed", speed);
        graphics.drawCenteredString(font, powerText, centerX, y + 1, 0xFFFFFF);
        graphics.drawString(font, lifeText, outerLeftX - font.width(lifeText), outerLeftY + 1, 0xFFFFFF, true);
        graphics.drawString(font, speedText, outerRightX + 2, outerRightY + 1, 0xFFFFFF, true);
    }

    private static int axisPoint(int center, int edge, int score) {
        double ratio = Mth.clamp(score / 100.0D, 0.0D, 1.0D);
        return (int) Math.round(center + (edge - center) * ratio);
    }

    private static void drawRadarFill(
            PoseStack poseStack,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, x1, y1, 0.0F).color(0.278F, 0.710F, 1.0F, 0.40F).endVertex();
        builder.vertex(matrix, x2, y2, 0.0F).color(0.278F, 0.710F, 1.0F, 0.40F).endVertex();
        builder.vertex(matrix, x3, y3, 0.0F).color(0.278F, 0.710F, 1.0F, 0.40F).endVertex();
        tesselator.end();
    }

    private static void drawRadarOutline(
            PoseStack poseStack,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3
    ) {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, x1, y1, 0.0F).color(0.498F, 0.859F, 1.0F, 1.0F).endVertex();
        builder.vertex(matrix, x2, y2, 0.0F).color(0.498F, 0.859F, 1.0F, 1.0F).endVertex();
        builder.vertex(matrix, x3, y3, 0.0F).color(0.498F, 0.859F, 1.0F, 1.0F).endVertex();
        builder.vertex(matrix, x1, y1, 0.0F).color(0.498F, 0.859F, 1.0F, 1.0F).endVertex();
        tesselator.end();
    }
}
