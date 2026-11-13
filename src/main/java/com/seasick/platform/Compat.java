package com.seasick.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.joml.Vector3f;

/**
 * Ce qui change d'une version de Minecraft à l'autre (indépendamment du loader).
 * Cette version-ci est celle de Minecraft 1.21–1.21.1 (Forge et NeoForge) ;
 * Forge 1.20.1 et NeoForge 26.3 ont la leur dans targets/&lt;cible&gt;/overrides.
 */
public final class Compat {

    private Compat() {}

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static float partialTick() {
        //#if mc1201
        //$$ return mc().getFrameTime();
        //#else
        return mc().getTimer().getGameTimeDeltaPartialTick(false);
        //#endif
    }

    /** Durée de la dernière image, en ticks. */
    public static float frameTicks() {
        //#if mc1201
        //$$ return mc().getDeltaFrameTime();
        //#else
        return mc().getTimer().getRealtimeDeltaTicks();
        //#endif
    }

    /** Identifiant (sans espace de noms) du type d'une entité : « boat », « oak_boat », « camel »... */
    public static String typePath(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    public static String itemPath(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /** Identifiant complet (« minecraft:bread », « farmersdelight:tomato_sauce »...). */
    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static String keyPath(ResourceKey<?> key) {
        return key.location().getPath();
    }

    public static String biomePath(Holder<Biome> biome) {
        return biome.unwrapKey().map(Compat::keyPath).orElse("");
    }

    public static String dimension(Level level) {
        return level.dimension().location().toString();
    }

    public static boolean hasNausea(Player p) {
        return p.hasEffect(MobEffects.CONFUSION);
    }

    /** Poussière colorée (0xRRGGBB). */
    public static ParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), scale);
    }

    /** Miettes de l'objet (comme quand on mange), qui retombent ; null si l'objet n'existe pas. */
    public static ParticleOptions itemParticle(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        return stack.isEmpty() ? null : new ItemParticleOption(ParticleTypes.ITEM, stack);
    }

    public static void toast(Component title, Component message) {
        SystemToast.addOrUpdate(mc().getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
    }

    public static void actionBar(Component message) {
        mc().gui.setOverlayMessage(message, false);
    }

    public static void chat(Component message) {
        if (mc().player != null) mc().player.displayClientMessage(message, false);
    }
}
