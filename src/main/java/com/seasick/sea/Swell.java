package com.seasick.sea;

import net.minecraft.util.Mth;

/**
 * Le mouvement du bateau dans la houle : roulis, tangage, embardée et pilonnement.
 *
 * - Fréquence de rencontre (effet Doppler) : face aux vagues on les croise plus vite,
 *   vent arrière on les accompagne : ωe = ω0 − ω0²·v·cos β / g.
 * - Le roulis suit la pente des vagues (π·H/L, avec L = 1,56·T²) et grandit en houle de travers ;
 *   le tangage grandit face aux vagues ou vent arrière. Un petit bateau ne « sent » pas les
 *   vagues plus courtes que lui.
 * - Les vagues arrivent par séries : une enveloppe lente fait grossir une vague sur sept environ.
 * - La dose de mal des transports suit la norme ISO 2631-1 : accélération verticale pondérée
 *   par le filtre Wf (sensibilité maximale vers 0,1–0,25 Hz), plus le clapot et les virages.
 */
public final class Swell {

    private Swell() {}

    private static final double G = 9.81;

    private static double phase, prevPhase;
    public static double rollAmp, pitchAmp, yawAmp, heaveAmp; // degrés / mètres
    /** Pulsation de rencontre (rad/s). */
    public static double omega = 1.0;
    /** Intensité pondérée au carré (m²/s⁴) : ce que reçoit l'oreille interne. */
    public static double intensity2;
    /** Événements sonores de la coque (craquement au bout du roulis, coup de bélier face aux vagues). */
    public static boolean creak, slam;

    public static void reset() {
        phase = prevPhase = 0;
        rollAmp = pitchAmp = yawAmp = heaveAmp = 0;
        intensity2 = 0;
        creak = slam = false;
    }

    /** Pondération Wf (ISO 2631-1, mal des transports), approchée : passe-bande 0,08–0,63 Hz. */
    public static double wf(double f) {
        double hp = (f / 0.05) * (f / 0.05);
        hp = hp / (1 + hp);
        double lp = 1 / Math.sqrt(1 + Math.pow(f / 0.25, 4));
        return Math.min(1.0, 1.15 * hp * lp);
    }

    /** Clapot et déferlement levés par le vent, en plus de la houle (m/s², vers 0,3 Hz). */
    public static double chop(double wind) {
        return 0.03 * Math.pow(Math.max(0.0, wind - 5.0), 1.4);
    }

    public static void tick() {
        Context ctx = SeaState.context;
        double hs = SeaState.hs, tp = SeaState.tp;
        double w0 = 2 * Math.PI / tp;
        double we = Math.abs(w0 - w0 * w0 * 0.4 * SeaState.alongWaves / G);
        we = Mth.clamp(we, 0.5 * w0, 2.0 * w0);
        omega += (we - omega) * 0.05;

        double l = 1.56 * tp * tp;
        double slope = Math.toDegrees(Math.atan(Math.PI * hs / Math.max(l, 0.5)));
        double resp = Mth.clamp((l - 2.8) / 8.0, 0.15, 1.0);
        double sb = Math.abs(SeaState.sinBeta), cb = Math.abs(SeaState.cosBeta);
        double roll = Math.min(24, (1.2 * slope + 1.6 * hs) * resp) * (0.3 + 0.7 * sb);
        double pitch = Math.min(12, (0.8 * slope + 0.9 * hs) * resp) * (0.3 + 0.7 * cb);
        double heave = hs / 2;
        if (ctx == Context.SWIM) {
            roll *= 0.6;
            pitch *= 0.6;
        } else if (ctx == Context.CAMEL) {
            // le dromadaire va l'amble : il tangue plus qu'il ne roule
            roll = 3.0 * hs / 0.35 * 0.5;
            pitch = 4.0 * hs / 0.35;
            heave = hs * 0.6;
        } else if (!ctx.onWater) {
            roll = pitch = heave = 0;
        }
        double yaw = 0.25 * roll * Math.abs(2 * SeaState.sinBeta * SeaState.cosBeta);
        double k = 0.05 / 1.5;
        rollAmp += (roll - rollAmp) * k;
        pitchAmp += (pitch - pitchAmp) * k;
        yawAmp += (yaw - yawAmp) * k;
        heaveAmp += (heave - heaveAmp) * k;

        prevPhase = phase;
        phase += omega * 0.05;
        if (phase > 2000 * Math.PI) {
            phase -= 2000 * Math.PI;
            prevPhase -= 2000 * Math.PI;
        }

        // sons de coque : au bout du roulis, et quand l'étrave retombe face aux vagues
        double a = Math.floorMod((long) Math.floor(prevPhase / (Math.PI / 2)), 4L);
        double b = Math.floorMod((long) Math.floor(phase / (Math.PI / 2)), 4L);
        creak = ctx == Context.BOAT && a != b && (b == 1 || b == 3) && rollAmp > 5;
        slam = ctx == Context.BOAT && a != b && b == 2 && pitchAmp > 4 && SeaState.heading() >= 3 && SeaState.speed > 3;

        // dose : accélérations pondérées
        double f = omega / (2 * Math.PI);
        double w = wf(f);
        double om2 = omega * omega / Math.sqrt(2);
        double aH = heaveAmp * om2;
        double aR = 1.2 * Math.toRadians(rollAmp) * om2;
        double aP = 0.8 * Math.toRadians(pitchAmp) * om2;
        double i2 = w * w * (aH * aH + 0.5 * aR * aR + 0.4 * aP * aP);
        if (ctx == Context.BOAT || ctx == Context.SWIM) {
            double c = wf(0.3) * chop(SeaState.windSpeed) * Math.min(1.0, hs / 0.5);
            if (ctx == Context.SWIM) c *= 0.7;
            i2 += c * c;
        }
        if (ctx == Context.BOAT || ctx == Context.BOAT_DRY) {
            double lat = Math.min(6.0, SeaState.speed * Math.abs(SeaState.yawRate));
            i2 += (0.1 * lat) * (0.1 * lat);
        }
        if (ctx == Context.CAMEL) i2 *= 2.0;
        intensity2 = i2;
    }

    private static double ph(float pt) {
        return Mth.lerp(pt, prevPhase, phase);
    }

    /** Enveloppe des séries de vagues : la septième vague est la plus forte. */
    private static double sets(double p) {
        return 0.7 + 0.3 * Math.sin(p / 7.3);
    }

    public static double roll(float pt) {
        double p = ph(pt);
        return rollAmp * sets(p) * (0.75 * Math.sin(p) + 0.25 * Math.sin(1.37 * p + 1.1));
    }

    public static double pitch(float pt) {
        double p = ph(pt);
        return pitchAmp * sets(p) * (0.7 * Math.sin(p + Math.PI / 2) + 0.3 * Math.sin(0.83 * p + 2.3));
    }

    public static double yaw(float pt) {
        double p = ph(pt);
        return yawAmp * sets(p) * Math.sin(p + 0.9);
    }

    /** Pilonnement (m) : positif quand le bateau monte. */
    public static double heave(float pt) {
        double p = ph(pt);
        return heaveAmp * sets(p) * Math.sin(p - Math.PI / 2);
    }

    /** Pour la jauge : position dans le cycle (0..2π). */
    public static double cycle(float pt) {
        return Math.floorMod((long) (ph(pt) * 1000), (long) (2 * Math.PI * 1000)) / 1000.0;
    }
}
