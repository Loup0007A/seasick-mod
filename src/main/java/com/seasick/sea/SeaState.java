package com.seasick.sea;

import com.seasick.config.SeaConfig;
import com.seasick.platform.Compat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * L'état de la mer autour du joueur, calculé uniquement côté client.
 *
 * Le vent (force et direction) varie lentement et dépend de la météo. La hauteur de la houle
 * suit la formule de Pierson–Moskowitz pour une mer pleinement levée (Hs ≈ 0,0214·U²),
 * limitée par la longueur d'eau libre au vent (« fetch »), par la profondeur (une vague
 * déferle quand elle dépasse ~0,6 fois le fond), par le biome (glace, rivière, marais) et
 * par un toit (pas de vent sous terre). La période suit la même loi (Tp ≈ 0,73·U).
 */
public final class SeaState {

    private SeaState() {}

    /** Mers imposées (commande /seasick sea, tests). */
    public enum Preset {
        AUTO(-1), CALM(2), SLIGHT(6), MODERATE(10), ROUGH(13), STORM(19), PHENOMENAL(26);

        public final double wind;

        Preset(double wind) {
            this.wind = wind;
        }
    }

    public static Preset preset = Preset.AUTO;

    // ── contexte ──
    public static Context context = Context.LAND;
    public static Entity vehicle;
    public static boolean driver;
    public static String worldSeed = "";

    // ── environnement (échantillonné) ──
    public static double windSpeed;      // m/s
    public static double windDir;        // rad : direction VERS laquelle souffle le vent (et vont les vagues)
    public static int depth;             // blocs d'eau sous la surface
    public static int fetch;             // blocs d'eau libre au vent
    public static double biomeFactor = 1.0;
    public static boolean covered;
    public static String biome = "";
    public static int surfaceY;

    // ── houle (lissée) ──
    public static double hs;             // hauteur significative (m)
    public static double tp = 4.0;       // période de pic (s)
    private static double hsTarget, tpTarget = 4.0;

    // ── cinématique du véhicule ──
    public static double speed;          // blocs/s
    public static double alongWaves;     // composante de la vitesse dans le sens des vagues (b/s)
    public static double yawRate;        // rad/s
    public static double cosBeta = 1.0;  // cap du bateau · sens des vagues (1 = vagues qui poussent par l'arrière)
    public static double sinBeta;

    private static Vec3 lastPos;
    private static float lastYaw;
    private static int sampleTimer;
    private static Context lastSampled = Context.LAND;

    public static final int[] DOUGLAS_LIMITS_CM = {5, 10, 50, 125, 250, 400, 600, 900, 1400};
    public static final double[] BEAUFORT = {0.5, 1.6, 3.4, 5.5, 8.0, 10.8, 13.9, 17.2, 20.8, 24.5, 28.5, 32.7};

    public static void reset() {
        context = Context.LAND;
        vehicle = null;
        driver = false;
        hs = hsTarget = 0;
        tp = tpTarget = 4.0;
        speed = alongWaves = yawRate = 0;
        lastPos = null;
        sampleTimer = 0;
    }

    public static void tick(LocalPlayer p) {
        Level level = p.level();
        Context before = context;
        context = detect(p);
        Entity carrier = vehicle != null ? vehicle : p;
        kinematics(carrier);
        wind(level);

        if (context.onWater && (--sampleTimer <= 0 || before != context || lastSampled != context)) {
            sample(level, carrier);
            sampleTimer = 20;
            lastSampled = context;
        }

        if (context.onWater) {
            double dev = 1 - Math.exp(-fetch / 60.0);
            double h = 0.0214 * windSpeed * windSpeed * dev * biomeFactor;
            h = Math.min(Math.min(h, 0.6 * Math.max(1, depth)), 10.0);
            hsTarget = Math.max(0.04, h);
            tpTarget = Math.max(1.4, 0.73 * windSpeed * Math.pow(Math.max(dev, 0.01), 0.3));
            if (covered) {
                hsTarget = Math.min(hsTarget, 0.08);
                tpTarget = 2.0;
            }
        } else if (context == Context.CAMEL) {
            // le pas du dromadaire : un balancement court, seulement quand il marche
            hsTarget = 0.35 * Mth.clamp(speed / 3.0, 0.0, 1.0);
            tpTarget = 1.7;
        } else {
            hsTarget = 0;
        }
        double k = hsTarget > hs ? 0.05 / 8.0 : 0.05 / (context.onWater ? 8.0 : 1.5);
        hs += (hsTarget - hs) * k;
        tp += (tpTarget - tp) * 0.05 / 6.0;
        if (Math.abs(hs) < 1e-4) hs = 0;
    }

