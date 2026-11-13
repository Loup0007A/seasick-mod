package com.seasick.sick;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Ce que vous avez mangé, et combien de temps ça vous pèse (ou vous soulage).
 *
 * - Du sec et du fade (pain, biscuit, algues séchées, pomme de terre) cale l'estomac : c'est le
 *   conseil de tous les marins.
 * - Le miel tient le rôle du gingembre : il apaise franchement.
 * - Un repas lourd (viande, ragoût, tarte) ou cru, le lait, et a fortiori la chair putréfiée,
 *   l'œil d'araignée ou le poisson-globe, font l'inverse.
 * - Un estomac vide est presque aussi mauvais qu'un estomac trop plein.
 */
public final class Stomach {

    private Stomach() {}

    private static final Set<String> PLAIN = Set.of("bread", "cookie", "dried_kelp", "baked_potato", "potato", "carrot",
        "apple", "melon_slice", "sweet_berries", "beetroot", "glow_berries", "golden_carrot", "golden_apple", "enchanted_golden_apple");
    private static final Set<String> HEAVY = Set.of("cooked_porkchop", "cooked_beef", "cooked_mutton", "cooked_chicken", "cooked_rabbit",
        "rabbit_stew", "mushroom_stew", "beetroot_soup", "pumpkin_pie", "cake");
    private static final Set<String> RAW = Set.of("beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon", "tropical_fish");
    private static final Set<String> SPOILED = Set.of("rotten_flesh", "spider_eye", "poisonous_potato", "pufferfish", "suspicious_stew");

    /** Couleur de la bile, quand l'estomac est vide. */
    public static final int BILE = 0xB5B33A;

    private static final Map<Factor, Integer> TIMERS = new EnumMap<>(Factor.class);
    /** Couleur (0xRRGGBB) et identifiant du dernier repas, pour ce qui ressortira. */
    private static int mealColor = BILE;
    private static String meal = "";
    private static String mealId = "";

    public static void reset() {
        TIMERS.clear();
        mealColor = BILE;
        meal = "";
        mealId = "";
    }

    public static void tick() {
        TIMERS.replaceAll((f, t) -> t - 1);
        TIMERS.values().removeIf(t -> t <= 0);
    }

    public static Map<Factor, Integer> timers() {
        return TIMERS;
    }

    public static int mealColor() {
        return mealColor;
    }

    public static String meal() {
        return meal;
    }

    /** Identifiant complet du dernier repas (pour ses miettes). */
    public static String mealId() {
        return mealId;
    }

    /** L'estomac est vidé. */
    public static void emptied() {
        mealColor = BILE;
        meal = "";
        mealId = "";
        TIMERS.remove(Factor.HEAVY_MEAL);
        TIMERS.remove(Factor.SPOILED);
        TIMERS.remove(Factor.MILK);
    }

    /** Effet du repas : variation immédiate de la dose (peut être négative), puis un facteur temporaire. */
    public static double eat(String item, String id) {
        double dose = 0;
        if (item.equals("honey_bottle")) {
            TIMERS.put(Factor.HONEY, 180 * 20);
            dose = -0.10;
        } else if (item.equals("milk_bucket")) {
            TIMERS.put(Factor.MILK, 120 * 20);
            dose = 0.03;
        } else if (SPOILED.contains(item)) {
            TIMERS.put(Factor.SPOILED, 60 * 20);
            dose = 0.20;
        } else if (RAW.contains(item)) {
            TIMERS.put(Factor.HEAVY_MEAL, 120 * 20);
            dose = 0.10;
        } else if (HEAVY.contains(item)) {
            TIMERS.put(Factor.HEAVY_MEAL, 120 * 20);
            dose = 0.06;
        } else if (PLAIN.contains(item)) {
            TIMERS.put(Factor.PLAIN_FOOD, 90 * 20);
            dose = -0.05;
        }
        if (!item.equals("milk_bucket") && !item.equals("potion")) {
            meal = item;
            mealId = id;
            mealColor = color(item);
        }
        return dose;
    }

    public static boolean isRich(String item) {
        return HEAVY.contains(item) || RAW.contains(item) || SPOILED.contains(item);
    }

    private static int color(String item) {
        if (item.contains("carrot") || item.equals("pumpkin_pie")) return 0xE08A2E;
        if (item.contains("beetroot") || item.contains("berries") || item.equals("melon_slice")) return 0xB0303A;
        if (item.equals("bread") || item.equals("cookie") || item.contains("potato")) return 0xC9A26B;
        if (item.contains("kelp")) return 0x5E7B2E;
        if (item.equals("honey_bottle")) return 0xE8B030;
        if (item.equals("mushroom_stew") || item.equals("suspicious_stew")) return 0x9B7653;
        if (item.equals("rabbit_stew")) return 0x9C6B3F;
        if (item.contains("apple")) return 0xD9C27A;
        if (item.equals("chorus_fruit")) return 0x8E6CA8;
        if (item.contains("cod") || item.contains("salmon") || item.contains("fish")) return 0xC8B79A;
        if (item.equals("rotten_flesh") || item.equals("spider_eye")) return 0x6B4A3A;
        if (HEAVY.contains(item) || RAW.contains(item)) return 0x8A5A3C;
        return 0xA8A050;
    }
}
