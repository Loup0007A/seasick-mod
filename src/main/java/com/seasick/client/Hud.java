package com.seasick.client;

import com.seasick.Seasick;
import com.seasick.config.SeaConfig;
import com.seasick.platform.Gfx;
import com.seasick.sea.Context;
import com.seasick.sea.SeaState;
import com.seasick.sea.Swell;
import com.seasick.sick.LandSway;
import com.seasick.sick.Sickness;
import com.seasick.sick.Vomit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

/**
 * Ce que l'on voit du malaise.
 *
 * Sous le HUD : une brume jaune-vert qui dérive (le teint qui tourne), les bords qui
 * s'assombrissent au rythme de la houle, des gouttes de sueur froide, des paupières lourdes,
 * et l'horizon artificiel quand le vrai a disparu.
 * Au-dessus : un instrument de bord en bois et laiton près de la barre d'action — un
 * clinomètre dont l'aiguille suit le roulis, le visage du marin et la jauge de malaise.
 * (Dans l'esprit des jauges de Cold Sweat et Tough As Nails.)
 */
public final class Hud {

    private Hud() {}

    static final ResourceLocation VIGNETTE = Seasick.id("textures/gui/vignette.png");
    static final ResourceLocation HAZE = Seasick.id("textures/gui/haze.png");
    static final ResourceLocation SWEAT = Seasick.id("textures/gui/sweat.png");
    static final ResourceLocation LID_TOP = Seasick.id("textures/gui/eyelid_top.png");
    static final ResourceLocation LID_BOTTOM = Seasick.id("textures/gui/eyelid_bottom.png");
    static final ResourceLocation HORIZON = Seasick.id("textures/gui/horizon.png");
    static final ResourceLocation BOAT_SYMBOL = Seasick.id("textures/gui/boat_symbol.png");
    static final ResourceLocation INSTRUMENT = Seasick.id("textures/gui/instrument.png");
    static final ResourceLocation NEEDLE = Seasick.id("textures/gui/needle.png");
    static final ResourceLocation FACES = Seasick.id("textures/gui/faces.png");
    static final ResourceLocation BAR = Seasick.id("textures/gui/bar.png");
    static final ResourceLocation LAMP = Seasick.id("textures/gui/lamp.png");

    /** Taille de l'instrument dans l'interface (texture à 4 fois cette densité). */
    public static final int IW = 66, IH = 28;

    private static float lids, lidsPrev;
    private static double needle, needleVel;
    /** Champ de vision vertical de la dernière image (degrés), pour placer l'horizon. */
    public static double lastFov = 70;
    public static boolean horizonDrawn;

    public static void reset() {
        lids = lidsPrev = 0;
        needle = needleVel = 0;
        horizonDrawn = false;
    }

    /** Paupières, mises à jour à chaque tick. */
    public static void tick() {
        lidsPrev = lids;
        float target = 0f;
        if (Sickness.eyesClosed) target = 1f;
        if (Sickness.yawnTicks > 0) target = Math.max(target, 0.62f * (float) Math.sin(Math.PI * Sickness.yawnTicks / 40.0));
        if (Vomit.acting()) target = Math.max(target, 0.35f);
        if (Sickness.stage >= 4) target = Math.max(target, 0.12f);
        lids += (target - lids) * (target > lids ? 0.45f : 0.3f);
    }

    /** Petite inertie de l'aiguille : un vrai clinomètre oscille un peu autour de la valeur. */
    public static void needleTick() {
        needleVel += -needle * 0.25 - needleVel * 0.35;
        needle += needleVel;
        if (Vomit.acting()) needleVel += (Math.random() - 0.5) * 1.5;
        if (Sickness.fumbleTicks > 0) needleVel += (Math.random() - 0.5) * 0.8;
    }