    private static Context detect(LocalPlayer p) {
        vehicle = p.getVehicle();
        driver = false;
        if (vehicle != null) {
            String type = Compat.typePath(vehicle);
            driver = vehicle.getControllingPassenger() == p;
            if (type.endsWith("boat") || type.endsWith("raft")) {
                return onWater(vehicle) ? Context.BOAT : Context.BOAT_DRY;
            }
            if (type.equals("camel") && SeaConfig.get(SeaConfig.CAMELS, true)) return Context.CAMEL;
            return Context.LAND;
        }
        if (p.isInWater() && !p.onGround()) {
            // les vagues ne se sentent qu'en surface
            Level level = p.level();
            BlockPos eye = BlockPos.containing(p.getEyePosition());
            for (int dy = 0; dy <= 2; dy++) {
                if (!level.getFluidState(eye.above(dy)).is(FluidTags.WATER)) return Context.SWIM;
            }
        }
        return Context.LAND;
    }

    private static boolean onWater(Entity boat) {
        Level level = boat.level();
        return level.getFluidState(BlockPos.containing(boat.getX(), boat.getY() - 0.05, boat.getZ())).is(FluidTags.WATER)
            || level.getFluidState(BlockPos.containing(boat.getX(), boat.getY() + 0.3, boat.getZ())).is(FluidTags.WATER);
    }

    private static void kinematics(Entity e) {
        Vec3 pos = e.position();
        float yaw = e.getYRot();
        if (lastPos == null) {
            lastPos = pos;
            lastYaw = yaw;
        }
        double vx = (pos.x - lastPos.x) * 20, vz = (pos.z - lastPos.z) * 20;
        double v = Math.sqrt(vx * vx + vz * vz);
        if (v > 60) v = 0; // téléportation
        speed += (v - speed) * 0.35;
        yawRate += (Math.toRadians(Mth.wrapDegrees(yaw - lastYaw)) * 20 - yawRate) * 0.35;
        lastPos = pos;
        lastYaw = yaw;

        double wx = Math.cos(windDir), wz = Math.sin(windDir);
        alongWaves = v > 60 ? 0 : vx * wx + vz * wz;
        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        cosBeta = fx * wx + fz * wz;
        sinBeta = fx * wz - fz * wx;
    }

    private static void wind(Level level) {
        if (preset != Preset.AUTO) {
            windSpeed += (preset.wind - windSpeed) * 0.05;
            windDir = Math.toRadians(60);
            return;
        }
        long seed = Wind.seed(worldSeed + "|" + Compat.dimension(level));
        double t = level.getGameTime() / 20.0;
        double calm = 2.5 + 6.0 * Wind.fractal(seed, t / 480.0);
        double rainy = 9.0 + 5.0 * Wind.fractal(seed + 1, t / 300.0);
        double stormy = 17.0 + 9.0 * Wind.fractal(seed + 2, t / 200.0);
        double u = Mth.lerp(level.getRainLevel(1f), calm, rainy);
        u = Mth.lerp(level.getThunderLevel(1f), u, stormy);
        windSpeed = u;
        windDir = 4 * Math.PI * Wind.noise(seed + 3, t / 1200.0);
    }

