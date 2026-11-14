package com.seasick.sick;

import com.seasick.client.LogbookScreen;
import com.seasick.client.Messages;
import com.seasick.client.Sounds;
import com.seasick.client.View;
import com.seasick.config.SeaConfig;
import com.seasick.platform.Compat;
import com.seasick.sea.Context;
import com.seasick.sea.SeaState;
import com.seasick.sea.Swell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Le mal de mer lui-même : une « dose » qui monte avec les accélérations ressenties et
 * redescend quand le corps récupère.
 *
 *   dS/dt = K · I² · M − S / τ
 *
 * I² vient de la houle (voir {@link Swell}), M est le produit des facteurs (horizon, barre,
 * estomac, lecture...), τ est le temps de récupération (plus long quand on est déjà mal, plus
 * court à terre). Chaque situation a donc un équilibre : bien se tenir peut suffire à ne
 * jamais être malade, même dans la tempête. À S = 1, on est malade.
 */
public final class Sickness {

    private Sickness() {}

    public static final double K = 0.016;
    public static final double TAU_SEA = 70.0;
    public static final double TAU_LAND = 35.0;
    private static final int[] UP = {15, 35, 60, 85};

    /** Dose : 0 = en forme, 1 = malade. */
    public static double dose;
    /** Dose affichée (lissée). */
    public static double shown;
    /** 0 en forme, 1 barbouillé, 2 nauséeux, 3 malade, 4 au bord du malaise. */
    public static int stage;
    public static double multiplier = 1.0;
    public static final List<Factor.Active> FACTORS = new ArrayList<>();

    public static boolean eyesClosed;
    /** Tests automatiques : yeux fermés sans touche. */
    public static boolean testEyes;
    public static boolean artificialHorizonShown;
    public static boolean horizonVisible;

    public static int yawnTicks;
    public static int fumbleTicks, fumbleDir;
    public static int reliefTicks;

    private static final Random RNG = new Random();
    private static int viewTimer;
    private static double horizonFree = 99, lookDist = 99;
    private static int yawnTimer = 400, gulpTimer = 300, stomachTimer = 300, sweatTimer = 40, fumbleTimer = 200;

    // traversée en cours
    private static boolean onTrip, tripSick;
    private static long tripTicks;
    private static double tripHsSum;
    private static int ashoreTicks;

    public static double q() {
        return dose * 100;
    }

    public static void reset() {
        dose = shown = 0;
        stage = 0;
        multiplier = 1;
        FACTORS.clear();
        eyesClosed = false;
        yawnTicks = fumbleTicks = reliefTicks = 0;
        onTrip = tripSick = false;
        tripTicks = 0;
        tripHsSum = 0;
        ashoreTicks = 0;
    }

    public static boolean immune(LocalPlayer p) {
        return p.isSpectator() || (p.isCreative() && SeaConfig.get(SeaConfig.CREATIVE_IMMUNE, false));
    }

    public static void tick(LocalPlayer p) {
        Stomach.tick();
        if (reliefTicks > 0) reliefTicks--;
        if (yawnTicks > 0) yawnTicks--;
        if (fumbleTicks > 0) fumbleTicks--;
        Context ctx = SeaState.context;
        boolean immune = immune(p);

        evaluate(p);
        double i2 = Swell.intensity2 + LandSway.intensity2();
        double tau = (ctx.moving() ? TAU_SEA : TAU_LAND) * (1 + 0.6 * dose);
        double gain = (immune || Vomit.busy()) ? 0 : K * i2 * multiplier;
        dose += (gain - dose / tau) * 0.05;
        dose = Mth.clamp(dose, 0, 1.3);
        if (immune) dose = Math.min(dose, 0.99);
        shown += (dose - shown) * 0.1;

        stages();
        symptoms(p, ctx);
        if (dose >= 1.0 && !Vomit.busy()) Vomit.start(false);
        voyage(p, ctx);
    }

    /** Équilibre vers lequel tend la dose si rien ne change (∞ si on finira forcément malade). */
    public static double equilibrium() {
        double a = TAU_SEA * K * (Swell.intensity2 + LandSway.intensity2()) * multiplier;
        if (!SeaState.context.moving()) a = TAU_LAND * K * LandSway.intensity2() * multiplier;
        if (0.6 * a >= 1) return Double.POSITIVE_INFINITY;
        return a / (1 - 0.6 * a);
    }

    /** Secondes avant d'être malade si rien ne change, ou -1. */
    public static int secondsUntilSick() {
        double eq = equilibrium();
        if (eq < 1.0) return -1;
        double s = dose, rate = K * (Swell.intensity2 + LandSway.intensity2()) * multiplier;
        for (int i = 0; i < 2400; i++) { // pas de 0,25 s, 10 minutes au plus
            if (s >= 1.0) return i / 4;
            s += (rate - s / (TAU_SEA * (1 + 0.6 * s))) * 0.25;
        }
        return -1;
    }

    // ───────────────────────── facteurs ─────────────────────────

