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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

/**
 * Ce que l'on voit du malaise : teint pâle, sueurs froides sur les bords, paupières lourdes,
 * l'horizon artificiel quand le vrai a disparu, et une petite jauge près de la barre d'action.
 */
public final class Hud {

    private Hud() {}

    private static final ResourceLocation VIGNETTE = Seasick.id("textures/gui/vignette.png");
    private static final ResourceLocation FACES = Seasick.id("textures/gui/faces.png");

    private static float lids, lidsPrev;
    /** Champ de vision vertical de la dernière image (degrés), pour placer l'horizon. */
    public static double lastFov = 70;
    public static boolean horizonDrawn;

    public static void reset() {
        lids = lidsPrev = 0;
        horizonDrawn = false;
    }

    /** Fermeture des paupières, mise à jour à chaque tick. */
    public static void tick() {
        lidsPrev = lids;
        float target = 0f;
        if (Sickness.eyesClosed) target = 1f;
        if (Sickness.yawnTicks > 0) target = Math.max(target, 0.62f * (float) Math.sin(Math.PI * Sickness.yawnTicks / 40.0));
        if (Vomit.acting()) target = Math.max(target, 0.35f);
        if (Sickness.stage >= 4) target = Math.max(target, 0.12f);
        lids += (target - lids) * (target > lids ? 0.45f : 0.3f);
    }

    /** Sous le HUD : teinte, vignette, paupières, horizon artificiel. */
    public static void under(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !SeaConfig.enabled()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        double q = Sickness.shown * 100;
        double fx = SeaConfig.screen();

        if (fx > 0 && q > 25) {
            float pallor = (float) (Mth.clamp((q - 25) / 75, 0, 1) * 0.26 * fx);
            Gfx.fill(g, 0, 0, w, h, argb(pallor, 0xA6D8A0));
        }
        if (fx > 0 && q > 40) {
            // sueurs froides : les bords s'assombrissent et respirent au rythme de la houle
            double pulse = 0.15 * Math.sin(Swell.cycle(pt));
            float a = (float) Mth.clamp((Mth.clamp((q - 40) / 45, 0, 1) + pulse) * fx, 0, 1);
            Gfx.texture(g, VIGNETTE, 0, 0, w, h, 0, 0, 256, 256, 256, 256, argb(a, 0x1C2A1C));
        }

        horizon(g, p, pt, w, h);

        float c = Mth.lerp(pt, lidsPrev, lids);
        if (Sickness.eyesClosed) c = Math.max(c, 0.0f);
        else c *= (float) Math.max(fx, 0.35);
        if (c > 0.01f) eyelids(g, w, h, c);
    }

