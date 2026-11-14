package com.seasick.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix3x2f;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;

/**
 * Variante Minecraft 26.x (voir src/main/java/com/seasick/platform/Gfx.java) : l'interface est
 * « extraite » (GuiGraphicsExtractor), le mélange fait partie du pipeline et la pose est en 2D.
 */
public final class Gfx {

    private Gfx() {}

    public static void fill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int argb) {
        g.fill(x0, y0, x1, y1, argb);
    }

    public static void gradient(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int top, int bottom) {
        g.fillGradient(x0, y0, x1, y1, top, bottom);
    }

    /** Filtrage linéaire : les textures du mod sont dessinées à 2-4 fois la densité de l'interface. */
    public static void texture(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h,
                               float u, float v, int rw, int rh, int tw, int th, int argb) {
        AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(tex);
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        g.submitGuiElementRenderState(new BlitRenderState(RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(t.getTextureView(), sampler),
            new Matrix3x2f(g.pose()), x, y, x + w, y + h, u / tw, (u + rw) / tw, v / th, (v + rh) / th, argb, g.peekScissorStack()));
    }

    public static void text(GuiGraphicsExtractor g, Font font, Component s, int x, int y, int argb, boolean shadow) {
        g.text(font, s, x, y, (argb & 0xFC000000) == 0 ? argb | 0xFF000000 : argb, shadow);
    }

    public static void text(GuiGraphicsExtractor g, Font font, FormattedCharSequence s, int x, int y, int argb, boolean shadow) {
        g.text(font, s, x, y, (argb & 0xFC000000) == 0 ? argb | 0xFF000000 : argb, shadow);
    }

    public static void push(GuiGraphicsExtractor g) {
        g.pose().pushMatrix();
    }

    public static void pop(GuiGraphicsExtractor g) {
        g.pose().popMatrix();
    }

    public static void translate(GuiGraphicsExtractor g, float x, float y) {
        g.pose().translate(x, y);
    }

    public static void rotate(GuiGraphicsExtractor g, float degrees) {
        g.pose().rotate((float) Math.toRadians(degrees));
    }

    public static void scale(GuiGraphicsExtractor g, float s) {
        g.pose().scale(s, s);
    }
}
