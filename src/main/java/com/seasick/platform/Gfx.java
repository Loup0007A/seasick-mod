package com.seasick.platform;

import com.mojang.math.Axis;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;

/**
 * Dessin dans l'interface. Les signatures sont les mêmes de 1.20.1 à 1.21.1 ;
 * Minecraft 26.x (GuiGraphicsExtractor, pipelines, pose 2D) a sa version dans targets/neoforge-26.3/overrides.
 * Toutes les couleurs sont en ARGB avec un alpha explicite.
 */
public final class Gfx {

    private Gfx() {}

    public static void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        g.fill(x0, y0, x1, y1, argb);
    }

    public static void gradient(GuiGraphics g, int x0, int y0, int x1, int y1, int top, int bottom) {
        g.fillGradient(x0, y0, x1, y1, top, bottom);
    }

    private static final java.util.Map<ResourceLocation, Object> SMOOTHED = new java.util.HashMap<>();

    /** Filtrage linéaire : les textures du mod sont dessinées à 2-4 fois la densité de l'interface. */
    private static void smooth(ResourceLocation tex) {
        var t = net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(tex);
        if (SMOOTHED.get(tex) != t) {
            t.setFilter(true, false);
            SMOOTHED.put(tex, t);
        }
    }

    /** Région (u, v, rw, rh) d'une texture de taille (tw, th) étirée sur le rectangle (x, y, w, h), teintée. */
    public static void texture(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h,
                               float u, float v, int rw, int rh, int tw, int th, int argb) {
        smooth(tex);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
        g.blit(tex, x, y, w, h, u, v, rw, rh, tw, th);
        g.setColor(1f, 1f, 1f, 1f);
    }

    public static void text(GuiGraphics g, Font font, Component s, int x, int y, int argb, boolean shadow) {
        g.drawString(font, s, x, y, argb, shadow);
    }

    public static void text(GuiGraphics g, Font font, FormattedCharSequence s, int x, int y, int argb, boolean shadow) {
        g.drawString(font, s, x, y, argb, shadow);
    }

    public static void push(GuiGraphics g) {
        g.pose().pushPose();
    }

    public static void pop(GuiGraphics g) {
        g.pose().popPose();
    }

    public static void translate(GuiGraphics g, float x, float y) {
        g.pose().translate(x, y, 0f);
    }

    public static void rotate(GuiGraphics g, float degrees) {
        g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
    }

    public static void scale(GuiGraphics g, float s) {
        g.pose().scale(s, s, 1f);
    }
}
