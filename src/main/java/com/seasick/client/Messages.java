package com.seasick.client;

import com.seasick.config.SeaConfig;
import com.seasick.platform.Compat;
import net.minecraft.network.chat.Component;

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

    /** Un conseil différent à chaque fois. */
    public static Component tip() {
        String[] tips = {"horizon", "helm", "eyes", "plain_food", "honey", "ashore", "no_reading"};
        tipIndex = (tipIndex + 1) % tips.length;
        if (tips[tipIndex].equals("eyes")) return Component.translatable("seasick.tip.eyes", ClientBoot.KEY_EYES.getTranslatedKeyMessage());
        return Component.translatable("seasick.tip." + tips[tipIndex]);
    }
}
