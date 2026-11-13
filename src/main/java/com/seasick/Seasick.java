package com.seasick;

import com.seasick.config.SeaConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
//#if forge
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//#endif
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seasick — le mal de mer, pour de vrai.
 *
 * Mod purement client : la houle, le vent et votre estomac sont calculés sur votre machine.
 * Sur un serveur dédié il ne fait rien (hormis la mise en scène du test automatique en CI).
 */
@Mod(Seasick.MOD_ID)
public class Seasick {

    public static final String MOD_ID = "seasick";
    public static final Logger LOGGER = LoggerFactory.getLogger("Seasick");

    //#if neoforge
    //$$ public Seasick(net.neoforged.bus.api.IEventBus modBus, net.neoforged.fml.ModContainer container) {
    //$$     if (FMLEnvironment.dist == Dist.CLIENT) {
    //$$         container.registerConfig(ModConfig.Type.CLIENT, SeaConfig.SPEC);
    //$$         com.seasick.client.ClientBoot.init(modBus);
    //$$     }
    //$$ }
    //#else
    public Seasick() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SeaConfig.SPEC);
            com.seasick.client.ClientBoot.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
    //#endif

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
