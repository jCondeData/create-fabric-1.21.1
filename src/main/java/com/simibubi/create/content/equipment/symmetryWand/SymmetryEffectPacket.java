package com.simibubi.create.content.equipment.symmetryWand;

import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public record SymmetryEffectPacket(BlockPos mirror, List<BlockPos> positions)
        implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, SymmetryEffectPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SymmetryEffectPacket::mirror,
                    CatnipStreamCodecBuilders.list(BlockPos.STREAM_CODEC),
                    SymmetryEffectPacket::positions,
                    SymmetryEffectPacket::new);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return AllPackets.SYMMETRY_EFFECT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (player.position().distanceTo(Vec3.atLowerCornerOf(mirror)) > 100) return;
        for (BlockPos to : positions) SymmetryHandler.drawEffect(mirror, to);
    }
}
