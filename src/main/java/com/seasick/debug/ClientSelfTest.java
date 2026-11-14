package com.seasick.debug;

import com.seasick.Seasick;
import com.seasick.client.Hud;
import com.seasick.client.LogbookScreen;
import com.seasick.client.SeaClient;
import com.seasick.client.View;
import com.seasick.config.SeaConfig;
import com.seasick.platform.Platform;
import com.seasick.sea.Context;
import com.seasick.sea.SeaState;
import com.seasick.sea.Swell;
import com.seasick.sick.Factor;
import com.seasick.sick.Journal;
import com.seasick.sick.LandSway;
import com.seasick.sick.Sickness;
import com.seasick.sick.Vomit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

/**
 * Test client (CI uniquement, -Dseasick.clienttest=true) : le joueur est à la barre d'un bateau
 * (voir ServerSelfTest). On fait varier la mer, on vérifie le modèle et on prend des captures.
 */
//#if mc26
//$$ // 26.x : enregistré par ClientBoot.onClientSetup (Minecraft doit exister avant l'initialisation de la classe)
//#else
@Mod.EventBusSubscriber(modid = Seasick.MOD_ID, value = Dist.CLIENT)
//#endif
public final class ClientSelfTest {

    private ClientSelfTest() {}

    private static final boolean ENABLED = Boolean.getBoolean("seasick.clienttest");

    private record Step(String label, BooleanSupplier ready, Runnable action, int minWait) {}

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static boolean built;
    private static int inGame, waited, failures;
    private static double rollModerate, doseDown, doseHorizon;

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static void step(String label, int wait, Runnable action) {
        STEPS.add(new Step(label, () -> true, action, wait));
    }

    private static void stepWhen(String label, BooleanSupplier ready, int wait, Runnable action) {
        STEPS.add(new Step(label, ready, action, wait));
    }

