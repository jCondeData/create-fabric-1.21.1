package com.simibubi.create.api.data.recipe;

import com.simibubi.create.Create;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A class containing some basic setup for other recipe generators to use. Addons should extend this
 * if they add a custom recipe type that is not a processing recipe type and want to use Create's
 * helpers. For processing recipes extend {@link ProcessingRecipeGen}.
 *
 * <p>fabric: built on {@link FabricRecipeProvider} so that Fabric resource conditions attached by
 * the recipe builders (e.g. {@code whenModLoaded}) are written into the generated json.
 */
public abstract class BaseRecipeProvider extends FabricRecipeProvider {
    protected final String modid;
    protected final List<GeneratedRecipe> all = new ArrayList<>();

    public BaseRecipeProvider(
            FabricDataOutput output,
            CompletableFuture<HolderLookup.Provider> registries,
            String defaultNamespace) {
        super(output, registries);
        this.modid = defaultNamespace;
    }

    protected ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(modid, path);
    }

    protected GeneratedRecipe register(GeneratedRecipe recipe) {
        all.add(recipe);
        return recipe;
    }

    @Override
    public void buildRecipes(RecipeOutput recipeOutput) {
        all.forEach(c -> c.register(recipeOutput));
        Create.LOGGER.info(
                "{} registered {} recipe{}", getName(), all.size(), all.size() == 1 ? "" : "s");
    }

    // fabric: FabricRecipeProvider moves every recipe into the namespace of the mod running
    // datagen; keep the id the recipe was built with, as upstream does
    @Override
    protected ResourceLocation getRecipeIdentifier(ResourceLocation identifier) {
        return identifier;
    }

    @FunctionalInterface
    public interface GeneratedRecipe {
        void register(RecipeOutput recipeOutput);
    }
}
