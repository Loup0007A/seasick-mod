package com.seasick.client;

import com.seasick.platform.Gfx;
import com.seasick.sea.Context;
import com.seasick.sea.SeaState;
import com.seasick.sick.Factor;
import com.seasick.sick.Journal;
import com.seasick.sick.LandSway;
import com.seasick.sick.Sickness;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Le carnet de bord : l'état de la mer, ce qui vous rend malade en ce moment (et de combien),
 * une prévision, et vos souvenirs de traversées.
 *
 * Il ne met pas le jeu en pause... et le lire en mer compte comme de la lecture.
 */
public class LogbookScreen extends Screen {

    private static final int PW = 404, PH = 222;
    private static final int GOLD = 0xFFE8C26A, TEXT = 0xFFE6EEF2, DIM = 0xFF9AA8B4, GOOD = 0xFF7FD67F, BAD = 0xFFFF8F6A;

    public LogbookScreen() {
        super(Component.translatable("seasick.logbook.title"));
    }

    @Override
    protected void init() {
        int x0 = (width - PW) / 2, y0 = (height - PH) / 2;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
            .bounds(x0 + PW - 64, y0 + PH - 22, 56, 16).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientBoot.KEY_LOGBOOK.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // Le fond et le cadre passent par renderBackground : avant 1.21 on l'appelle soi-même,
    // en 1.21 Screen.render l'appelle, en 26.x il est dessiné avant le reste (extractBackground).
    //#if mc1201
    //$$ @Override
    //$$ public void renderBackground(GuiGraphics g) {
    //$$     backdrop(g);
    //$$ }
    //#else
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        backdrop(g);
    }
    //#endif

