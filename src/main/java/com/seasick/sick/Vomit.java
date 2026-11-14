package com.seasick.sick;

import com.seasick.client.Messages;
import com.seasick.client.Sounds;
import com.seasick.client.View;
import com.seasick.config.SeaConfig;
import com.seasick.platform.Compat;
import com.seasick.sea.Context;
import com.seasick.sea.SeaState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Le moment où l'on est malade : un avertissement (deux haut-le-cœur, le temps de se pencher
 * par-dessus bord), puis l'acte, puis le soulagement — réel mais passager.
 *
 * Penché par-dessus le bastingage, c'est propre et le soulagement est plus grand. Sinon...
 * c'est dans le bateau. Quand l'estomac est déjà vide, il ne reste que des haut-le-cœur à sec.
 */
public final class Vomit {

    private Vomit() {}

    public enum Phase { IDLE, WARNING, ACT }

    public static final int WARNING_TICKS = 70;
    public static final int ACT_TICKS = 36;

    public static Phase phase = Phase.IDLE;
    public static int t;
    public static boolean clean, dry, overWater;
    private static long lastAct = Long.MIN_VALUE / 2;
    private static Vec3 landing;
    private static final Random RNG = new Random();

    public static boolean busy() {
        return phase != Phase.IDLE;
    }

    public static boolean acting() {
        return phase == Phase.ACT;
    }

    public static void reset() {
        phase = Phase.IDLE;
        t = 0;
    }

    /** @param now sans avertissement (le repas qui ne passe pas) */
    public static void start(boolean now) {
        if (busy()) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        if (now) {
            act(p);
        } else {
            phase = Phase.WARNING;
            t = 0;
            Component lean = SeaState.context == Context.BOAT ? Component.translatable("seasick.vomit.lean")
                : Component.translatable("seasick.vomit.coming");
            Messages.say(Messages.tint(lean, 0xFFB060, false));
        }
    }

    /** Fin de l'avertissement, du haut-le-cœur ou de l'acte : rattrape aussi les retards. */
    public static void tick(LocalPlayer p) {
        if (phase == Phase.WARNING) {
            t++;
            if (t == 8 || t == 38) {
                Sounds.heave();
                View.dip(6f);
            }
            if (t >= WARNING_TICKS) act(p);
        } else if (phase == Phase.ACT) {
            t++;
            if (SeaConfig.get(SeaConfig.AFFECT_CONTROLS, true)) {
                // on se penche : le regard descend (par-dessus bord, juste assez pour voir l'eau au-delà du plat-bord)
                float target = clean ? 42f : 68f;
                float x = p.getXRot();
                float nx = x + Mth.clamp(target - x, -9f, 9f);
                p.xRotO = p.getXRot();
                p.setXRot(nx);
            }
            if (t == 3) View.dip(8f);
            if (!dry && t >= 4 && t <= 30 && SeaConfig.get(SeaConfig.GROSS_EFFECTS, true)) particles(p);
            if (!dry && t == 14) Sounds.splash(overWater && clean);
            if (t >= ACT_TICKS) finish(p);
        }
    }

