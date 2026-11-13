package com.seasick.sea;

/**
 * Bruit lisse et déterministe : le vent d'un monde donné souffle de la même façon pour
 * tous ceux qui ont le mod (même graine, même heure du jeu), sans rien demander au serveur.
 */
public final class Wind {

    private Wind() {}

    private static double hash(long seed, long i) {
        long x = seed ^ (i * 0x9E3779B97F4A7C15L);
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (x >>> 11) * 0x1.0p-53;
    }

    /** Bruit de valeur 1D dans [0, 1], interpolation de Hermite. */
    public static double noise(long seed, double t) {
        long i = (long) Math.floor(t);
        double f = t - i;
        double u = f * f * (3 - 2 * f);
        return hash(seed, i) * (1 - u) + hash(seed, i + 1) * u;
    }

    /** Deux octaves : variations lentes et rafales plus courtes. */
    public static double fractal(long seed, double t) {
        return 0.7 * noise(seed, t) + 0.3 * noise(seed * 31 + 7, t * 3.1);
    }

    public static long seed(String s) {
        long h = 1125899906842597L;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        return h;
    }
}
