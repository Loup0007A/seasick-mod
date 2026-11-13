package com.seasick.client;

import com.seasick.Seasick;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Enregistrements côté client sur le bus du mod : touches et couches du HUD. */
public final class ClientBoot {

    private ClientBoot() {}

    //#if mc26
    //$$ public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Seasick.id("main"));
    //$$ public static final KeyMapping KEY_EYES = new KeyMapping("key.seasick.close_eyes",
    //$$     InputConstants.Type.KEYSYM, InputConstants.KEY_Z, CATEGORY);
    //$$ public static final KeyMapping KEY_LOGBOOK = new KeyMapping("key.seasick.logbook",
    //$$     InputConstants.Type.KEYSYM, InputConstants.KEY_O, CATEGORY);
    //#else
    public static final KeyMapping KEY_EYES = new KeyMapping("key.seasick.close_eyes",
        InputConstants.Type.KEYSYM, InputConstants.KEY_Z, "key.categories.seasick");
    public static final KeyMapping KEY_LOGBOOK = new KeyMapping("key.seasick.logbook",
        InputConstants.Type.KEYSYM, InputConstants.KEY_O, "key.categories.seasick");
    //#endif

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientBoot::onKeys);
        //#if neoforge
        //$$ modBus.addListener(ClientBoot::onLayers);
        //#endif
        //#if mc1201
        //$$ modBus.addListener(ClientBoot::onOverlays);
        //#endif
        //#if mc26
        //$$ modBus.addListener(ClientBoot::onClientSetup);
        //#endif
    }

    //#if mc1201
    //$$ /** Forge 1.20.1 : couches du HUD par RegisterGuiOverlaysEvent. */
    //$$ private static void onOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
    //$$     event.registerBelowAll("sea_under", (gui, g, pt, w, h) -> SeaClient.underLayer(g, pt));
    //$$     event.registerAboveAll("sea_over", (gui, g, pt, w, h) -> SeaClient.overLayer(g, pt));
    //$$ }
    //#endif

    //#if neoforge
    //$$ /** NeoForge a une API de couches : effets sous tout le HUD, jauge au-dessus. */
    //$$ private static void onLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
    //$$     event.registerBelowAll(Seasick.id("sea_under"), (g, dt) -> SeaClient.underLayer(g, dt.getGameTimeDeltaPartialTick(false)));
    //$$     event.registerAboveAll(Seasick.id("sea_over"), (g, dt) -> SeaClient.overLayer(g, dt.getGameTimeDeltaPartialTick(false)));
    //$$ }
    //#endif

    //#if mc26
    //$$ /** 26.x : les mods sont construits avant Minecraft ; les gestionnaires qui le gardent en champ statique attendent ici. */
    //$$ private static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
    //$$     event.enqueueWork(() -> {
    //$$         net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(SeaClient.class);
    //$$         net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.seasick.debug.ClientSelfTest.class);
    //$$     });
    //$$ }
    //#endif

    private static void onKeys(RegisterKeyMappingsEvent event) {
        //#if mc26
        //$$ event.registerCategory(CATEGORY);
        //#endif
        event.register(KEY_EYES);
        event.register(KEY_LOGBOOK);
    }
}
