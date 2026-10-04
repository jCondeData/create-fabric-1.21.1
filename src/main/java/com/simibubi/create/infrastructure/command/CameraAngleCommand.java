package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService.Mode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;

public class CameraAngleCommand {
    private static final DynamicCommandExceptionType UNKNOWN_MODE =
            new DynamicCommandExceptionType(
                    mode ->
                            Component.literal(
                                    "Unknown camera animation mode '"
                                            + mode
                                            + "', expected one of: "
                                            + String.join(", ", modeNames())));

    public static ArgumentBuilder<FabricClientCommandSource, ?> register() {
        return ClientCommandManager.literal("angle")
                .requires(cs -> cs.hasPermission(2))
                .then(
                        ClientCommandManager.argument("players", EntityArgument.players())
                                .then(
                                        ClientCommandManager.literal("yaw")
                                                .then(
                                                        ClientCommandManager.argument(
                                                                        "degrees",
                                                                        FloatArgumentType
                                                                                .floatArg())
                                                                .executes(
                                                                        ctx -> {
                                                                            float angleTarget =
                                                                                    FloatArgumentType
                                                                                            .getFloat(
                                                                                                    ctx,
                                                                                                    "degrees");
                                                                            CameraAngleAnimationService
                                                                                    .setYawTarget(
                                                                                            angleTarget);

                                                                            return Command
                                                                                    .SINGLE_SUCCESS;
                                                                        })))
                                .then(
                                        ClientCommandManager.literal("pitch")
                                                .then(
                                                        ClientCommandManager.argument(
                                                                        "degrees",
                                                                        FloatArgumentType
                                                                                .floatArg())
                                                                .executes(
                                                                        ctx -> {
                                                                            float angleTarget =
                                                                                    FloatArgumentType
                                                                                            .getFloat(
                                                                                                    ctx,
                                                                                                    "degrees");
                                                                            CameraAngleAnimationService
                                                                                    .setPitchTarget(
                                                                                            angleTarget);

                                                                            return Command
                                                                                    .SINGLE_SUCCESS;
                                                                        })))
                                .then(
                                        ClientCommandManager.literal("mode")
                                                .then(
                                                        // fabric: NeoForge's EnumArgument has no
                                                        // Fabric equivalent; a word argument with
                                                        // suggestions needs no argument type
                                                        // registration
                                                        ClientCommandManager.argument(
                                                                        "mode",
                                                                        StringArgumentType.word())
                                                                .suggests(
                                                                        (ctx, builder) ->
                                                                                SharedSuggestionProvider
                                                                                        .suggest(
                                                                                                modeNames(),
                                                                                                builder))
                                                                .executes(
                                                                        ctx -> {
                                                                            Mode mode =
                                                                                    getMode(ctx);

                                                                            CameraAngleAnimationService
                                                                                    .setAnimationMode(
                                                                                            mode);

                                                                            return Command
                                                                                    .SINGLE_SUCCESS;
                                                                        })
                                                                .then(
                                                                        ClientCommandManager
                                                                                .argument(
                                                                                        "speed",
                                                                                        FloatArgumentType
                                                                                                .floatArg(
                                                                                                        0))
                                                                                .executes(
                                                                                        ctx -> {
                                                                                            Mode
                                                                                                    mode =
                                                                                                            getMode(
                                                                                                                    ctx);
                                                                                            float
                                                                                                    speed =
                                                                                                            FloatArgumentType
                                                                                                                    .getFloat(
                                                                                                                            ctx,
                                                                                                                            "speed");

                                                                                            CameraAngleAnimationService
                                                                                                    .setAnimationMode(
                                                                                                            mode);
                                                                                            CameraAngleAnimationService
                                                                                                    .setAnimationSpeed(
                                                                                                            speed);

                                                                                            return Command
                                                                                                    .SINGLE_SUCCESS;
                                                                                        })))));
    }

    private static Mode getMode(CommandContext<FabricClientCommandSource> ctx)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "mode");
        try {
            return Mode.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw UNKNOWN_MODE.create(name);
        }
    }

    private static String[] modeNames() {
        return Arrays.stream(Mode.values())
                .map(mode -> mode.name().toLowerCase(Locale.ROOT))
                .toArray(String[]::new);
    }
}
