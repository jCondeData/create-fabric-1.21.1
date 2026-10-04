package com.simibubi.create.compat.farmersdelight;

import com.simibubi.create.compat.Mods;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FarmersDelightCompat {
    // fabric: Farmer's Delight is not a compile dependency, so rich soil is matched by id
    public static boolean shouldHarvestMushroom(Level world, BlockPos pos, BlockState state) {
        BlockState below = world.getBlockState(pos.below());
        return !Mods.FARMERSDELIGHT
                .rl("rich_soil")
                .equals(BuiltInRegistries.BLOCK.getKey(below.getBlock()));
    }
}
