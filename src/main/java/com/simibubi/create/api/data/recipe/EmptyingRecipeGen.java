package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

/**
 * The base class for Emptying recipe generation. Addons should extend this and use the {@link
 * ProcessingRecipeGen#create} methods to make recipes. For an example of how you might do this, see
 * Create's implementation: {@link
 * com.simibubi.create.foundation.data.recipe.CreateEmptyingRecipeGen}. Needs to be added to a
 * registered recipe provider to do anything, see {@link
 * com.simibubi.create.foundation.data.recipe.CreateRecipeProvider}
 */
public abstract class EmptyingRecipeGen extends ProcessingRecipeGen {

    public EmptyingRecipeGen(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries,
            String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected AllRecipeTypes getRecipeType() {
        return AllRecipeTypes.EMPTYING;
    }
}
