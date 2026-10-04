package com.simibubi.create.content.kinetics.drill;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CobbleGenOptimisation {

    static CobbleGenLevel cachedLevel;

    public record CobbleGenBlockConfiguration(List<BlockState> statesAroundDrill) {}

    @Nullable
    public static CobbleGenBlockConfiguration getConfig(
            LevelAccessor level, BlockPos drillPos, Direction drillDirection) {
        List<BlockState> list = new ArrayList<>();
        for (Direction side : Iterate.directions) {
            BlockPos relative = drillPos.relative(drillDirection).relative(side);
            if (level instanceof Level l && !l.isLoaded(relative)) return null;
            list.add(level.getBlockState(relative));
        }
        return new CobbleGenBlockConfiguration(list);
    }

    // ponytail: cobble-gen fast path disabled on Fabric — Porting Lib's FluidInteractionRegistry
    // keeps its
    // interaction map private, so we can't predict the drill's output. Drills fall back to normal
    // breaking.
    // Revisit with an accessor mixin on FluidInteractionRegistry#INTERACTIONS if cobble farms lag.
    public static BlockState determineOutput(
            ServerLevel level, BlockPos pos, CobbleGenBlockConfiguration config) {
        return Blocks.AIR.defaultBlockState();
    }

    public static void invalidateWorld(LevelAccessor world) {
        if (cachedLevel != null && cachedLevel.getLevel() == world) cachedLevel = null;
    }
}
