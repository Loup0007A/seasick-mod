package com.seasick.client;

import com.seasick.config.SeaConfig;
import com.seasick.sick.LandSway;
import com.seasick.sick.Sickness;
import com.seasick.sick.Vomit;
import com.seasick.sea.Swell;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * Ce que la houle fait à votre regard.
 *
 * Le roulis fait pencher l'image (l'horizon bascule). Le tangage et l'embardée bougent
 * réellement votre regard — et donc votre visée — comme sur un vrai bateau : on n'ajoute
 * à la rotation du joueur que la différence avec l'image précédente, si bien que tout
 * revient en place à la fin de chaque vague et que la souris reste libre.
 */
public final class View {

    private View() {}

    /** Décalages actuellement appliqués à la rotation du joueur (degrés). */
    public static double appliedPitch, appliedYaw;
    /** Roulis et tangage « image seule » de la frame en cours. */
    public static double roll, cameraPitch;
    /** Variation de champ de vision (pilonnement, haut-le-cœur). */
    public static double fov;

    private static double dip, dipVel;
    private static double tremorX, tremorY;
    private static final Random RNG = new Random();

    public static void reset() {
        appliedPitch = appliedYaw = 0;
        roll = cameraPitch = fov = 0;
        dip = dipVel = 0;
    }

    /** Un haut-le-cœur : la tête plonge d'un coup. */
    public static void dip(float degrees) {
        dipVel += degrees * 0.6;
    }

    /** Appelé au début de chaque image, avant que la caméra ne soit placée. */
    public static void frame(LocalPlayer p, float pt, float frameTicks) {
        double motion = SeaConfig.motion();
        double rollT = (Swell.roll(pt) + LandSway.roll(pt)) * motion;
        double pitchT = (Swell.pitch(pt) + LandSway.pitch(pt)) * motion;
        double yawT = Swell.yaw(pt) * motion;

        // haut-le-cœur : petit ressort amorti
        double dt = Math.min(frameTicks, 3f);
        dipVel += (-dip * 0.35 - dipVel * 0.45) * dt;
        dip += dipVel * dt;
        if (Math.abs(dip) < 1e-3 && Math.abs(dipVel) < 1e-3) dip = dipVel = 0;
        pitchT += dip * Math.max(0.3, motion);

        // pendant l'acte : la tête tremble
        if (Vomit.acting()) rollT += Math.sin((Vomit.t + pt) * 2.7) * 2.5 * Math.max(0.3, motion);

        // tremblement des mains quand on est vraiment mal
        if (Sickness.stage >= 3 && SeaConfig.get(SeaConfig.AFFECT_CONTROLS, true)) {
            double k = (Sickness.stage - 2) * 0.12;
            tremorX += ((RNG.nextDouble() - 0.5) * k - tremorX * 0.2) * dt;
            tremorY += ((RNG.nextDouble() - 0.5) * k - tremorY * 0.2) * dt;
            p.setYRot(p.getYRot() + (float) tremorX);
            p.yRotO += (float) tremorX;
            p.setXRot(Mth.clamp(p.getXRot() + (float) tremorY, -90f, 90f));
            p.xRotO += (float) tremorY;
        }

        roll = rollT;
        boolean aim = SeaConfig.get(SeaConfig.SWAY_AIM, true);
        if (aim) {
            cameraPitch = 0;
            double dp = pitchT - appliedPitch, dy = yawT - appliedYaw;
            float nx = Mth.clamp(p.getXRot() + (float) dp, -90f, 90f);
            dp = nx - p.getXRot();
            p.setXRot(nx);
            p.xRotO += (float) dp;
            p.setYRot(p.getYRot() + (float) dy);
            p.yRotO += (float) dy;
            appliedPitch += dp;
            appliedYaw += dy;
        } else {
            release(p);
            cameraPitch = pitchT;
        }
        fov = -Swell.heave(pt) * 1.4 * motion;
    }

    /** Rend à la souris les décalages en cours (fin de traversée, réglage changé). */
    public static void release(LocalPlayer p) {
        if (appliedPitch != 0 || appliedYaw != 0) {
            float nx = Mth.clamp(p.getXRot() - (float) appliedPitch, -90f, 90f);
            p.xRotO += nx - p.getXRot();
            p.setXRot(nx);
            p.setYRot(p.getYRot() - (float) appliedYaw);
            p.yRotO -= (float) appliedYaw;
        }
        appliedPitch = appliedYaw = 0;
    }
}
