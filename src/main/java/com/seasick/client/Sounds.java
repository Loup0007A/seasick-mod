package com.seasick.client;

import com.seasick.config.SeaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Sons du corps et de la coque, composés uniquement de sons vanilla (désignés par leur nom,
 * stable d'une version à l'autre) : rien à télécharger, rien à installer côté serveur.
 */
public final class Sounds {

    private Sounds() {}

    public static void play(String id, float volume, float pitch) {
        play(id, volume, pitch, false);
    }

    /** @param essential joué même si les sons du corps sont désactivés (le moment où l'on est malade) */
    public static void play(String id, float volume, float pitch, boolean essential) {
        if (!essential && !SeaConfig.get(SeaConfig.SOUNDS, true)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        SoundEvent ev = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(id));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, volume));
    }

    public static void gulp() {
        play("entity.generic.drink", 0.25f, 1.35f);
    }

    public static void stomach() {
        play("block.bubble_column.upwards_inside", 0.22f, 0.55f);
        play("entity.slime.squish_small", 0.12f, 0.5f);
    }

    public static void heave() {
        play("entity.player.burp", 0.55f, 0.55f, true);
    }

    public static void retch(boolean dry) {
        if (dry) {
            play("entity.player.burp", 0.7f, 0.45f, true);
            play("entity.player.hurt", 0.35f, 0.6f, true);
        } else {
            play("entity.llama.spit", 0.9f, 0.55f, true);
            play("entity.slime.squish_small", 0.5f, 0.6f, true);
        }
    }

    public static void splash(boolean water) {
        if (water) play("entity.generic.splash", 0.35f, 1.4f, true);
        else play("block.honey_block.slide", 0.6f, 0.7f, true);
    }

    public static void creak() {
        play("block.wooden_trapdoor.open", 0.10f, 0.45f + (float) Math.random() * 0.1f);
    }

    public static void slam() {
        play("entity.generic.splash", 0.18f, 0.6f);
    }

    public static void relief() {
        play("entity.player.breath", 0.4f, 0.9f, true);
    }
}
