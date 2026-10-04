package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.PolishingRecipeGen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

/**
 * Create's own Data Generation for the singular default polishing recipe
 *
 * @see PolishingRecipeGen
 */
@SuppressWarnings("unused")
public final class CreatePolishingRecipeGen extends PolishingRecipeGen {

    GeneratedRecipe ROSE_QUARTZ =
            create(AllItems.ROSE_QUARTZ::get, b -> b.output(AllItems.POLISHED_ROSE_QUARTZ.get()));

    public CreatePolishingRecipeGen(
            FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create.ID);
    }
}
