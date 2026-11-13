package com.seasick.platform;

/**
 * Ce qui diffère d'un loader à l'autre. Cette version-ci est celle de Forge 1.21–1.21.1 ;
 * chaque autre cible a la sienne dans targets/&lt;cible&gt;/overrides (voir tools/port.py).
 */
public final class Platform {

    private Platform() {}

    /** Nom de la cible, pour les journaux. */
    public static final String TARGET = "forge-1.21.1";

    /** Les couches du HUD sont enregistrées par un événement du loader (sinon : réflexion sur Gui.layers). */
    public static final boolean GUI_LAYER_EVENT = false;
}