    private static void add(Factor f, double mult, int ticks) {
        FACTORS.add(new Factor.Active(f, mult, ticks));
    }

    private static void add(Factor f) {
        add(f, f.base, -1);
    }

    private static void evaluate(LocalPlayer p) {
        Minecraft mc = Minecraft.getInstance();
        FACTORS.clear();
        Level level = p.level();
        Context ctx = SeaState.context;
        float pitch = p.getXRot() - (float) View.appliedPitch;
        boolean dark = level.getSkyDarken() >= 8 || level.getThunderLevel(1f) > 0.5f;

        if (--viewTimer <= 0) {
            viewTimer = 4;
            Vec3 eye = p.getEyePosition();
            float yaw = p.getYRot();
            Vec3 flat = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
            horizonFree = ray(level, p, eye, eye.add(flat.scale(48)));
            lookDist = ray(level, p, eye, eye.add(p.getViewVector(1f).scale(16)));
        }
        horizonVisible = false;

        Screen screen = mc.screen;
        if (eyesClosed) {
            add(Factor.EYES_CLOSED);
        } else if (screen instanceof BookViewScreen || screen instanceof BookEditScreen) {
            add(Factor.BOOK);
        } else if (screen instanceof AbstractContainerScreen<?> || screen instanceof ChatScreen
            || screen instanceof AbstractSignEditScreen || screen instanceof LogbookScreen) {
            add(Factor.READING);
        } else if (p.isScoping()) {
            add(Factor.SPYGLASS);
        } else if (pitch > 20 && (holding(p, InteractionHand.MAIN_HAND) || holding(p, InteractionHand.OFF_HAND))) {
            add(Factor.MAP);
        } else if (pitch > 45) {
            add(Factor.LOOKING_DOWN);
        } else if (lookDist < 4) {
            add(Factor.CLOSE_VIEW);
        } else if (Math.abs(pitch) <= 15 && horizonFree >= 24) {
            horizonVisible = true;
            boolean dim = dark || level.getRainLevel(1f) > 0.6f;
            add(dim ? Factor.HORIZON_DIM : Factor.HORIZON);
        } else if (dark) {
            add(Factor.DARK);
        }
        if (!horizonVisible && !eyesClosed && artificialHorizonShown && screen == null) add(Factor.ARTIFICIAL_HORIZON);
        if (!mc.options.getCameraType().isFirstPerson()) add(Factor.THIRD_PERSON);

        if (ctx == Context.BOAT || ctx == Context.BOAT_DRY || ctx == Context.CAMEL) {
            add(SeaState.driver ? Factor.HELM : Factor.PASSENGER);
        }
        if (p.getFoodData().getFoodLevel() <= 6) add(Factor.EMPTY_STOMACH);
        for (Map.Entry<Factor, Integer> e : Stomach.timers().entrySet()) add(e.getKey(), e.getKey().base, e.getValue());
        if (Compat.hasNausea(p)) add(Factor.NAUSEA);
        if (reliefTicks > 0) add(Factor.RELIEF, Factor.RELIEF.base, reliefTicks);

        double legs = Journal.get().seaLegs;
        if (SeaConfig.get(SeaConfig.SEA_LEGS, true) && legs > 0.005) add(Factor.SEA_LEGS, 1 - 0.6 * legs, -1);
        double sus = SeaConfig.get(SeaConfig.SUSCEPTIBILITY, 1.0);
        if (Math.abs(sus - 1) > 0.01) add(Factor.SUSCEPTIBILITY, sus, -1);

        double m = 1;
        for (Factor.Active a : FACTORS) m *= a.mult();
        multiplier = Mth.clamp(m, 0.1, 6.0);
    }

    private static boolean holding(LocalPlayer p, InteractionHand hand) {
        return Compat.itemPath(p.getItemInHand(hand)).equals("filled_map");
    }