    private static void shot(String name) {
        Minecraft mc = mc();
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
            c -> Seasick.LOGGER.info("[CLIENTTEST] capture {}", name));
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            Seasick.LOGGER.error("[CLIENTTEST] ÉCHEC : {}", what);
        } else {
            Seasick.LOGGER.info("[CLIENTTEST] ok : {}", what);
        }
    }

    private static boolean has(Factor f) {
        return Sickness.FACTORS.stream().anyMatch(a -> a.factor() == f);
    }

    /** Oriente le bateau (et le joueur) : yaw du bateau, écart du regard, inclinaison du regard. */
    private static void aim(float boatYaw, float lookOffset, float pitch) {
        var p = mc().player;
        if (p == null) return;
        Entity v = p.getVehicle();
        if (v != null) {
            v.setYRot(boatYaw);
            v.yRotO = boatYaw;
        }
        View.release(p);
        float yaw = boatYaw + lookOffset;
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.setXRot(pitch);
        p.xRotO = pitch;
        p.yHeadRot = p.yHeadRotO = yaw;
    }

    private static void sea(SeaState.Preset preset) {
        SeaState.preset = preset;
    }

    private static void build() {
        built = true;
        // préréglages : les vagues vont vers 60° (x+0,5 ; z+0,87) ; bateau cap 60° = houle de travers ; regard à -100° = vers le phare
        step("bateau", 1, () -> {
            check(SeaState.context == Context.BOAT, "à bord, sur l'eau (" + SeaState.context + ")");
            check(SeaState.driver, "à la barre");
            Journal.wipe();
            sea(SeaState.Preset.CALM);
            aim(60f, -100f, 2f);
        });
        step("calme", 200, () -> {
            check(SeaState.hs < 0.3, "mer calme (Hs " + SeaState.hs + ")");
            shot("01_calm");
            sea(SeaState.Preset.MODERATE);
        });
        step("agitée", 400, () -> {
            aim(60f, -100f, 2f);
            rollModerate = Swell.rollAmp;
            check(SeaState.hs > 1.2 && SeaState.hs < 3.5, "mer agitée (Hs " + String.format("%.2f", SeaState.hs) + ")");
            check(Swell.rollAmp > 3, "le bateau roule (" + String.format("%.1f", Swell.rollAmp) + "°)");
            check(SeaState.heading() == 2, "houle de travers (" + SeaState.heading() + ")");
        });
        stepWhen("agitée-capture", () -> Math.abs(View.roll) > Swell.rollAmp * 0.85, 1, () -> shot("02_moderate_roll"));
        step("tempête", 1, () -> sea(SeaState.Preset.STORM));
        step("tempête-mesure", 500, () -> {
            aim(60f, -100f, 2f);
            check(SeaState.hs > 5, "tempête (Hs " + String.format("%.2f", SeaState.hs) + ")");
            check(Swell.rollAmp > rollModerate, "la tempête roule plus fort (" + String.format("%.1f contre %.1f", Swell.rollAmp, rollModerate) + ")");
            check(Swell.intensity2 > 0.4, "dose forte en tempête (I² " + String.format("%.3f", Swell.intensity2) + ")");
        });
        stepWhen("tempête-capture", () -> Math.abs(View.roll) > Swell.rollAmp * 0.85, 1, () -> shot("03_storm"));

        // facteurs : horizon, regard baissé, barre
        step("horizon", 1, () -> aim(60f, 0f, 0f));
        step("horizon-check", 12, () -> {
            check(has(Factor.HORIZON), "les yeux sur l'horizon");
            check(has(Factor.HELM), "à la barre");
            aim(60f, 0f, 70f);
        });
        step("pieds-check", 12, () -> {
            check(has(Factor.LOOKING_DOWN), "le regard sur ses pieds");
            Sickness.dose = 0;
        });
        step("dose-pieds", 200, () -> {
            aim(60f, 0f, 70f);
            doseDown = Sickness.dose;
            check(doseDown > 0.08, "la dose monte en regardant ses pieds (" + String.format("%.3f", doseDown) + ")");
            Sickness.dose = 0;
            aim(60f, 0f, 0f);
        });
        step("dose-horizon", 200, () -> {
            doseHorizon = Sickness.dose;
            check(doseHorizon < doseDown * 0.6, "l'horizon soulage (" + String.format("%.3f contre %.3f", doseHorizon, doseDown) + ")");
            check(Sickness.equilibrium() > 0, "équilibre calculé (" + String.format("%.2f", Sickness.equilibrium()) + ")");
        });

        // stades
        step("nauséeux", 1, () -> {
            sea(SeaState.Preset.MODERATE);
            Sickness.dose = Sickness.shown = 0.45;
            aim(60f, -100f, 4f);
        });
        step("nauséeux-capture", 30, () -> {
            check(Sickness.stage == 2, "stade nauséeux (" + Sickness.stage + ")");
            Sickness.dose = Sickness.shown = 0.45;
            shot("04_queasy");
            Sickness.dose = Sickness.shown = 0.8;
        });
        step("malade-capture", 30, () -> {
            check(Sickness.stage == 3, "stade malade (" + Sickness.stage + ")");
            Sickness.dose = Sickness.shown = 0.8;
            shot("05_sick");
        });

        // carnet de bord
        step("carnet", 1, () -> {
            Sickness.dose = Sickness.shown = 0.55;
            LogbookScreen.showPage(0);
            SeaClient.openLogbook();
        });
        step("carnet-capture", 25, () -> {
            check(mc().screen instanceof LogbookScreen, "carnet de bord ouvert");
            check(has(Factor.READING), "lire en mer compte comme de la lecture");
            shot("06_logbook");
            LogbookScreen.showPage(1);
        });
        step("carnet-page-2", 6, () -> {
            shot("06b_logbook_sea_legs");
            LogbookScreen.showPage(0);
            mc().setScreen(null);
        });

        // yeux fermés, horizon artificiel
        step("yeux", 1, () -> Sickness.testEyes = true);
        step("yeux-capture", 20, () -> {
            check(has(Factor.EYES_CLOSED), "yeux fermés");
            shot("07_eyes_closed");
            Sickness.testEyes = false;
            SeaConfig.HORIZON.set(SeaConfig.Horizon.ALWAYS);
            Sickness.dose = Sickness.shown = 0.4;
            aim(60f, 0f, 22f);
        });
        step("horizon-artificiel", 30, () -> {
            check(Hud.horizonDrawn, "horizon artificiel affiché");
            shot("08_artificial_horizon");
            SeaConfig.HORIZON.set(SeaConfig.Horizon.ASSIST);
        });

        // malade, penché par-dessus bord
        step("malade", 1, () -> {
            aim(60f, 90f, 35f);
            // au-dessus du seuil : la récupération du tick ne doit pas la faire repasser dessous
            Sickness.dose = 1.05;
        });
        stepWhen("avertissement", () -> Vomit.phase == Vomit.Phase.WARNING, 1, () -> check(true, "avertissement avant d'être malade"));
        stepWhen("acte", () -> Vomit.acting() && Vomit.t >= 12, 1, () -> {
            check(Vomit.clean, "penché par-dessus bord");
            shot("09_over_the_rail");
        });
        stepWhen("soulagement", () -> !Vomit.busy(), 4, () -> {
            Journal j = Journal.get();
            check(j.timesSick == 1 && j.overTheRail == 1, "compté dans le carnet (" + j.timesSick + "/" + j.overTheRail + ")");
            check(Sickness.dose <= 0.4, "soulagé (" + String.format("%.2f", Sickness.dose) + ")");
        });
        // les facteurs sont réévalués au tick suivant la fin du malaise
        step("soulagement-facteur", 3, () -> {
            check(has(Factor.RELIEF), "facteur de soulagement");
            Sickness.onEat("honey_bottle");
        });
        step("miel", 3, () -> {
            check(has(Factor.HONEY), "le miel apaise");
            Vomit.start(true);
        });
        stepWhen("à sec", () -> !Vomit.busy(), 4, () -> check(Journal.get().dryHeaves == 1, "haut-le-cœur à sec juste après"));

        // mal de débarquement et pied marin
        step("débarquement", 1, () -> {
            sea(SeaState.Preset.CALM);
            Sickness.dose = Sickness.shown = 0.1;
            LandSway.start(0.9);
        });
        step("débarquement-check", 60, () -> {
            check(LandSway.active() && LandSway.envelope() > 0.3, "le sol tangue encore");
            Journal.get().seaLegs = 0.52;
        });
        step("pied-marin", 10, () -> {
            check(Journal.get().milestone >= 2, "rang de matelot (" + Journal.get().milestone + ")");
            check(has(Factor.SEA_LEGS), "le pied marin protège");
        });
        step("pied-marin-regard", 1, () -> aim(60f, -100f, 4f));
        step("pied-marin-capture", 5, () -> shot("10_sea_legs"));

        // confort : sans mouvement de caméra
        step("confort", 1, () -> {
            LandSway.reset();
            sea(SeaState.Preset.ROUGH);
            SeaConfig.REDUCE_MOTION.set(true);
            Sickness.dose = Sickness.shown = 0.3;
            aim(60f, -100f, 4f);
        });
        step("confort-check", 200, () -> {
            check(Math.abs(View.roll) < 1e-6, "caméra immobile en mode confort");
            check(Hud.horizonDrawn, "horizon artificiel en mode confort");
            shot("11_reduce_motion");
            SeaConfig.REDUCE_MOTION.set(false);
        });

        step("fin", 10, () -> {
            sea(SeaState.Preset.AUTO);
            Journal.get().touch();
            Journal.save();
            check(SeaClient.failures() == 0, "aucune erreur dans le mod (" + SeaClient.failures() + ")");
            Seasick.LOGGER.info("[CLIENTTEST] RESULT: {}", failures == 0 ? "OK" : "FAIL " + failures);
            mc().stop();
        });
    }

    //#if mc1201
    //$$ @SubscribeEvent
    //$$ public static void onTick(TickEvent.ClientTickEvent event) {
    //$$     if (event.phase == TickEvent.Phase.END) tick();
    //$$ }
    //#else
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent.Post event) {
        tick();
    }
    //#endif

    private static void tick() {
        if (!ENABLED) return;
        Minecraft mc = mc();
        if (mc.player == null || mc.level == null) return;
        inGame++;
        if (inGame < 200) return;
        if (!built) {
            if (mc.player.getVehicle() == null) {
                if (inGame > 1200) {
                    check(false, "le joueur n'est jamais monté dans le bateau");
                    Seasick.LOGGER.info("[CLIENTTEST] RESULT: FAIL {}", failures);
                    mc.stop();
                }
                return;
            }
            build();
            Seasick.LOGGER.info("[CLIENTTEST] début ({} étapes, cible {})", STEPS.size(), Platform.TARGET);
        }
        Step s = STEPS.peek();
        if (s == null) return;
        waited++;
        if (waited < s.minWait || !s.ready.getAsBoolean()) {
            if (waited > 1600) {
                check(false, "délai dépassé à l'étape « " + s.label + " »");
                STEPS.poll();
                waited = 0;
            }
            return;
        }
        STEPS.poll();
        waited = 0;
        try {
            s.action.run();
        } catch (Throwable t) {
            check(false, "exception à l'étape « " + s.label + " » : " + t);
            Seasick.LOGGER.error("[CLIENTTEST]", t);
        }
    }
}
