package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.createmod.catnip.command.CatnipCommands;
import net.createmod.catnip.platform.CatnipServices;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.Collections;

public class AllCommands {
    // Client Commands

    // fabric: registered through ClientCommandRegistrationCallback, see ClientEvents
    public static void registerClient(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> util = buildClientUtilityCommands();

        LiteralArgumentBuilder<FabricClientCommandSource> root =
                ClientCommandManager.literal("create")
                        .requires(cs -> cs.hasPermission(0))
                        // general purpose
                        .then(ToggleDebugCommand.register())
                        .then(FabulousWarningCommand.register())
                        .then(OverlayConfigCommand.register())
                        // .then(FixLightingCommand.register()) fabric: NeoForge only command

                        // utility
                        .then(util)
                        .then(forwardToServer());

        LiteralCommandNode<FabricClientCommandSource> createRoot = dispatcher.register(root);
        createRoot.addChild(buildRedirect("u", util));
        createOrAddToShortcut(dispatcher, "c", createRoot);
    }

    private static LiteralCommandNode<FabricClientCommandSource> buildClientUtilityCommands() {
        return ClientCommandManager.literal("util")
                .then(ClearBufferCacheCommand.register())
                .then(CameraDistanceCommand.register())
                .then(CameraAngleCommand.register())
                .then(forwardToServer())
                .build();
    }

    // Server Commands

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> util = buildUtilityCommands();

        LiteralArgumentBuilder<CommandSourceStack> root =
                Commands.literal("create")
                        .requires(cs -> cs.hasPermission(0))
                        // general purpose
                        .then(DumpRailwaysCommand.register())
                        .then(DebugInfoCommand.register())
                        .then(HighlightCommand.register())
                        .then(PassengerCommand.register())
                        .then(CouplingCommand.register())
                        .then(CloneCommand.register())
                        .then(TrainCommand.register())
                        .then(GlueCommand.register())

                        // utility
                        .then(util);

        if (CatnipServices.PLATFORM.isDevelopmentEnvironment()
                && CatnipServices.PLATFORM.getEnv().isClient())
            root.then(CreateTestCommand.register());

        LiteralCommandNode<CommandSourceStack> createRoot = dispatcher.register(root);
        createRoot.addChild(CatnipCommands.buildRedirect("u", util));
        CatnipCommands.createOrAddToShortcut(dispatcher, "c", createRoot);
    }

    private static LiteralCommandNode<CommandSourceStack> buildUtilityCommands() {
        return Commands.literal("util")
                .then(ReplaceInCommandBlocksCommand.register())
                // .then(DebugValueCommand.register())
                // .then(KillTPSCommand.register())
                // .then(DebugHatsCommand.register())
                .build();
    }

    // fabric: Fabric API runs client commands first and only passes the input on to the server
    // when the client dispatcher fails with an unknown command or a parse error. /create, /c and
    // their util nodes exist on both sides, so a server-only sub-command (e.g. /create glue) would
    // fail on the client with "incorrect argument" and never reach the server. This catch-all
    // argument turns every sub-command the client doesn't know into a parse error instead, which
    // Fabric forwards to the server. It is client-only and never serialized.
    private static RequiredArgumentBuilder<FabricClientCommandSource, Void> forwardToServer() {
        return ClientCommandManager.argument("server_command", ServerCommandArgument.INSTANCE);
    }

    private static final class ServerCommandArgument implements ArgumentType<Void> {
        private static final ServerCommandArgument INSTANCE = new ServerCommandArgument();

        @Override
        public Void parse(StringReader reader) throws CommandSyntaxException {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                    .dispatcherParseException()
                    .createWithContext(reader, "not a client-side command");
        }
    }

    // fabric: generic copies of CatnipCommands#buildRedirect / #createOrAddToShortcut, which
    // only accept CommandSourceStack
    private static <S> LiteralCommandNode<S> buildRedirect(
            String alias, LiteralCommandNode<S> destination) {
        LiteralArgumentBuilder<S> builder =
                LiteralArgumentBuilder.<S>literal(alias)
                        .requires(destination.getRequirement())
                        .forward(
                                destination.getRedirect(),
                                destination.getRedirectModifier(),
                                destination.isFork())
                        .executes(destination.getCommand());
        for (CommandNode<S> child : destination.getChildren()) builder.then(child);
        return builder.build();
    }

    private static <S> void createOrAddToShortcut(
            CommandDispatcher<S> dispatcher, String shortcut, LiteralCommandNode<S> root) {
        CommandNode<S> node = dispatcher.findNode(Collections.singleton(shortcut));
        if (node != null) {
            for (CommandNode<S> child : root.getChildren()) node.addChild(child);
            return;
        }

        dispatcher.getRoot().addChild(buildRedirect(shortcut, root));
    }
}
