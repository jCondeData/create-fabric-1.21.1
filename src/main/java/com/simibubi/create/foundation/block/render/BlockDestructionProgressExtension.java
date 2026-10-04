package com.simibubi.create.foundation.block.render;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface BlockDestructionProgressExtension {
    @Nullable
    Set<BlockPos> create$getExtraPositions();

    void create$setExtraPositions(@Nullable Set<BlockPos> positions);
}
