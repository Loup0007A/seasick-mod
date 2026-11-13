package com.seasick.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuration client : config/seasick-client.toml */
public final class SeaConfig {

    private SeaConfig() {}

    public enum HudPosition { HOTBAR_LEFT, TOP_LEFT, TOP_RIGHT, HIDDEN }

    public enum Horizon { OFF, ASSIST, ALWAYS }

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue SUSCEPTIBILITY;
    public static final ForgeConfigSpec.BooleanValue CREATIVE_IMMUNE;
    public static final ForgeConfigSpec.BooleanValue SEA_LEGS;
    public static final ForgeConfigSpec.DoubleValue SEA_LEGS_RATE;
    public static final ForgeConfigSpec.BooleanValue LAND_SICKNESS;
    public static final ForgeConfigSpec.BooleanValue CAMELS;

    public static final ForgeConfigSpec.DoubleValue MOTION_SCALE;
    public static final ForgeConfigSpec.BooleanValue REDUCE_MOTION;
    public static final ForgeConfigSpec.BooleanValue SWAY_AIM;
    public static final ForgeConfigSpec.DoubleValue SCREEN_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue GROSS_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue AFFECT_CONTROLS;
    public static final ForgeConfigSpec.EnumValue<Horizon> HORIZON;

    public static final ForgeConfigSpec.EnumValue<HudPosition> HUD;
    public static final ForgeConfigSpec.BooleanValue MESSAGES;
    public static final ForgeConfigSpec.BooleanValue SOUNDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Seasick — client configuration / configuration client").push("sickness");
        ENABLED = b.comment("Master switch. false = no sea motion and no sickness at all.")
            .define("enabled", true);
        SUSCEPTIBILITY = b.comment("How prone you are to motion sickness (0.25 = iron stomach, 1 = average, 3 = very sensitive).")
            .defineInRange("susceptibility", 1.0, 0.25, 3.0);
        CREATIVE_IMMUNE = b.comment("Creative-mode players feel the sea but never get sick.")
            .define("creativeImmune", false);
        SEA_LEGS = b.comment("Habituation: time spent at sea slowly makes you resistant (and you lose it after long stays ashore).")
            .define("seaLegs", true);
        SEA_LEGS_RATE = b.comment("Speed of habituation (1 = about 40 minutes of moderate sea for full sea legs).")
            .defineInRange("seaLegsRate", 1.0, 0.1, 10.0);
        LAND_SICKNESS = b.comment("Mal de débarquement: after a long voyage the ground keeps swaying for a while.")
            .define("landSickness", true);
        CAMELS = b.comment("Camels are the ships of the desert: riding one rocks you too.")
            .define("camels", true);
        b.pop();

        b.comment("Comfort and accessibility").push("comfort");
        MOTION_SCALE = b.comment("Strength of the camera motion (roll, pitch, heave). 0 = still camera.")
            .defineInRange("motionScale", 1.0, 0.0, 1.5);
        REDUCE_MOTION = b.comment("Never move the camera. The sea is shown with the artificial horizon and the HUD gauge instead.")
            .define("reduceMotion", false);
        SWAY_AIM = b.comment("The boat's pitching also moves your aim (true) or only the picture (false).")
            .define("swayAim", true);
        SCREEN_EFFECTS = b.comment("Strength of the pale tint, cold-sweat vignette and heavy eyelids. 0 = none.")
            .defineInRange("screenEffects", 1.0, 0.0, 1.0);
        GROSS_EFFECTS = b.comment("Being sick shows particles and plays sounds. false = it only fades the screen.")
            .define("grossEffects", true);
        AFFECT_CONTROLS = b.comment("Sickness may shake your aim, make you fumble the oars and make you lean over the side.")
            .define("affectControls", true);
        HORIZON = b.comment("Artificial horizon line. ASSIST = only when you feel unwell and cannot see the real horizon.")
            .defineEnum("artificialHorizon", Horizon.ASSIST);
        b.pop();

        b.comment("Interface").push("interface");
        HUD = b.comment("Where the queasiness gauge is drawn.")
            .defineEnum("hud", HudPosition.HOTBAR_LEFT);
        MESSAGES = b.comment("Short messages above the hotbar (symptoms and tips).")
            .define("messages", true);
        SOUNDS = b.comment("Body and boat sounds (gulps, stomach, creaking hull).")
            .define("sounds", true);
        b.pop();

        SPEC = b.build();
    }

    /** Lecture tolérante : la config n'est pas encore chargée au tout début. */
    public static <T> T get(ForgeConfigSpec.ConfigValue<T> v, T fallback) {
        try {
            return v.get();
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static boolean enabled() {
        return get(ENABLED, true);
    }

    public static double motion() {
        if (get(REDUCE_MOTION, false)) return 0.0;
        return get(MOTION_SCALE, 1.0);
    }

    public static double screen() {
        return get(SCREEN_EFFECTS, 1.0);
    }
}
