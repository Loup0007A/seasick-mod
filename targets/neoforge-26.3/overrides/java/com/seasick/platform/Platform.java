package com.seasick.platform;

/** Variante NeoForge 26.3 (voir src/main/java/com/seasick/platform/Platform.java). */
public final class Platform {

    private Platform() {}

    public static final String TARGET = "neoforge-26.3";

    /** NeoForge enregistre les couches du HUD par RegisterGuiLayersEvent (voir ClientBoot). */
    public static final boolean GUI_LAYER_EVENT = true;
}
