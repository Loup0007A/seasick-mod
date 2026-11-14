package com.seasick.client;

import com.seasick.Seasick;
import com.seasick.platform.Gfx;
import com.seasick.sea.Context;
import com.seasick.sea.SeaState;
import com.seasick.sea.Swell;
import com.seasick.sick.Factor;
import com.seasick.sick.Journal;
import com.seasick.sick.LandSway;
import com.seasick.sick.Sickness;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Le journal de bord, en livre ouvert (à la manière des livres Patchouli).
 *
 * Première double page : la mer (une petite scène animée — la houle, le bateau qui roule, le
 * vent) et votre état (visage, jauge, prévision, ce qui joue en ce moment).
 * Seconde double page : le pied marin et vos souvenirs, et les conseils du médecin de bord.
 *
 * Il ne met pas le jeu en pause... et le lire en mer compte comme de la lecture.
 */
public class LogbookScreen extends Screen {

    private static final ResourceLocation BOOK = Seasick.id("textures/gui/book.png");
    private static final ResourceLocation BOAT = Seasick.id("textures/gui/little_boat.png");
    private static final ResourceLocation ARROW = Seasick.id("textures/gui/arrow.png");

    /** Taille du livre en unités d'interface (texture à 2 fois cette densité). */
    private static final int BW = 340, BH = 210;
    private static final int LX = 24, RX = 184, PW = 134, TOP = 17;
    private static final int TITLE = 0xFF7A1E14, HEAD = 0xFF5A3418, INK = 0xFF3A2818, DIM = 0xFF8A7458,
        GOOD = 0xFF2E6A2A, BAD = 0xFF9A2A18, RULE = 0x605A3418;
    private static final int PAGES = 2;

    private static int page;
    private float scale = 1f;
    private int bx, by;

    public LogbookScreen() {
        super(Component.translatable("seasick.logbook.title"));
    }

