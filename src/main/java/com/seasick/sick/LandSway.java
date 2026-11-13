package com.seasick.sick;

import com.seasick.client.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Le mal de débarquement : après une longue traversée, le sol continue de tanguer doucement.
 * Le cerveau a appris à compenser la houle et met un moment à désapprendre.
 */
public final class LandSway {

    private LandSway() {}

    public static double strength;
    private static int t, duration;
    private static double phase, prevPhase;

    public static void reset() {
        strength = 0;
        t = duration = 0;
    }

    public static boolean active() {
        return strength > 0 && t < duration;
    }

    public static void start(double a) {
        strength = Mth.clamp(a, 0, 1);
        duration = (int) ((30 + 120 * strength) * 20);
        t = 0;
        Messages.say(Component.translatable("seasick.land.start"));
    }

    public static void tick() {
        if (!active()) {
            if (strength > 0 && duration > 0) {
                strength = 0;
                Messages.maybe(Component.translatable("seasick.land.end"));
            }
            return;
        }
        t++;
        prevPhase = phase;
        phase += 2 * Math.PI / 8.0 * 0.05;
    }

    /** Enveloppe : le tangage s'éteint peu à peu. */
    public static double envelope() {
        if (!active()) return 0;
        double k = 1 - t / (double) duration;
        return strength * k * k;
    }

    public static double roll(float pt) {
        return 2.5 * envelope() * Math.sin(Mth.lerp(pt, prevPhase, phase));
    }

    public static double pitch(float pt) {
        return 1.0 * envelope() * Math.sin(Mth.lerp(pt, prevPhase, phase) * 0.7 + 1.0);
    }

    /** Dérive latérale de la marche. */
    public static double drift() {
        return 0.12 * envelope() * Math.sin(phase);
    }

    /** Un peu de malaise, presque rien. */
    public static double intensity2() {
        return 0.02 * envelope();
    }

    public static int secondsLeft() {
        return active() ? (duration - t) / 20 : 0;
    }
}