    private static int argb(double a, int rgb) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    static double time(float pt) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : (mc.level.getGameTime() % 240000L + pt) / 20.0;
    }

    // ───────────────────────── sous le HUD ─────────────────────────

    public static void under(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !SeaConfig.enabled()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        double q = Sickness.shown * 100;
        double fx = SeaConfig.screen();
        double t = time(pt);

        if (fx > 0 && q > 25) {
            // le teint tourne : deux nappes de brume qui dérivent à des vitesses différentes
            double a = Mth.clamp((q - 25) / 75, 0, 1) * 0.5 * fx;
            double sway = (Swell.roll(pt) + LandSway.roll(pt)) * 0.004;
            haze(g, w, h, 1.35, t * 0.021 + sway, t * 0.013, argb(a, 0xC8D66E));
            haze(g, w, h, 1.7, -t * 0.017 + 0.4 - sway, t * 0.009 + 0.25, argb(a * 0.7, 0xA9C858));
        }
        if (fx > 0 && q > 40) {
            // les bords s'assombrissent et respirent au rythme de la houle
            double pulse = 0.15 * Math.sin(Swell.cycle(pt));
            double a = Mth.clamp((Mth.clamp((q - 40) / 45, 0, 1) + pulse) * fx, 0, 1);
            Gfx.texture(g, VIGNETTE, 0, 0, w, h, 0, 0, 512, 512, 512, 512, argb(a, 0x16241A));
        }
        if (fx > 0 && q > 45) {
            // sueurs froides : gouttes qui perlent sur les bords, avec un reflet qui va et vient
            double a = Mth.clamp((q - 45) / 40, 0, 1) * (0.75 + 0.25 * Math.sin(t * 1.3)) * fx;
            Gfx.texture(g, SWEAT, 0, 0, w, h, 0, 0, 512, 512, 512, 512, argb(a, 0xFFFFFF));
        }

        horizon(g, p, pt, w, h);

        float c = Mth.lerp(pt, lidsPrev, lids);
        if (!Sickness.eyesClosed) c *= (float) Math.max(fx, 0.35);
        if (c > 0.01f) eyelids(g, w, h, c);
    }

    /** Une nappe de brume plus grande que l'écran, qui dérive lentement. */
    private static void haze(GuiGraphics g, int w, int h, double scale, double ox, double oy, int color) {
        int hw = (int) (w * scale), hh = (int) (h * scale);
        double mx = (hw - w) / 2.0, my = (hh - h) / 2.0;
        int x = (int) (-mx + Math.sin(ox * 2 * Math.PI) * mx);
        int y = (int) (-my + Math.cos(oy * 2 * Math.PI) * my);
        Gfx.texture(g, HAZE, x, y, hw, hh, 0, 0, 512, 512, 512, 512, color);
    }

    private static void eyelids(GuiGraphics g, int w, int h, float c) {
        int lidH = (int) (h * 0.62);
        int travel = (int) (h * 0.58);
        int top = (int) (c * travel);
        // paupière du haut : la texture descend, l'espace au-dessus est noir
        if (top - lidH > 0) Gfx.fill(g, 0, 0, w, top - lidH, 0xFF070303);
        Gfx.texture(g, LID_TOP, 0, top - lidH, w, lidH, 0, 0, 512, 192, 512, 192, 0xFFFFFFFF);
        int bottom = h - (int) (c * travel * 0.85);
        Gfx.texture(g, LID_BOTTOM, 0, bottom, w, lidH, 0, 0, 512, 192, 512, 192, 0xFFFFFFFF);
        if (bottom + lidH < h) Gfx.fill(g, 0, bottom + lidH, w, h, 0xFF070303);
        if (c > 0.97f) Gfx.fill(g, 0, 0, w, h, 0xFF070303);
    }

    private static boolean wantsHorizon() {
        SeaConfig.Horizon mode = SeaConfig.get(SeaConfig.HORIZON, SeaConfig.Horizon.ASSIST);
        if (mode == SeaConfig.Horizon.OFF) return false;
        Context ctx = SeaState.context;
        boolean afloat = ctx.onWater && SeaState.hs > 0.15 || ctx == Context.CAMEL && SeaState.hs > 0.05 || LandSway.active();
        if (!afloat || Sickness.eyesClosed) return false;
        if (mode == SeaConfig.Horizon.ALWAYS || SeaConfig.get(SeaConfig.REDUCE_MOTION, false)) return true;
        return Sickness.stage >= 1 && !Sickness.horizonVisible;
    }

    /** Horizon artificiel : un trait lumineux sur le vrai horizon, et la maquette fixe du bateau. */
    private static void horizon(GuiGraphics g, LocalPlayer p, float pt, int w, int h) {
        horizonDrawn = wantsHorizon();
        Sickness.artificialHorizonShown = horizonDrawn;
        if (!horizonDrawn) return;
        double boatRoll = Swell.roll(pt) + LandSway.roll(pt);
        double boatPitch = Swell.pitch(pt) + LandSway.pitch(pt);
        double look = p.getViewXRot(pt) - View.appliedPitch + boatPitch;
        double half = Math.toRadians(Mth.clamp(lastFov, 10, 170) / 2);
        double f = (h / 2.0) / Math.tan(half);

        Gfx.push(g);
        Gfx.translate(g, w / 2f, h / 2f);
        Gfx.rotate(g, (float) -boatRoll);
        int len = (int) (w * 0.92);
        int dy = (int) Math.round(-f * Math.tan(Math.toRadians(Mth.clamp(look, -80, 80))));
        if (Math.abs(dy) < h) {
            // trait sombre dessous pour le contraste sur un ciel clair, puis le trait lumineux
            Gfx.texture(g, HORIZON, -len / 2, dy - 5, len, 10, 0, 0, 512, 24, 512, 24, 0x90001828);
            Gfx.texture(g, HORIZON, -len / 2, dy - 3, len, 6, 0, 0, 512, 24, 512, 24, 0xF0D4F2FF);
        }
        // échelle de tangage, tous les 5°
        for (int k = -2; k <= 2; k++) {
            if (k == 0) continue;
            int yy = (int) Math.round(-f * Math.tan(Math.toRadians(Mth.clamp(look - 5 * k, -80, 80))));
            if (Math.abs(yy) > h / 2) continue;
            int lw = Math.abs(k) == 1 ? 36 : 56;
            Gfx.texture(g, HORIZON, -lw / 2, yy - 3, lw, 6, 0, 0, 512, 24, 512, 24, 0x60001828);
            Gfx.texture(g, HORIZON, -lw / 2, yy - 2, lw, 4, 0, 0, 512, 24, 512, 24, 0x90D4F2FF);
        }
        Gfx.pop(g);
        // la maquette du bateau, fixe : là où pointe l'étrave
        Gfx.texture(g, BOAT_SYMBOL, w / 2 - 32, h / 2 - 8, 64, 16, 0, 0, 256, 64, 256, 64, 0xE6FFFFFF);
    }

    // ───────────────────────── au-dessus du HUD : l'instrument ─────────────────────────

    public static void over(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !SeaConfig.enabled() || mc.options.hideGui) return;
        SeaConfig.HudPosition pos = SeaConfig.get(SeaConfig.HUD, SeaConfig.HudPosition.HOTBAR_LEFT);
        if (pos == SeaConfig.HudPosition.HIDDEN) return;
        double q = Sickness.shown * 100;
        boolean show = SeaState.context.moving() || q > 1 || LandSway.active();
        if (!show) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int x, y;
        switch (pos) {
            case TOP_LEFT -> { x = 4; y = 4; }
            case TOP_RIGHT -> { x = w - IW - 4; y = 4; }
            default -> {
                x = w / 2 - 91 - 6 - IW;
                if (!p.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) x -= 29;
                y = h - IH - 1;
            }
        }
        instrument(g, x, y, pt, q);
    }

    /** L'instrument complet en (x, y). */
    public static void instrument(GuiGraphics g, int x, int y, float pt, double q) {
        Gfx.texture(g, INSTRUMENT, x, y, IW, IH, 0, 0, IW * 4, IH * 4, IW * 4, IH * 4, 0xFFFFFFFF);

        // aiguille du clinomètre : le roulis réel du bateau
        double roll = Mth.clamp(Swell.roll(pt) + LandSway.roll(pt), -48, 48);
        Gfx.push(g);
        Gfx.translate(g, x + 17f, y + 21.5f);
        Gfx.rotate(g, (float) (roll + needle));
        Gfx.texture(g, NEEDLE, -3, -15, 6, 18, 0, 0, 24, 72, 24, 72, 0xFFFFFFFF);
        Gfx.pop(g);

        // voyant : état de la mer (Douglas)
        Context ctx = SeaState.context;
        int lamp = ctx.onWater ? seaColor(SeaState.douglas(SeaState.hs)) : (ctx == Context.CAMEL ? 0xFFE8C070 : 0xFF6A6A6A);
        double glowA = ctx.onWater ? 0.85 + 0.15 * Math.sin(time(pt) * 3) : 0.55;
        Gfx.texture(g, LAMP, x + 2, y + 21, 6, 6, 0, 0, 64, 64, 64, 64, argb(glowA, lamp));

        // le visage, qui oscille un peu avec la houle
        int face = Sickness.eyesClosed ? 5 : Mth.clamp(Sickness.stage, 0, 4);
        Gfx.push(g);
        Gfx.translate(g, x + 46.5f, y + 12.5f);
        Gfx.rotate(g, (float) (roll * 0.35));
        Gfx.texture(g, FACES, -9, -9, 18, 18, face * 64, 0, 64, 64, 384, 64, 0xFFFFFFFF);
        Gfx.pop(g);

        // jauge
        double frac = Mth.clamp(q / 100, 0, 1);
        int bw = (int) Math.round(frac * 25);
        if (bw > 0) Gfx.texture(g, BAR, x + 35, y + 23, bw, 3, 0, 0, (int) Math.max(1, frac * 256), 16, 256, 16, 0xFFFFFFFF);
    }

    public static int gaugeColor(double q) {
        if (q < 15) return 0xFF6FD06F;
        if (q < 35) return 0xFFC8D860;
        if (q < 60) return 0xFFE8C040;
        if (q < 85) return 0xFFE88A3A;
        return 0xFFE84A3A;
    }

    /** Couleur d'encre plus sombre, lisible sur parchemin. */
    public static int inkColor(double q) {
        if (q < 15) return 0xFF2E7A2E;
        if (q < 35) return 0xFF6A7A1A;
        if (q < 60) return 0xFF9A6A10;
        if (q < 85) return 0xFFA84A16;
        return 0xFFA82416;
    }

    public static int seaColor(int douglas) {
        if (douglas <= 2) return 0xFF7FC8FF;
        if (douglas <= 3) return 0xFF62E0C0;
        if (douglas <= 4) return 0xFFE8D060;
        if (douglas <= 6) return 0xFFF09040;
        return 0xFFF04A4A;
    }

    /** Couleur d'encre de l'état de la mer. */
    public static int seaInk(int douglas) {
        if (douglas <= 2) return 0xFF2A5C8A;
        if (douglas <= 3) return 0xFF1E7A66;
        if (douglas <= 4) return 0xFF8A7414;
        if (douglas <= 6) return 0xFFA85A14;
        return 0xFFA82416;
    }
}
