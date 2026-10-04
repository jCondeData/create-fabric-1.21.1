package com.simibubi.create.content.processing.sequenced;

import com.simibubi.create.compat.recipeViewerCommon.SequencedAssemblySubCategoryType;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Set;

public interface IAssemblyRecipe {
    default boolean supportsAssembly() {
        return true;
    }

    @Environment(EnvType.CLIENT)
    Component getDescriptionForAssembly();

    void addRequiredMachines(Set<ItemLike> list);

    void addAssemblyIngredients(List<Ingredient> list);

    default void addAssemblyFluidIngredients(List<FluidIngredient> list) {}

    SequencedAssemblySubCategoryType getJEISubCategory();
}
