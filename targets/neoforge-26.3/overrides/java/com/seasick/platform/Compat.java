package com.seasick.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Variante Minecraft 26.x (voir src/main/java/com/seasick/platform/Compat.java). */
public final class Compat {

    private Compat() {}

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static float partialTick() {
        return mc().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public static float frameTicks() {
        return mc().getDeltaTracker().getRealtimeDeltaTicks();
    }

    public static String typePath(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    public static String itemPath(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static String keyPath(ResourceKey<?> key) {
        return key.identifier().getPath();
    }

    public static String biomePath(Holder<Biome> biome) {
        return biome.unwrapKey().map(Compat::keyPath).orElse("");
    }

    public static String dimension(Level level) {
        return level.dimension().identifier().toString();
    }

    public static boolean hasNausea(Player p) {
        return p.hasEffect(MobEffects.NAUSEA);
    }

    public static ParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(rgb, scale);
    }

    public static ParticleOptions itemParticle(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == net.minecraft.world.item.Items.AIR ? null : new ItemParticleOption(ParticleTypes.ITEM, item);
    }

    public static void toast(Component title, Component message) {
        SystemToast.addOrUpdate(mc().gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
    }

    public static void actionBar(Component message) {
        if (mc().player != null) mc().player.sendOverlayMessage(message);
    }

    public static void chat(Component message) {
        if (mc().player != null) mc().player.sendSystemMessage(message);
    }

    /** Bouton de souris (règle de réécriture de mouseClicked). */
    public static int legacyButton(int button) {
        return button;
    }
}