    private static double ray(Level level, LocalPlayer p, Vec3 from, Vec3 to) {
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.MISS) return 99;
        return hit.getLocation().distanceTo(from);
    }

    // ───────────────────────── stades et symptômes ─────────────────────────

    private static void stages() {
        double q = q();
        int s = stage;
        while (s < 4 && q >= UP[s]) s++;
        while (s > 0 && q < UP[s - 1] - 5) s--;
        if (s > stage) {
            if (s < 4) Messages.say(Messages.symptom(Component.translatable("seasick.stage.up." + s), Messages.tip()));
            else Messages.say(Messages.tint(Component.translatable("seasick.stage.up.4"), 0xFF9A5A, true));
        } else if (s == 0 && stage >= 2) {
            Messages.say(Messages.tint(Component.translatable("seasick.better"), 0x9AE0A0, true));
        }
        stage = s;
    }

    private static void symptoms(LocalPlayer p, Context ctx) {
        if (stage >= 1 && --yawnTimer <= 0) {
            // somnolence (syndrome « sopite ») : on bâille, les paupières tombent
            yawnTimer = 400 + RNG.nextInt(800) - stage * 80;
            yawnTicks = 40;
        }
        if (stage >= 2 && --gulpTimer <= 0) {
            gulpTimer = 240 + RNG.nextInt(360) - stage * 40;
            Sounds.gulp();
        }
        if (stage >= 3 && --stomachTimer <= 0) {
            stomachTimer = 300 + RNG.nextInt(200);
            Sounds.stomach();
        }
        if (stage >= 2 && --sweatTimer <= 0) {
            // sueurs froides : visibles en vue extérieure
            sweatTimer = 30 + RNG.nextInt(30);
            if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                Vec3 e = p.getEyePosition();
                p.level().addParticle(ParticleTypes.FALLING_WATER, e.x + (RNG.nextDouble() - 0.5) * 0.5, e.y + 0.15, e.z + (RNG.nextDouble() - 0.5) * 0.5, 0, 0, 0);
            }
        }
        if (stage >= 3 && ctx == Context.BOAT && SeaState.driver && SeaConfig.get(SeaConfig.AFFECT_CONTROLS, true) && --fumbleTimer <= 0) {
            // les mains moites lâchent une rame
            fumbleTimer = 160 + RNG.nextInt(140);
            fumbleTicks = 8;
            fumbleDir = RNG.nextBoolean() ? 1 : -1;
        }
        if (Swell.creak && RNG.nextFloat() < 0.6f) Sounds.creak();
        if (Swell.slam) Sounds.slam();
    }

    // ───────────────────────── repas ─────────────────────────

    public static void onEat(String item) {
        onEat(item, "minecraft:" + item);
    }

    /** @param item nom de l'objet (« bread ») @param id identifiant complet (« minecraft:bread ») */
    public static void onEat(String item, String id) {
        double before = dose;
        dose = Mth.clamp(dose + Stomach.eat(item, id), 0, 1.3);
        if (Stomach.isRich(item) && before >= 0.6 && RNG.nextFloat() < 0.35f && !Vomit.busy()) {
            Messages.say(Component.translatable("seasick.food.wont_stay"));
            Vomit.start(true);
        } else if (item.equals("honey_bottle")) {
            Messages.maybe(Component.translatable("seasick.food.honey"));
        } else if (Stomach.isRich(item) && SeaState.context.moving()) {
            Messages.maybe(Component.translatable("seasick.food.heavy"));
        }
    }

    // ───────────────────────── traversées et pied marin ─────────────────────────

    private static void voyage(LocalPlayer p, Context ctx) {
        Journal j = Journal.get();
        if (ctx == Context.BOAT) {
            if (!onTrip) {
                onTrip = true;
                tripSick = false;
                tripTicks = 0;
                tripHsSum = 0;
            }
            ashoreTicks = 0;
            tripTicks++;
            tripHsSum += SeaState.hs;
            j.ticksAtSea++;
            j.distance += SeaState.speed * 0.05;
            if (j.ticksAtSea % 200 == 0) j.touch();
        } else if (onTrip && ++ashoreTicks >= 60) {
            onTrip = false;
            j.voyages++;
            j.lastVoyage = tripTicks;
            if (!tripSick) j.longestCalmVoyage = Math.max(j.longestCalmVoyage, tripTicks);
            j.touch();
            double avgHs = tripHsSum / Math.max(1, tripTicks);
            if (ctx == Context.LAND && tripTicks >= 90 * 20 && SeaConfig.get(SeaConfig.LAND_SICKNESS, true)) {
                double a = Mth.clamp(tripTicks / 20.0 / 600.0 * avgHs / 2.0, 0.15, 1.0) * (1 - 0.6 * j.seaLegs);
                if (a >= 0.12) LandSway.start(a);
            }
        }

        if (SeaConfig.get(SeaConfig.SEA_LEGS, true)) {
            double before = j.seaLegs;
            if (SeaState.atSea() && SeaState.hs >= 0.3) {
                double rate = SeaConfig.get(SeaConfig.SEA_LEGS_RATE, 1.0);
                j.seaLegs += 0.05 * Math.min(SeaState.hs, 4.0) / 2.5 / 2400.0 * rate;
            } else if (ctx == Context.LAND) {
                j.seaLegs -= 0.05 / (3 * 3600.0);
            }
            j.seaLegs = Mth.clamp(j.seaLegs, 0, 1);
            int rank = Journal.rank(j.seaLegs);
            if (rank > j.milestone) {
                j.milestone = rank;
                j.touch();
                Compat.toast(Component.translatable("seasick.rank.toast"),
                    Component.translatable("seasick.rank." + rank).append(" — ").append(Component.translatable("seasick.rank.desc." + rank)));
                Sounds.play("entity.player.levelup", 0.4f, 1.3f);
            }
            if ((int) (before * 1000) != (int) (j.seaLegs * 1000)) j.touch();
        }
    }

    public static void markSick() {
        tripSick = true;
    }

    public static long tripTicks() {
        return onTrip ? tripTicks : 0;
    }
}
