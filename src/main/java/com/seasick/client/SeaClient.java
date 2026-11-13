package com.seasick.client;

import com.seasick.Seasick;
import com.seasick.config.SeaConfig;
import com.seasick.platform.Compat;
import com.seasick.platform.Platform;
import com.seasick.sea.SeaState;
import com.seasick.sea.Swell;
import com.seasick.sick.Journal;
import com.seasick.sick.LandSway;
import com.seasick.sick.Sickness;
import com.seasick.sick.Stomach;
import com.seasick.sick.Vomit;
//#if !mc1201
import net.minecraft.client.DeltaTracker;
//#endif
//#if !mc1201 && !mc26
import net.minecraft.client.gui.LayeredDraw;
//#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le chef d'orchestre côté client : fait avancer la mer, le corps et l'interface à chaque tick,
 * et relaie les événements du loader (caméra, champ de vision, déplacements, repas).
 */
//#if mc26
//$$ // 26.x : enregistré par ClientBoot.onClientSetup (Minecraft doit exister avant l'initialisation de la classe)
//#else
@Mod.EventBusSubscriber(modid = Seasick.MOD_ID, value = Dist.CLIENT)
//#endif
public final class SeaClient {

    private SeaClient() {}

    private static boolean layersInstalled, layersFailed;
    private static Runnable deferred;
    private static int saveTimer;
    private static String worldKey;
    private static int failures;
    private static final Set<String> REPORTED = new HashSet<>();

    public static int failures() {
        return failures;
    }

    public static void defer(Runnable r) {
        deferred = r;
    }

    public static void openLogbook() {
        Minecraft.getInstance().setScreen(new LogbookScreen());
    }

    private static void fail(String where, Throwable t) {
        failures++;
        if (REPORTED.add(where)) Seasick.LOGGER.error("Seasick : erreur dans {}", where, t);
    }

    // ───────────────────────── ticks ─────────────────────────

