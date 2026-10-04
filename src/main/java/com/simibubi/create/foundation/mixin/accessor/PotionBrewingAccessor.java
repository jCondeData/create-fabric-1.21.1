package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(PotionBrewing.class)
public interface PotionBrewingAccessor {
    @Accessor("potionMixes")
    List<PotionBrewing.Mix<Potion>> create$getPotionMixes();

    @Accessor("containerMixes")
    List<PotionBrewing.Mix<Item>> create$getContainerMixes();

    @Invoker("isContainer")
    boolean create$isContainer(ItemStack stack);
}
