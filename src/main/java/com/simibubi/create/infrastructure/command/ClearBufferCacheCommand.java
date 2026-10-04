package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.CreateClient;

import net.createmod.ponder.PonderClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class ClearBufferCacheCommand {
    static ArgumentBuilder<FabricClientCommandSource, ?> register() {
        return ClientCommandManager.literal("clearRenderBuffers")
                .executes(
                        ctx -> {
                            PonderClient.invalidateRenderers();
                            CreateClient.invalidateRenderers();

                            ctx.getSource()
                                    .sendFeedback(Component.literal("Cleared rendering buffers."));
                            return Command.SINGLE_SUCCESS;
                        });
    }
}