    /** Fond, eau libre au vent, biome, toit : relevés une fois par seconde. */
    private static void sample(Level level, Entity e) {
        BlockPos base = BlockPos.containing(e.getX(), e.getY() + 0.3, e.getZ());
        // surface : premier bloc sans eau au-dessus
        int y = base.getY();
        while (level.getFluidState(new BlockPos(base.getX(), y, base.getZ())).is(FluidTags.WATER) && y < base.getY() + 4) y++;
        surfaceY = y;
        int d = 0;
        while (d < 48 && level.getFluidState(new BlockPos(base.getX(), y - 1 - d, base.getZ())).is(FluidTags.WATER)) d++;

        if (preset != Preset.AUTO) {
            depth = 40;
            fetch = 200;
            biomeFactor = 1.0;
            covered = false;
            biome = "preset";
            return;
        }
        depth = Math.max(1, d);

        // longueur d'eau libre en remontant le vent
        double ux = -Math.cos(windDir), uz = -Math.sin(windDir);
        int f = 4;
        for (; f <= 192; f += 4) {
            BlockPos q = BlockPos.containing(e.getX() + ux * f, y - 1, e.getZ() + uz * f);
            if (!level.isLoaded(q)) continue; // au-delà de la vue : on suppose le large
            if (!level.getFluidState(q).is(FluidTags.WATER)) break;
        }
        fetch = Math.min(f, 192);

        Holder<Biome> h = level.getBiome(base);
        biome = Compat.biomePath(h);
        double bf;
        if (h.is(BiomeTags.IS_DEEP_OCEAN)) bf = 1.0;
        else if (h.is(BiomeTags.IS_OCEAN)) bf = 0.85;
        else if (h.is(BiomeTags.IS_RIVER)) bf = 0.3;
        else if (h.is(BiomeTags.IS_BEACH)) bf = 0.6;
        else if (biome.contains("swamp") || biome.contains("mangrove")) bf = 0.2;
        else bf = 0.35;
        if (biome.contains("frozen")) bf *= 0.5; // la banquise amortit la houle
        biomeFactor = bf;

        covered = level.getHeight(Heightmap.Types.MOTION_BLOCKING, base.getX(), base.getZ()) > y + 1;
    }

    // ── échelles ──

    /** Échelle de Douglas (état de la mer), 0 à 9. */
    public static int douglas(double hs) {
        int cm = (int) Math.round(hs * 100);
        for (int i = 0; i < DOUGLAS_LIMITS_CM.length; i++) if (cm < DOUGLAS_LIMITS_CM[i]) return i;
        return 9;
    }

    /** Échelle de Beaufort (force du vent), 0 à 12. */
    public static int beaufort(double u) {
        for (int i = 0; i < BEAUFORT.length; i++) if (u < BEAUFORT[i]) return i;
        return 12;
    }

    /** Allure par rapport aux vagues : 0 vent arrière, 1 grand largue, 2 de travers, 3 près, 4 debout. */
    public static int heading() {
        double a = Math.toDegrees(Math.acos(Mth.clamp(cosBeta, -1, 1)));
        if (a < 30) return 0;
        if (a < 60) return 1;
        if (a < 120) return 2;
        if (a < 150) return 3;
        return 4;
    }

    /** Point cardinal d'où vient le vent (0 = nord, puis sens horaire par 45°). */
    public static int windFrom() {
        // windDir : angle dans le plan (x, z) ; le vent VIENT de la direction opposée
        double fx = -Math.cos(windDir), fz = -Math.sin(windDir);
        double deg = Math.toDegrees(Math.atan2(fx, -fz)); // 0 = nord (-z), 90 = est (+x)
        return Math.floorMod((int) Math.round(deg / 45.0), 8);
    }

    public static boolean atSea() {
        return context.onWater && hs > 0.03;
    }
}