    private static int argb(float a, int rgb) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static void eyelids(GuiGraphics g, int w, int h, float c) {
        int half = h / 2 + 2;
        int lid = (int) (half * c);
        int edge = Math.max(4, h / 14);
        Gfx.fill(g, 0, 0, w, lid, 0xFF050303);
        Gfx.gradient(g, 0, lid, w, lid + edge, 0xFF050303, 0x00050303);
        Gfx.fill(g, 0, h - lid, w, h, 0xFF050303);
        Gfx.gradient(g, 0, h - lid - edge, w, h - lid, 0x00050303, 0xFF050303);
        if (c > 0.97f) Gfx.fill(g, 0, 0, w, h, 0xFF050303);
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

    /** Horizon artificiel : la ligne du vrai horizon, et la maquette fixe du bateau au centre. */
    private static void horizon(GuiGraphics g, LocalPlayer p, float pt, int w, int h) {
        horizonDrawn = wantsHorizon();
        Sickness.artificialHorizonShown = horizonDrawn;
        if (!horizonDrawn) return;
        double boatRoll = Swell.roll(pt) + LandSway.roll(pt);
        double boatPitch = Swell.pitch(pt) + LandSway.pitch(pt);
        double look = p.getViewXRot(pt) - View.appliedPitch + boatPitch;
        double half = Math.toRadians(Mth.clamp(lastFov, 10, 170) / 2);
        double f = (h / 2.0) / Math.tan(half);
        double y = h / 2.0 - f * Math.tan(Math.toRadians(Mth.clamp(look, -80, 80)));
        int cx = w / 2;
        int alpha = 0xB0;

        Gfx.push(g);
        Gfx.translate(g, cx, h / 2f);
        Gfx.rotate(g, (float) -boatRoll);
        int dy = (int) Math.round(y - h / 2.0);
        if (Math.abs(dy) < h) {
            int span = (int) (w * 0.8);
            for (int x = -span; x < span; x += 18) {
                Gfx.fill(g, x, dy, x + 11, dy + 1, (alpha << 24) | 0xCDEFFF);
            }
            // repères de tangage tous les 5°
            for (int k = -2; k <= 2; k++) {
                if (k == 0) continue;
                int yy = (int) Math.round(h / 2.0 - f * Math.tan(Math.toRadians(look - 5 * k)) - h / 2.0);
                Gfx.fill(g, -14, yy, 14, yy + 1, 0x60CDEFFF);
            }
        }
        Gfx.pop(g);
        // maquette fixe : là où est le bateau
        Gfx.fill(g, cx - 34, h / 2, cx - 12, h / 2 + 1, 0xC0FFD24A);
        Gfx.fill(g, cx + 12, h / 2, cx + 34, h / 2 + 1, 0xC0FFD24A);
        Gfx.fill(g, cx - 1, h / 2 - 1, cx + 2, h / 2 + 2, 0xC0FFD24A);
    }

    /** Au-dessus du HUD : la jauge. */
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
            case TOP_RIGHT -> { x = w - 40; y = 4; }
            default -> {
                x = w / 2 - 91 - 40;
                if (!p.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) x -= 29;
                y = h - 21;
            }
        }
        Gfx.fill(g, x - 2, y - 2, x + 38, y + 20, 0x90101820);
        int face = Mth.clamp(Sickness.stage, 0, 4);
        // le visage oscille avec la houle
        double tilt = (Swell.roll(pt) + LandSway.roll(pt)) * 0.6;
        Gfx.push(g);
        Gfx.translate(g, x + 8, y + 8);
        Gfx.rotate(g, (float) tilt);
        Gfx.texture(g, FACES, -8, -8, 16, 16, face * 16, 0, 16, 16, 80, 16, 0xFFFFFFFF);
        Gfx.pop(g);

        // barre de malaise
        int bw = 16;
        int fill = (int) Math.round(Mth.clamp(q / 100, 0, 1) * bw);
        Gfx.fill(g, x, y + 17, x + bw, y + 19, 0xFF2A2A2A);
        Gfx.fill(g, x, y + 17, x + fill, y + 19, gaugeColor(q));

        // état de la mer : petite vague + chiffre de Douglas
        int d = SeaState.context.onWater ? SeaState.douglas(SeaState.hs) : -1;
        int wx = x + 19, wy = y + 3;
        int wave = d < 0 ? 0xFF8A8A8A : seaColor(d);
        for (int i = 0; i < 7; i++) {
            int yy = wy + (int) Math.round(Math.sin(i / 6.0 * Math.PI * 2 + Swell.cycle(pt)) * 1.5);
            Gfx.fill(g, wx + i * 2, yy, wx + i * 2 + 2, yy + 1, wave);
        }
        String label = d >= 0 ? String.valueOf(d) : (SeaState.context == Context.CAMEL ? "~" : "-");
        Gfx.text(g, mc.font, Component.literal(label), wx + 4, y + 8, 0xFFFFFFFF, true);
    }

    public static int gaugeColor(double q) {
        if (q < 15) return 0xFF6FD06F;
        if (q < 35) return 0xFFC8D860;
        if (q < 60) return 0xFFE8C040;
        if (q < 85) return 0xFFE88A3A;
        return 0xFFE84A3A;
    }

    public static int seaColor(int douglas) {
        if (douglas <= 2) return 0xFF7FC8FF;
        if (douglas <= 3) return 0xFF62E0C0;
        if (douglas <= 4) return 0xFFE8D060;
        if (douglas <= 6) return 0xFFF09040;
        return 0xFFF04A4A;
    }
}
