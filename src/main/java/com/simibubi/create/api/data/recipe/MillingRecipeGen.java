package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

/**
 * The base class for Milling recipe generation. Addons should extend this and use the {@link
 * ProcessingRecipeGen#create} methods or the helper methods contained in this class to make
 * recipes. For an example of how you might do this, see Create's implementation: {@link
 * com.simibubi.create.foundation.data.recipe.CreateMillingRecipeGen}. Needs to be added to a
 * registered recipe provider to do anything, see {@link
 * com.simibubi.create.foundation.data.recipe.CreateRecipeProvider}
 */
public abstract class MillingRecipeGen extends ProcessingRecipeGen {

    protected GeneratedRecipe metalOre(
            String name, ItemEntry<? extends Item> crushed, int duration) {
        return create(
                name + "_ore",
                b ->
                        b.duration(duration)
                                .withCondition(
                                        ResourceConditions.tagsPopulated(
                                                AllTags.commonItemTag("ores/" + name)))
                                .require(AllTags.commonItemTag("ores/" + name))
                                .output(crushed.get()));
    }

    public MillingRecipeGen(
            FabricDataOutput output,
            CompletableFuture<HolderLookup.Provider> registries,
            String defaultNamespace) {
        super(output, registries, defaultNamespace);
    }

    @Override
    protected AllRecipeTypes getRecipeType() {
        return AllRecipeTypes.MILLING;
    }
}
