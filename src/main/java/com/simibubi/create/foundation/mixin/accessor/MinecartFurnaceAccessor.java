package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.world.entity.vehicle.MinecartFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecartFurnace.class)
public interface MinecartFurnaceAccessor {
    @Accessor("fuel")
    int create$getFuel();

    @Accessor("fuel")
    void create$setFuel(int fuel);
}
