package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemFrame.class)
public interface ItemFrameAccessor {
    @Invoker("getFrameItemStack")
    ItemStack create$getFrameItemStack();
}