    private static void act(LocalPlayer p) {
        phase = Phase.ACT;
        t = 0;
        Level level = p.level();
        dry = level.getGameTime() - lastAct < 90 * 20 || Stomach.meal().isEmpty() && p.getFoodData().getFoodLevel() <= 4;
        lastAct = level.getGameTime();
        Context ctx = SeaState.context;
        Entity v = SeaState.vehicle;
        float pitch = p.getXRot() - (float) View.appliedPitch;
        if ((ctx == Context.BOAT || ctx == Context.CAMEL) && v != null) {
            float side = Math.abs(Mth.wrapDegrees(p.getYRot() - v.getYRot()));
            clean = side >= 50 && side <= 130 && pitch > 15;
        } else {
            clean = pitch > 25 || ctx == Context.SWIM;
        }
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1f);
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        landing = clean ? eye.add(dir.scale(1.3)).add(0, -1.6, 0) : eye.add(dir.scale(0.6)).add(0, -1.4, 0);
        overWater = level.getFluidState(BlockPos.containing(landing.x, landing.y + 0.5, landing.z)).is(FluidTags.WATER)
            || level.getFluidState(BlockPos.containing(landing.x, landing.y, landing.z)).is(FluidTags.WATER);
        Sounds.retch(dry);
    }

    private static void particles(LocalPlayer p) {
        Level level = p.level();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1f);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        // la bouche est sous les yeux, un peu en avant : les particules ne masquent pas toute la vue
        Vec3 mouth = eye.add(flat.scale(0.55)).add(0, -0.42, 0);
        int color = Stomach.mealColor();
        String meal = Stomach.meal();
        for (int i = 0; i < 3; i++) {
            double vx = flat.x * 0.16 + (RNG.nextDouble() - 0.5) * 0.05;
            double vy = -0.08 + (RNG.nextDouble() - 0.5) * 0.04;
            double vz = flat.z * 0.16 + (RNG.nextDouble() - 0.5) * 0.05;
            level.addParticle(Compat.dust(jitter(color), 0.8f + RNG.nextFloat() * 0.5f), mouth.x, mouth.y, mouth.z, vx, vy, vz);
        }
        // ce qui tombe vraiment : gouttes et morceaux, soumis à la gravité
        level.addParticle(ParticleTypes.FALLING_HONEY, mouth.x + (RNG.nextDouble() - 0.5) * 0.15, mouth.y,
            mouth.z + (RNG.nextDouble() - 0.5) * 0.15, flat.x * 0.12, -0.05, flat.z * 0.12);
        if (t % 2 == 0) {
            level.addParticle(meal.isEmpty() || meal.contains("kelp") ? ParticleTypes.ITEM_SLIME : ParticleTypes.FALLING_HONEY,
                mouth.x, mouth.y, mouth.z, flat.x * 0.15, -0.05, flat.z * 0.15);
            var crumbs = meal.isEmpty() ? null : Compat.itemParticle(Stomach.mealId());
            if (crumbs != null) level.addParticle(crumbs, mouth.x, mouth.y, mouth.z, flat.x * 0.18, -0.02, flat.z * 0.18);
        }
        if (meal.equals("glow_berries") && t % 3 == 0) {
            level.addParticle(ParticleTypes.GLOW, mouth.x, mouth.y, mouth.z, flat.x * 0.1, -0.02, flat.z * 0.1);
        }
        if (t >= 12 && t % 3 == 0 && landing != null) {
            if (overWater && clean) {
                level.addParticle(ParticleTypes.SPLASH, landing.x + RNG.nextGaussian() * 0.2, landing.y + 0.6, landing.z + RNG.nextGaussian() * 0.2, 0, 0.1, 0);
            } else {
                level.addParticle(Compat.dust(jitter(color), 1.4f), landing.x + RNG.nextGaussian() * 0.2, landing.y + 0.5, landing.z + RNG.nextGaussian() * 0.2, 0, -0.02, 0);
            }
        }
    }

    private static int jitter(int rgb) {
        int r = Mth.clamp(((rgb >> 16) & 255) + RNG.nextInt(31) - 15, 0, 255);
        int g = Mth.clamp(((rgb >> 8) & 255) + RNG.nextInt(31) - 15, 0, 255);
        int b = Mth.clamp((rgb & 255) + RNG.nextInt(31) - 15, 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static void finish(LocalPlayer p) {
        phase = Phase.IDLE;
        t = 0;
        Journal j = Journal.get();
        if (dry) {
            Sickness.dose = Math.min(Sickness.dose, 0.72);
            Sickness.reliefTicks = 10 * 20;
            j.dryHeaves++;
            Messages.say(Messages.tint(Component.translatable("seasick.vomit.dry"), 0xC6E08C, true));
        } else {
            Sickness.dose = Math.min(Sickness.dose, clean ? 0.38 : 0.55);
            Sickness.reliefTicks = (clean ? 25 : 15) * 20;
            j.timesSick++;
            if (clean && SeaState.context == Context.BOAT) j.overTheRail++;
            Stomach.emptied();
            Messages.say(Messages.tint(Component.translatable(clean ? "seasick.vomit.clean" : "seasick.vomit.messy"), clean ? 0x9AE0A0 : 0xD8C070, true));
            Sounds.relief();
        }
        Sickness.markSick();
        j.touch();
    }
}
