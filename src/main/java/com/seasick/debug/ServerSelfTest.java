package com.seasick.debug;

import com.seasick.Seasick;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//#if !mc26
import net.minecraft.world.level.GameRules;
//#endif
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mise en scène pour l'intégration continue uniquement (-Dseasick.selftest=true) :
 * un grand bassin d'eau, une petite île avec un phare, et le joueur à la barre d'un bateau.
 * Sans cette propriété, cette classe ne fait rien.
 */
@Mod.EventBusSubscriber(modid = Seasick.MOD_ID)
public final class ServerSelfTest {

    private ServerSelfTest() {}

    private static final boolean ENABLED = Boolean.getBoolean("seasick.selftest");
    private static final int R = 34;

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!ENABLED) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        //#if mc26
        //$$ var rules = level.getGameRules();
        //$$ rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, server);
        //$$ rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER, false, server);
        //$$ rules.set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS, false, server);
        //$$ var src = server.createCommandSourceStack().withSuppressedOutput();
        //$$ server.getCommands().performPrefixedCommand(src, "time set 4500");
        //$$ server.getCommands().performPrefixedCommand(src, "weather clear 999999");
        //#else
        level.setDayTime(4500);
        level.setWeatherParameters(999999, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        //#endif
        Seasick.LOGGER.info("[SELFTEST] READY");
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer sp)) return;
        //#if mc26
        //$$ ServerLevel level = sp.level();
        //$$ MinecraftServer server = level.getServer();
        //$$ server.getPlayerList().op(new net.minecraft.server.players.NameAndId(sp.getGameProfile()));
        //$$ BlockPos spawn = level.getRespawnData().pos();
        //#else
        MinecraftServer server = sp.getServer();
        server.getPlayerList().op(sp.getGameProfile());
        ServerLevel level = sp.serverLevel();
        BlockPos spawn = level.getSharedSpawnPos();
        //#endif
        sp.setGameMode(GameType.CREATIVE);
        int cx = spawn.getX(), cz = spawn.getZ();
        int sea = Math.max(level.getHeight(Heightmap.Types.OCEAN_FLOOR, cx, cz), 64);

        // le bassin : 7 blocs d'eau sur du sable, ciel dégagé au-dessus
        BlockState water = Blocks.WATER.defaultBlockState(), sand = Blocks.SAND.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = cx - R; x <= cx + R; x++) {
            for (int z = cz - R; z <= cz + R; z++) {
                for (int y = sea - 8; y <= sea + 24; y++) {
                    BlockState s = y == sea - 8 ? sand : y < sea ? water : air;
                    level.setBlock(m.set(x, y, z), s, 2 | 16);
                }
            }
        }
        // une petite île et son phare, pour donner un repère aux captures
        int ix = cx + 15, iz = cz + 18;
        for (int x = ix - 3; x <= ix + 3; x++) {
            for (int z = iz - 3; z <= iz + 3; z++) {
                if ((x - ix) * (x - ix) + (z - iz) * (z - iz) > 10) continue;
                for (int y = sea - 7; y <= sea; y++) level.setBlock(m.set(x, y, z), y == sea ? Blocks.GRASS_BLOCK.defaultBlockState() : sand, 2 | 16);
            }
        }
        for (int y = sea + 1; y <= sea + 9; y++) {
            BlockState s = y >= sea + 8 ? Blocks.GLOWSTONE.defaultBlockState()
                : ((y - sea) % 3 == 0 ? Blocks.BRICKS : Blocks.QUARTZ_BLOCK).defaultBlockState();
            level.setBlock(m.set(ix, y, iz), s, 2 | 16);
        }

        ItemStack[] hotbar = {
            new ItemStack(Items.BREAD, 16), new ItemStack(Items.HONEY_BOTTLE, 4), new ItemStack(Items.DRIED_KELP, 16),
            new ItemStack(Items.SPYGLASS), new ItemStack(Items.COOKED_BEEF, 8), new ItemStack(Items.COMPASS)
        };
        for (int i = 0; i < hotbar.length; i++) sp.getInventory().setItem(i, hotbar[i]);
        sp.getInventory().selected = 0;

        //#if mc26
        //$$ Entity boat = EntityType.OAK_BOAT.create(level);
        //#else
        Entity boat = EntityType.BOAT.create(level);
        //#endif
        if (boat != null) {
            boat.moveTo(cx + 0.5, sea + 0.05, cz + 0.5, 0f, 0f);
            level.addFreshEntity(boat);
            sp.teleportTo(cx + 0.5, sea + 0.6, cz + 0.5);
            boolean ok = sp.startRiding(boat);
            Seasick.LOGGER.info("[SELFTEST] bateau prêt (embarqué : {}) mer à y={}", ok, sea);
        }
    }
}
