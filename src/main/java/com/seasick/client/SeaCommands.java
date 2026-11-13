package com.seasick.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.seasick.Seasick;
import com.seasick.platform.Compat;
import com.seasick.sea.SeaState;
import com.seasick.sick.Factor;
import com.seasick.sick.Journal;
import com.seasick.sick.LandSway;
import com.seasick.sick.Sickness;
import com.seasick.sick.Vomit;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Commandes client : /seasick status | logbook | sea <état> | set <0-100> | sealegs <0-100> | vomit | landsick | reset */
@Mod.EventBusSubscriber(modid = Seasick.MOD_ID, value = Dist.CLIENT)
public final class SeaCommands {

    private SeaCommands() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("seasick")
            .executes(ctx -> status())
            .then(Commands.literal("status").executes(ctx -> status()))
            .then(Commands.literal("logbook").executes(ctx -> {
                SeaClient.defer(() -> SeaClient.openLogbook());
                return 1;
            }))
            .then(Commands.literal("sea")
                .then(Commands.argument("state", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(SeaState.Preset.values()).map(p -> p.name().toLowerCase(Locale.ROOT)), b))
                    .executes(ctx -> {
                        String s = StringArgumentType.getString(ctx, "state").toUpperCase(Locale.ROOT);
                        try {
                            SeaState.preset = SeaState.Preset.valueOf(s);
                        } catch (IllegalArgumentException e) {
                            Compat.chat(Component.translatable("seasick.cmd.unknown", s.toLowerCase(Locale.ROOT)));
                            return 0;
                        }
                        Compat.chat(Component.translatable("seasick.cmd.sea", s.toLowerCase(Locale.ROOT)));
                        return 1;
                    })))
            .then(Commands.literal("set")
                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                    Sickness.dose = IntegerArgumentType.getInteger(ctx, "percent") / 100.0;
                    Sickness.shown = Sickness.dose;
                    return status();
                })))
            .then(Commands.literal("sealegs")
                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                    Journal j = Journal.get();
                    j.seaLegs = IntegerArgumentType.getInteger(ctx, "percent") / 100.0;
                    j.touch();
                    Compat.chat(Component.translatable("seasick.cmd.sealegs", (int) Math.round(j.seaLegs * 100)));
                    return 1;
                })))
            .then(Commands.literal("vomit").executes(ctx -> {
                SeaClient.defer(() -> Vomit.start(false));
                return 1;
            }))
            .then(Commands.literal("landsick").executes(ctx -> {
                LandSway.start(0.8);
                return 1;
            }))
            .then(Commands.literal("reset").executes(ctx -> {
                Journal.wipe();
                Sickness.dose = Sickness.shown = 0;
                SeaState.preset = SeaState.Preset.AUTO;
                LandSway.reset();
                Compat.chat(Component.translatable("seasick.cmd.reset"));
                return 1;
            })));
    }

    private static int status() {
        int d = SeaState.douglas(SeaState.hs);
        Compat.chat(Component.translatable("seasick.cmd.status",
            Component.translatable("seasick.stage." + Sickness.stage), (int) Math.round(Sickness.dose * 100),
            Component.translatable("seasick.douglas." + d), String.format("%.1f", SeaState.hs),
            SeaState.beaufort(SeaState.windSpeed), (int) Math.round(Journal.get().seaLegs * 100)));
        String factors = Sickness.FACTORS.stream()
            .filter(a -> Math.abs(a.mult() - 1) > 0.005)
            .map(a -> a.factor().name().toLowerCase(Locale.ROOT) + " x" + String.format("%.2f", a.mult()))
            .collect(Collectors.joining(", "));
        if (!factors.isEmpty()) Compat.chat(Component.literal(factors));
        return 1;
    }

    /** Pour les tests : nombre de facteurs connus. */
    public static int factorCount() {
        return Factor.values().length;
    }
}