    //#if mc1201
    //$$ @SubscribeEvent
    //$$ public static void onClientTick(TickEvent.ClientTickEvent event) {
    //$$     if (event.phase == TickEvent.Phase.END) clientTick();
    //$$ }
    //$$
    //$$ @SubscribeEvent
    //$$ public static void onRenderTick(TickEvent.RenderTickEvent event) {
    //$$     if (event.phase == TickEvent.Phase.START) frame();
    //$$ }
    //#else
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        clientTick();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent.Pre event) {
        frame();
    }
    //#endif

    private static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        installLayers();
        if (deferred != null) {
            Runnable r = deferred;
            deferred = null;
            try {
                r.run();
            } catch (Throwable t) {
                fail("commande", t);
            }
        }
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (worldKey == null) {
            worldKey = worldKey(mc);
            Journal.load(worldKey);
            SeaState.worldSeed = worldKey;
        }
        Messages.tick();
        if (mc.isPaused()) return;
        if (!SeaConfig.enabled()) {
            View.release(p);
            resetState();
            return;
        }
        Sickness.eyesClosed = (ClientBoot.KEY_EYES.isDown() && mc.screen == null) || Sickness.testEyes;
        while (ClientBoot.KEY_LOGBOOK.consumeClick()) {
            if (mc.screen == null) openLogbook();
        }
        try {
            SeaState.tick(p);
            Swell.tick();
            LandSway.tick();
            Sickness.tick(p);
            Vomit.tick(p);
            Hud.tick();
        } catch (Throwable t) {
            fail("tick", t);
        }
        if (++saveTimer >= 1200) {
            saveTimer = 0;
            Journal.save();
        }
    }

    private static void frame() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused() || !SeaConfig.enabled()) return;
        try {
            View.frame(p, Compat.partialTick(), Compat.frameTicks());
        } catch (Throwable t) {
            fail("frame", t);
        }
    }

    private static String worldKey(Minecraft mc) {
        try {
            if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
                java.nio.file.Path root = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                return "sp_" + root.getFileName();
            }
            if (mc.getCurrentServer() != null) return "mp_" + mc.getCurrentServer().ip;
        } catch (Throwable t) {
            Seasick.LOGGER.debug("Seasick : nom du monde inconnu", t);
        }
        return "default";
    }

    private static void resetState() {
        SeaState.reset();
        Swell.reset();
        Sickness.reset();
        Vomit.reset();
        LandSway.reset();
        Stomach.reset();
        Hud.reset();
    }

    // ───────────────────────── caméra / déplacements / repas ─────────────────────────

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (Minecraft.getInstance().player == null || !SeaConfig.enabled()) return;
        event.setRoll(event.getRoll() + (float) View.roll);
        if (View.cameraPitch != 0) event.setPitch(event.getPitch() + (float) View.cameraPitch);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        //#if !mc26
        if (!event.usedConfiguredFov()) return;
        //#endif
        if (Minecraft.getInstance().player == null || !SeaConfig.enabled()) return;
        event.setFOV((float) (event.getFOV() + View.fov));
        Hud.lastFov = event.getFOV();
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || !SeaConfig.enabled() || !SeaConfig.get(SeaConfig.AFFECT_CONTROLS, true)) return;
        //#if mc26
        //$$ com.seasick.platform.MoveInput in = com.seasick.platform.MoveInput.from(event.getInput());
        //#else
        Input in = event.getInput();
        //#endif
        if (Vomit.acting()) {
            in.forwardImpulse = 0;
            in.leftImpulse = 0;
            in.up = in.down = in.left = in.right = false;
            in.jumping = false;
        } else {
            if (LandSway.active() && (in.forwardImpulse != 0 || in.leftImpulse != 0)) {
                in.leftImpulse += (float) LandSway.drift();
            }
            if (Sickness.fumbleTicks > 0) {
                in.left = Sickness.fumbleDir > 0;
                in.right = Sickness.fumbleDir < 0;
            }
        }
        //#if mc26
        //$$ in.applyTo(event.getInput());
        //#endif
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || !SeaConfig.enabled()) return;
        try {
            Sickness.onEat(Compat.itemPath(event.getItem()));
        } catch (Throwable t) {
            fail("repas", t);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Journal.unload();
        worldKey = null;
        SeaState.preset = SeaState.Preset.AUTO;
        View.reset();
        resetState();
    }

    // ───────────────────────── couches du HUD ─────────────────────────

    public static void underLayer(GuiGraphics g, float pt) {
        try {
            Hud.under(g, pt);
        } catch (Throwable t) {
            fail("hud", t);
        }
    }

    public static void overLayer(GuiGraphics g, float pt) {
        try {
            Hud.over(g, pt);
        } catch (Throwable t) {
            fail("jauge", t);
        }
    }

    /** Forge 1.21.1 n'a pas d'API de couches : on en insère deux dans la liste du HUD vanilla. */
    @SuppressWarnings("unchecked")
    private static void installLayers() {
        Minecraft mc = Minecraft.getInstance();
        if (layersInstalled || layersFailed || mc.gui == null) return;
        if (Platform.GUI_LAYER_EVENT) {
            layersInstalled = true;
            return;
        }
        //#if mc1201 || mc26
        //$$ layersFailed = true;
        //#else
        try {
            Field top = Gui.class.getDeclaredField("layers");
            top.setAccessible(true);
            LayeredDraw draw = (LayeredDraw) top.get(mc.gui);
            Field inner = LayeredDraw.class.getDeclaredField("layers");
            inner.setAccessible(true);
            List<LayeredDraw.Layer> list = (List<LayeredDraw.Layer>) inner.get(draw);
            list.add(0, (GuiGraphics g, DeltaTracker dt) -> underLayer(g, dt.getGameTimeDeltaPartialTick(false)));
            list.add((GuiGraphics g, DeltaTracker dt) -> overLayer(g, dt.getGameTimeDeltaPartialTick(false)));
            layersInstalled = true;
        } catch (Throwable t) {
            layersFailed = true;
            Seasick.LOGGER.warn("Seasick : couches du HUD indisponibles, repli sur le rendu de secours", t);
        }
        //#endif
    }

    /** Repli si les couches n'ont pas pu être installées. */
    @SubscribeEvent
    public static void onChatOverlay(CustomizeGuiOverlayEvent.Chat event) {
        if (!layersFailed) return;
        float pt = Compat.partialTick();
        underLayer(event.getGuiGraphics(), pt);
        overLayer(event.getGuiGraphics(), pt);
    }
}
