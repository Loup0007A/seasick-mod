package com.seasick.client;

import com.seasick.config.SeaConfig;
import com.seasick.platform.Compat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/** Messages au-dessus de la barre d'action, sans en noyer le joueur. */
public final class Messages {

    private Messages() {}

    private static int cooldown;
    private static int tipIndex;

    public static void tick() {
        if (cooldown > 0) cooldown--;
    }

    /** Message important : affiché même pendant la pause entre deux messages. */
    public static void say(Component c) {
        if (!SeaConfig.get(SeaConfig.MESSAGES, true)) return;
        Compat.actionBar(c);
        cooldown = 100;
    }

    /** Message ordinaire : ignoré si un autre vient d'être affiché. */
    public static void maybe(Component c) {
        if (cooldown > 0) return;
        say(c);
    }

    /** Texte coloré (0xRRGGBB), éventuellement en italique. */
    public static MutableComponent tint(Component c, int rgb, boolean italic) {
        return c.copy().withStyle(st -> st.withColor(TextColor.fromRgb(rgb)).withItalic(italic));
    }

    /** Un symptôme (vert pâle, italique) suivi d'un conseil (gris clair). */
    public static Component symptom(Component s, Component tip) {
        MutableComponent m = tint(s, 0xC6E08C, true);
        if (tip != null) m.append(Component.literal("  ")).append(tint(tip, 0xD8D8D0, false));
        return m;
    }

    /** Un conseil différent à chaque fois. */
    public static Component tip() {
        String[] tips = {"horizon", "helm", "eyes", "plain_food", "honey", "ashore", "no_reading"};
        tipIndex = (tipIndex + 1) % tips.length;
        if (tips[tipIndex].equals("eyes")) return Component.translatable("seasick.tip.eyes", ClientBoot.KEY_EYES.getTranslatedKeyMessage());
        return Component.translatable("seasick.tip." + tips[tipIndex]);
    }
}