    @Override
    protected void init() {
        scale = Math.min(1f, Math.min((width - 12) / (float) BW, (height - 12) / (float) BH));
        bx = (int) ((width - BW * scale) / 2);
        by = (int) ((height - BH * scale) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Pour les tests : ouvrir une double page donnée. */
    public static void showPage(int p) {
        page = Mth.clamp(p, 0, PAGES - 1);
    }

    private void turn(int d) {
        int np = Mth.clamp(page + d, 0, PAGES - 1);
        if (np != page) {
            page = np;
            Sounds.play("item.book.page_turn", 1f, 1f, true);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientBoot.KEY_LOGBOOK.matches(key, scan)) {
            onClose();
            return true;
        }
        if (key == 262 || key == 266) { // →, page suivante
            turn(1);
            return true;
        }
        if (key == 263 || key == 267) { // ←, page précédente
            turn(-1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double lx = (mouseX - bx) / scale, ly = (mouseY - by) / scale;
        if (ly >= 184 && ly <= 202) {
            if (page > 0 && lx >= 14 && lx <= 40) {
                turn(-1);
                return true;
            }
            if (page < PAGES - 1 && lx >= 300 && lx <= 326) {
                turn(1);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    //#if mc1201
    //$$ @Override
    //$$ public void renderBackground(GuiGraphics g) {
    //$$     Gfx.gradient(g, 0, 0, width, height, 0x60000000, 0xA0000000);
    //$$ }
    //#else
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Gfx.gradient(g, 0, 0, width, height, 0x60000000, 0xA0000000);
    }
    //#endif

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        //#if mc1201
        //$$ renderBackground(g);
        //#endif
        super.render(g, mouseX, mouseY, partialTick);
        float pt = partialTick;
        Gfx.push(g);
        Gfx.translate(g, bx, by);
        Gfx.scale(g, scale);
        Gfx.texture(g, BOOK, 0, 0, BW, BH, 0, 0, BW * 2, BH * 2, BW * 2, BH * 2, 0xFFFFFFFF);
        if (page == 0) {
            seaPage(g, pt);
            youPage(g);
        } else {
            legsPage(g);
            doctorPage(g);
        }
        // pagination
        double lx = (mouseX - bx) / scale, ly = (mouseY - by) / scale;
        centered(g, Component.literal(String.valueOf(page * 2 + 1)), LX + PW / 2, 191, DIM);
        centered(g, Component.literal(String.valueOf(page * 2 + 2)), RX + PW / 2, 191, DIM);
        if (page > 0) arrow(g, 18, 186, -90, ly >= 184 && ly <= 202 && lx >= 14 && lx <= 40);
        if (page < PAGES - 1) arrow(g, 308, 186, 90, ly >= 184 && ly <= 202 && lx >= 300 && lx <= 326);
        Gfx.pop(g);
    }

    private void arrow(GuiGraphics g, int x, int y, float rot, boolean hover) {
        Gfx.push(g);
        Gfx.translate(g, x + 7, y + 7);
        Gfx.rotate(g, rot);
        int c = hover ? 0xFF9A2A18 : 0xC05A3418;
        Gfx.texture(g, ARROW, -7, -7, 14, 14, 0, 0, 64, 64, 64, 64, c);
        Gfx.pop(g);
    }

    // ───────────────────────── outils d'écriture ─────────────────────────

    private void centered(GuiGraphics g, Component c, int cx, int y, int color) {
        Gfx.text(g, font, c, cx - font.width(c) / 2, y, color, false);
    }

    /** Texte replié ; renvoie la position de la ligne suivante. */
    private int wrap(GuiGraphics g, Component c, int x, int y, int color, int maxWidth) {
        for (FormattedCharSequence l : font.split(c, maxWidth)) {
            Gfx.text(g, font, l, x, y, color, false);
            y += 10;
        }
        return y;
    }

    /** Titre de section, souligné d'un trait d'encre. */
    private int header(GuiGraphics g, Component c, int x, int y) {
        Gfx.text(g, font, c, x, y, HEAD, false);
        int w = font.width(c);
        Gfx.fill(g, x, y + 9, x + w, y + 10, 0x905A3418);
        Gfx.fill(g, x + w + 3, y + 5, x + PW, y + 6, RULE);
        return y + 14;
    }

    private void bar(GuiGraphics g, int x, int y, int w, double frac, boolean gradient, int color) {
        Gfx.fill(g, x - 1, y - 1, x + w + 1, y + 5, 0xA05A3418);
        Gfx.fill(g, x, y, x + w, y + 4, 0xFFE8D8B4);
        int fw = (int) Math.round(Mth.clamp(frac, 0, 1) * w);
        if (fw <= 0) return;
        if (gradient) Gfx.texture(g, Hud.BAR, x, y, fw, 4, 0, 0, (int) Math.max(1, frac * 256), 16, 256, 16, 0xFFFFFFFF);
        else Gfx.fill(g, x, y, x + fw, y + 4, color);
    }

    // ───────────────────────── page 1 : la mer ─────────────────────────

    private void seaPage(GuiGraphics g, float pt) {
        Journal j = Journal.get();
        centered(g, title, LX + PW / 2, TOP, TITLE);
        centered(g, Component.literal("~ ").append(Component.translatable("seasick.rank." + Journal.rank(j.seaLegs))).append(" ~"),
            LX + PW / 2, TOP + 11, DIM);
        int y = TOP + 25;
        scene(g, LX, y, PW, 50, pt);
        y += 56;

        Context ctx = SeaState.context;
        if (ctx == Context.CAMEL) {
            wrap(g, Component.translatable("seasick.logbook.camel"), LX, y, INK, PW);
            return;
        }
        if (ctx == Context.BOAT_DRY) {
            wrap(g, Component.translatable("seasick.logbook.dry"), LX, y, INK, PW);
            return;
        }
        if (!ctx.onWater) {
            y = wrap(g, Component.translatable("seasick.logbook.ashore"), LX, y, INK, PW);
            if (LandSway.active()) wrap(g, Component.translatable("seasick.logbook.land", LandSway.secondsLeft()), LX, y + 2, BAD, PW);
            return;
        }
        int d = SeaState.douglas(SeaState.hs);
        y = wrap(g, Component.translatable("seasick.douglas." + d).append(String.format(" (%d) · %.1f m", d, SeaState.hs)), LX, y, Hud.seaInk(d), PW);
        y = wrap(g, Component.translatable("seasick.logbook.period", String.format("%.1f", SeaState.tp),
            Component.translatable("seasick.heading." + SeaState.heading())), LX, y, INK, PW);
        int b = SeaState.beaufort(SeaState.windSpeed);
        y = wrap(g, Component.translatable("seasick.logbook.wind", b, Component.translatable("seasick.beaufort." + b),
            Component.translatable("seasick.compass." + SeaState.windFrom())), LX, y, INK, PW);
        y = wrap(g, Component.translatable("seasick.logbook.water", SeaState.fetch >= 192 ? "192+" : String.valueOf(SeaState.fetch), SeaState.depth),
            LX, y, DIM, PW);
        if (SeaState.covered) y = wrap(g, Component.translatable("seasick.logbook.covered"), LX, y, DIM, PW);
        if (SeaState.preset != SeaState.Preset.AUTO) {
            wrap(g, Component.translatable("seasick.logbook.preset", SeaState.preset.name().toLowerCase()), LX, y, DIM, PW);
        }
    }

    /** Petite scène peinte : ciel, houle animée à l'échelle, bateau qui roule, flèche du vent. */
    private void scene(GuiGraphics g, int x, int y, int w, int h, float pt) {
        Minecraft mc = Minecraft.getInstance();
        Context ctx = SeaState.context;
        float rain = mc.level == null ? 0 : mc.level.getRainLevel(pt);
        int skyTop = rain > 0.3f ? 0xFF8C98A4 : 0xFF9CC4E4, skyBot = rain > 0.3f ? 0xFFC4C8C8 : 0xFFE8EEDC;
        Gfx.fill(g, x - 1, y - 1, x + w + 1, y + h + 1, 0xC05A3418);
        Gfx.gradient(g, x, y, x + w, y + h, skyTop, skyBot);
        double t = Hud.time(pt);
        boolean water = ctx.onWater || ctx == Context.BOAT_DRY;
        int mid = y + h * 3 / 5;
        if (water || ctx == Context.LAND && LandSway.active()) {
            // houle à l'échelle : amplitude selon la hauteur, longueur selon la période
            double amp = Mth.clamp(SeaState.hs * 2.4, 0.4, h * 0.3);
            double lambda = Mth.clamp(1.56 * SeaState.tp * SeaState.tp * 0.9, 10, 140);
            double phase = t * 2 * Math.PI / Math.max(SeaState.tp, 1.2);
            for (int i = 0; i < w; i++) {
                double yy = mid - amp * Math.sin(2 * Math.PI * i / lambda - phase) - amp * 0.3 * Math.sin(4 * Math.PI * i / lambda - phase * 1.3);
                int top = (int) Math.round(yy);
                Gfx.fill(g, x + i, top, x + i + 1, y + h, 0xFF3A6E9A);
                Gfx.fill(g, x + i, top, x + i + 1, top + 1, 0xFFE4F2FA);
                Gfx.fill(g, x + i, top + 1, x + i + 1, Math.min(y + h, top + 4), 0xFF5A8EBA);
            }
            // le bateau, sur la crête du milieu
            int cx = x + w / 2;
            double cy = mid - amp * Math.sin(2 * Math.PI * (w / 2.0) / lambda - phase);
            double roll = Mth.clamp(Swell.roll(pt) + LandSway.roll(pt), -40, 40);
            Gfx.push(g);
            Gfx.translate(g, cx, (float) cy - 4);
            Gfx.rotate(g, (float) roll);
            Gfx.texture(g, BOAT, -12, -10, 24, 14, 0, 0, 128, 72, 128, 72, 0xFFFFFFFF);
            Gfx.pop(g);
        } else {
            // à terre : herbe et sable
            int ground = ctx == Context.CAMEL ? 0xFFD9C08A : 0xFF7AA65A;
            Gfx.fill(g, x, mid, x + w, y + h, ground);
            Gfx.fill(g, x, mid, x + w, mid + 1, 0xFF5A7A3A);
        }
        // flèche du vent, relative à votre regard (vers le haut = devant vous)
        if (mc.player != null && (water || ctx == Context.CAMEL)) {
            double yaw = Math.toRadians(mc.player.getYRot());
            double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
            double wx = Math.cos(SeaState.windDir), wz = Math.sin(SeaState.windDir);
            double ang = Math.toDegrees(Math.atan2(wx * rx + wz * rz, wx * fx + wz * fz));
            Gfx.push(g);
            Gfx.translate(g, x + w - 10, y + 10);
            Gfx.rotate(g, (float) ang);
            Gfx.texture(g, ARROW, -6, -6, 12, 12, 0, 0, 64, 64, 64, 64, 0xE03A2818);
            Gfx.pop(g);
            Component f = Component.literal(String.valueOf(SeaState.beaufort(SeaState.windSpeed)));
            Gfx.text(g, font, f, x + w - 20 - font.width(f) / 2, y + 6, 0xFF3A2818, false);
        }
    }

    // ───────────────────────── page 2 : vous ─────────────────────────

    private void youPage(GuiGraphics g) {
        int y = header(g, Component.translatable("seasick.logbook.you"), RX, TOP);
        double q = Sickness.dose * 100;
        int face = Sickness.eyesClosed ? 5 : Mth.clamp(Sickness.stage, 0, 4);
        Gfx.texture(g, Hud.FACES, RX, y, 26, 26, face * 64, 0, 64, 64, 384, 64, 0xFFFFFFFF);
        Gfx.text(g, font, Component.translatable("seasick.stage." + Sickness.stage), RX + 32, y + 3, Hud.inkColor(q), false);
        Gfx.text(g, font, Component.literal(Math.round(q) + " %"), RX + 32, y + 14, DIM, false);
        bar(g, RX + 32 + 30, y + 16, PW - 64, q / 100, true, 0);
        y += 32;

        double eq = Sickness.equilibrium();
        int secs = Sickness.secondsUntilSick();
        Component forecast;
        int color = INK;
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
            double e = eq * 100;
            int st = e >= 85 ? 4 : e >= 60 ? 3 : e >= 35 ? 2 : e >= 15 ? 1 : 0;
            forecast = Component.translatable("seasick.forecast.settle", Component.translatable("seasick.stage." + st));
        }
        y = wrap(g, forecast, RX, y, color, PW);
        if (Sickness.eyesClosed) y = wrap(g, Component.translatable("seasick.logbook.eyes"), RX, y, DIM, PW);
        y += 4;

        y = header(g, Component.translatable("seasick.logbook.why"), RX, y);
        List<Factor.Active> list = new ArrayList<>(Sickness.FACTORS);
        list.removeIf(a -> Math.round((a.mult() - 1) * 100) == 0);
        if (list.isEmpty()) {
            wrap(g, Component.translatable("seasick.logbook.nothing"), RX, y, DIM, PW);
            return;
        }
        for (Factor.Active a : list) {
            if (y > 172) break;
            int pct = (int) Math.round((a.mult() - 1) * 100);
            boolean bad = pct > 0;
            MutableComponent c = Component.literal((bad ? "▲ +" : "▼ ") + pct + "%  ").append(a.factor().label());
            if (a.ticksLeft() > 0) c.append(String.format(" (%ds)", a.ticksLeft() / 20));
            y = wrap(g, c, RX, y, bad ? BAD : GOOD, PW);
        }
    }

    // ───────────────────────── page 3 : pied marin et souvenirs ─────────────────────────

    private void legsPage(GuiGraphics g) {
        Journal j = Journal.get();
        int y = header(g, Component.translatable("seasick.logbook.legs_title"), LX, TOP);
        int rank = Journal.rank(j.seaLegs);
        centered(g, Component.translatable("seasick.rank." + rank), LX + PW / 2, y, TITLE);
        y += 11;
        y = wrap(g, Component.translatable("seasick.rank.desc." + rank), LX, y, DIM, PW);
        y += 2;
        bar(g, LX, y, PW, j.seaLegs, false, 0xFF3C7AB0);
        y += 9;
        for (int r = 0; r <= 4; r++) {
            boolean got = rank >= r;
            Component c = Component.literal(got ? "✔ " : "○ ").append(Component.translatable("seasick.rank." + r))
                .append(" · " + (r * 25) + "%");
            Gfx.text(g, font, c, LX + 4, y, got ? INK : DIM, false);
            y += 10;
        }
        y += 4;
        y = header(g, Component.translatable("seasick.logbook.log"), LX, y);
        long mins = j.ticksAtSea / 20 / 60;
        y = wrap(g, Component.translatable("seasick.logbook.time", mins / 60, String.format("%02d", mins % 60), (int) j.distance, j.voyages), LX, y, INK, PW);
        y = wrap(g, Component.translatable("seasick.logbook.sick", j.timesSick, j.overTheRail, j.dryHeaves), LX, y, INK, PW);
        long calm = j.longestCalmVoyage / 20;
        wrap(g, Component.translatable("seasick.logbook.calm", calm / 60, String.format("%02d", calm % 60)), LX, y, INK, PW);
    }

    // ───────────────────────── page 4 : le médecin de bord ─────────────────────────

    private void doctorPage(GuiGraphics g) {
        int y = header(g, Component.translatable("seasick.logbook.doctor"), RX, TOP);
        for (int i = 1; i <= 7; i++) {
            Component key = i == 3 ? Component.translatable("seasick.guide.3", ClientBoot.KEY_EYES.getTranslatedKeyMessage())
                : Component.translatable("seasick.guide." + i);
            y = wrap(g, Component.literal("• ").append(key), RX, y, i == 7 ? BAD : INK, PW);
            y += 2;
        }
    }
}
