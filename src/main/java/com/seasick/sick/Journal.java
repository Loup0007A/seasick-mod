package com.seasick.sick;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seasick.Seasick;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Le carnet de bord : pied marin et souvenirs de traversées, un fichier par monde ou serveur
 * (dossier seasick/ du jeu). Rien n'est stocké sur le serveur.
 */
public final class Journal {

    /** Accoutumance, de 0 (terrien) à 1 (loup de mer). */
    public double seaLegs;
    public long ticksAtSea;
    public double distance;
    public int timesSick;
    public int overTheRail;
    public int dryHeaves;
    public int voyages;
    public long longestCalmVoyage;
    public long lastVoyage;
    public int milestone;

    private transient String key = "default";
    private transient boolean dirty;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Journal current = new Journal();

    public static Journal get() {
        return current;
    }

    /** Titre de marin selon le pied marin : 0 terrien, 1 mousse, 2 matelot, 3 marin aguerri, 4 loup de mer. */
    public static int rank(double legs) {
        if (legs >= 0.999) return 4;
        if (legs >= 0.75) return 3;
        if (legs >= 0.5) return 2;
        if (legs >= 0.25) return 1;
        return 0;
    }

    private static Path dir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("seasick");
    }

    private static String sanitize(String s) {
        String r = s.toLowerCase().replaceAll("[^a-z0-9._-]+", "_");
        return r.isEmpty() ? "default" : (r.length() > 80 ? r.substring(0, 80) : r);
    }

    public static void load(String worldKey) {
        String k = sanitize(worldKey);
        if (k.equals(current.key)) return;
        save();
        Journal j = null;
        try {
            Path p = dir().resolve(k + ".json");
            if (Files.exists(p)) j = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), Journal.class);
        } catch (Exception e) {
            Seasick.LOGGER.warn("Seasick : carnet de bord illisible pour {}", k, e);
        }
        if (j == null) j = new Journal();
        j.key = k;
        j.seaLegs = Math.max(0, Math.min(1, j.seaLegs));
        current = j;
    }

    public void touch() {
        dirty = true;
    }

    public static void save() {
        Journal j = current;
        if (!j.dirty || j.key == null) return;
        try {
            Files.createDirectories(dir());
            Files.writeString(dir().resolve(j.key + ".json"), GSON.toJson(j), StandardCharsets.UTF_8);
            j.dirty = false;
        } catch (Exception e) {
            Seasick.LOGGER.warn("Seasick : impossible d'écrire le carnet de bord", e);
        }
    }

    public static void unload() {
        save();
        current = new Journal();
    }

    public static void wipe() {
        String k = current.key;
        current = new Journal();
        current.key = k;
        current.dirty = true;
        save();
    }
}
