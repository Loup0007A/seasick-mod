package com.seasick.platform;

/** Variante Forge 1.20.1 (voir src/main/java/com/seasick/platform/Platform.java). */
public final class Platform {

    private Platform() {}

    public static final String TARGET = "forge-1.20.1";

    /** Forge 1.20.1 enregistre les couches du HUD par RegisterGuiOverlaysEvent (voir ClientBoot). */
    public static final boolean GUI_LAYER_EVENT = true;
}