    private void backdrop(GuiGraphics g) {
        Gfx.gradient(g, 0, 0, width, height, 0x50000000, 0x80000000);
        int x0 = (width - PW) / 2, y0 = (height - PH) / 2;
        Gfx.fill(g, x0, y0, x0 + PW, y0 + PH, 0xE8101820);
        Gfx.fill(g, x0, y0, x0 + PW, y0 + 1, 0xFF6A5A3A);
        Gfx.fill(g, x0, y0 + PH - 1, x0 + PW, y0 + PH, 0xFF6A5A3A);
        Gfx.fill(g, x0 + PW / 2, y0 + 20, x0 + PW / 2 + 1, y0 + PH - 26, 0x60E8C26A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        //#if mc1201
        //$$ renderBackground(g);
        //#endif
        super.render(g, mouseX, mouseY, partialTick);
        int x0 = (width - PW) / 2, y0 = (height - PH) / 2;

        Journal j = Journal.get();
        int rank = Journal.rank(j.seaLegs);
        line(g, title.copy().append(" — ").append(Component.translatable("seasick.rank." + rank)), x0 + 8, y0 + 6, GOLD, PW - 16);

        int lx = x0 + 8, rx = x0 + PW / 2 + 8, cw = PW / 2 - 16;
        int y = y0 + 22;
        y = sea(g, lx, y, cw);
        y += 4;
        you(g, lx, y, cw);
        int ry = y0 + 22;
        ry = why(g, rx, ry, cw);
        ry += 4;
        log(g, rx, ry, cw, j);
    }

    private int header(GuiGraphics g, String key, int x, int y) {
        Gfx.text(g, font, Component.translatable(key), x, y, GOLD, true);
        return y + 11;
    }

    private void line(GuiGraphics g, Component c, int x, int y, int color, int maxWidth) {
        String s = c.getString();
        if (font.width(s) > maxWidth) s = font.plainSubstrByWidth(s, maxWidth - font.width("…")) + "…";
        Gfx.text(g, font, Component.literal(s), x, y, color, false);
    }

    /** Texte replié sur plusieurs lignes si besoin ; renvoie la position de la ligne suivante. */
    private int wrap(GuiGraphics g, Component c, int x, int y, int color, int maxWidth) {
        for (FormattedCharSequence l : font.split(c, maxWidth)) {
            Gfx.text(g, font, l, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private int sea(GuiGraphics g, int x, int y, int w) {
        y = header(g, "seasick.logbook.sea", x, y);
        Context ctx = SeaState.context;
        if (ctx.onWater || ctx == Context.CAMEL || ctx == Context.BOAT_DRY) {
            if (ctx == Context.CAMEL) {
                return wrap(g, Component.translatable("seasick.logbook.camel"), x, y, TEXT, w);
            }
            if (ctx == Context.BOAT_DRY) {
                return wrap(g, Component.translatable("seasick.logbook.dry"), x, y, TEXT, w);
            }
            int d = SeaState.douglas(SeaState.hs);
            y = wrap(g, Component.translatable("seasick.douglas." + d).append(String.format(" (%d) · %.1f m", d, SeaState.hs)), x, y, Hud.seaColor(d), w);
            y = wrap(g, Component.translatable("seasick.logbook.period", String.format("%.1f", SeaState.tp),
                Component.translatable("seasick.heading." + SeaState.heading())), x, y, TEXT, w);
            int b = SeaState.beaufort(SeaState.windSpeed);
            y = wrap(g, Component.translatable("seasick.logbook.wind", b, Component.translatable("seasick.beaufort." + b),
                Component.translatable("seasick.compass." + SeaState.windFrom())), x, y, TEXT, w);
            y = wrap(g, Component.translatable("seasick.logbook.water", SeaState.fetch >= 192 ? "192+" : String.valueOf(SeaState.fetch), SeaState.depth),
                x, y, DIM, w);
            if (SeaState.covered) {
                y = wrap(g, Component.translatable("seasick.logbook.covered"), x, y, DIM, w);
            }
            if (SeaState.preset != SeaState.Preset.AUTO) {
                y = wrap(g, Component.translatable("seasick.logbook.preset", SeaState.preset.name().toLowerCase()), x, y, DIM, w);
            }
        } else {
            y = wrap(g, Component.translatable("seasick.logbook.ashore"), x, y, TEXT, w);
        }
        return y;
    }

    private void you(GuiGraphics g, int x, int y, int w) {
        y = header(g, "seasick.logbook.you", x, y);
        double q = Sickness.dose * 100;
        int bw = w - 4;
        Gfx.fill(g, x, y, x + bw, y + 6, 0xFF2A2A2A);
        Gfx.fill(g, x, y, x + (int) (Mth.clamp(q / 100, 0, 1) * bw), y + 6, Hud.gaugeColor(q));
        y += 9;
        y = wrap(g, Component.translatable("seasick.stage." + Sickness.stage).append(String.format(" · %d%%", (int) Math.round(q))), x, y, Hud.gaugeColor(q), w);
        double eq = Sickness.equilibrium();
        int secs = Sickness.secondsUntilSick();
        Component forecast;
        int color = TEXT;
        if (secs >= 0) {
            forecast = Component.translatable("seasick.forecast.sick", secs / 60, String.format("%02d", secs % 60));
            color = BAD;
        } else if (Double.isInfinite(eq) || eq >= 1) {
            forecast = Component.translatable("seasick.forecast.later");
            color = BAD;
        } else if (eq < Sickness.dose - 0.02) {
            forecast = Component.translatable("seasick.forecast.better");
            color = GOOD;
        } else {
            int st = 0;
            double eqq = eq * 100;
            if (eqq >= 15) st = 1;
            if (eqq >= 35) st = 2;
            if (eqq >= 60) st = 3;
            if (eqq >= 85) st = 4;
            forecast = Component.translatable("seasick.forecast.settle", Component.translatable("seasick.stage." + st));
        }
        y = wrap(g, forecast, x, y, color, w);
        if (LandSway.active()) {
            y = wrap(g, Component.translatable("seasick.logbook.land", LandSway.secondsLeft()), x, y, DIM, w);
        }
        if (Sickness.eyesClosed) {
            wrap(g, Component.translatable("seasick.logbook.eyes"), x, y, DIM, w);
        }
    }

    private int why(GuiGraphics g, int x, int y, int w) {
        y = header(g, "seasick.logbook.why", x, y);
        List<Factor.Active> list = new ArrayList<>(Sickness.FACTORS);
        if (list.isEmpty()) {
            return wrap(g, Component.translatable("seasick.logbook.nothing"), x, y, DIM, w);
        }
        int shown = 0;
        for (Factor.Active a : list) {
            if (shown >= 7) break;
            int pct = (int) Math.round((a.mult() - 1) * 100);
            if (pct == 0) continue;
            MutableComponent c = Component.literal((pct > 0 ? "+" : "") + pct + "% ").append(a.factor().label());
            if (a.ticksLeft() > 0) c.append(String.format(" (%ds)", a.ticksLeft() / 20));
            y = wrap(g, c, x, y, pct > 0 ? BAD : GOOD, w);
            shown++;
        }
        return y;
    }

    private void log(GuiGraphics g, int x, int y, int w, Journal j) {
        y = header(g, "seasick.logbook.log", x, y);
        int bw = w - 4;
        Gfx.fill(g, x, y, x + bw, y + 4, 0xFF2A2A2A);
        Gfx.fill(g, x, y, x + (int) (j.seaLegs * bw), y + 4, 0xFF5AB4E8);
        y += 7;
        y = wrap(g, Component.translatable("seasick.logbook.legs", (int) Math.round(j.seaLegs * 100)), x, y, TEXT, w);
        long mins = j.ticksAtSea / 20 / 60;
        y = wrap(g, Component.translatable("seasick.logbook.time", mins / 60, String.format("%02d", mins % 60), (int) j.distance, j.voyages), x, y, TEXT, w);
        y = wrap(g, Component.translatable("seasick.logbook.sick", j.timesSick, j.overTheRail, j.dryHeaves), x, y, TEXT, w);
        long calm = j.longestCalmVoyage / 20;
        wrap(g, Component.translatable("seasick.logbook.calm", calm / 60, String.format("%02d", calm % 60)), x, y, TEXT, w);
    }
}
