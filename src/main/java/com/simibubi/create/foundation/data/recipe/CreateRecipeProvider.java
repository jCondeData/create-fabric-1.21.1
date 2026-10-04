package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.recipe.ProcessingRecipeGen;

import io.github.fabricators_of_create.porting_lib.tags.Tags;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The class that handles gathering Create's generated recipes for most types. Data here is only
 * generated when running server dategen
 *
 * @see com.simibubi.create.infrastructure.data.CreateDatagen
 */
public final class CreateRecipeProvider extends FabricRecipeProvider {

    static final List<ProcessingRecipeGen<?, ?, ?>> GENERATORS = new ArrayList<>();
    // fabric: fluid amounts are in droplets
    static final long BUCKET = FluidConstants.BUCKET;
    static final long BOTTLE = FluidConstants.BOTTLE;

    public CreateRecipeProvider(
            FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput recipeOutput) {}

    // fabric: registered on the FabricDataGenerator pack instead of a NeoForge DataGenerator
    public static void registerAllProcessing(FabricDataGenerator.Pack pack) {
        pack.addProvider(
                (FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) -> {
                    GENERATORS.add(new CreateCrushingRecipeGen(output, registries));
                    GENERATORS.add(new CreateMillingRecipeGen(output, registries));
                    GENERATORS.add(new CreateCuttingRecipeGen(output, registries));
                    GENERATORS.add(new CreateWashingRecipeGen(output, registries));
                    GENERATORS.add(new CreatePolishingRecipeGen(output, registries));
                    GENERATORS.add(new CreateDeployingRecipeGen(output, registries));
                    GENERATORS.add(new CreateMixingRecipeGen(output, registries));
                    GENERATORS.add(new CreateCompactingRecipeGen(output, registries));
                    GENERATORS.add(new CreatePressingRecipeGen(output, registries));
                    GENERATORS.add(new CreateFillingRecipeGen(output, registries));
                    GENERATORS.add(new CreateEmptyingRecipeGen(output, registries));
                    GENERATORS.add(new CreateHauntingRecipeGen(output, registries));
                    GENERATORS.add(new CreateItemApplicationRecipeGen(output, registries));

                    return new DataProvider() {

                        @Override
                        public String getName() {
                            return "Create's Processing Recipes";
                        }

                        @Override
                        public CompletableFuture<?> run(CachedOutput dc) {
                            return CompletableFuture.allOf(
                                    GENERATORS.stream()
                                            .map(gen -> gen.run(dc))
                                            .toArray(CompletableFuture[]::new));
                        }
                    };
                });
    }

    protected static class I {

        static TagKey<Item> redstone() {
            return Tags.Items.DUSTS_REDSTONE;
        }

        static TagKey<Item> planks() {
            return ItemTags.PLANKS;
        }

        static TagKey<Item> woodSlab() {
            return ItemTags.WOODEN_SLABS;
        }

        static TagKey<Item> gold() {
            return Tags.Items.INGOTS_GOLD;
        }

        static TagKey<Item> goldSheet() {
            return AllTags.commonItemTag("gold_plates");
        }

        static TagKey<Item> stone() {
            return Tags.Items.STONES;
        }

        static ItemLike andesiteAlloy() {
            return AllItems.ANDESITE_ALLOY.get();
        }

        static ItemLike shaft() {
            return AllBlocks.SHAFT.get();
        }

        static ItemLike cog() {
            return AllBlocks.COGWHEEL.get();
        }

        static ItemLike largeCog() {
            return AllBlocks.LARGE_COGWHEEL.get();
        }

        static ItemLike andesiteCasing() {
            return AllBlocks.ANDESITE_CASING.get();
        }

        static ItemLike vault() {
            return AllBlocks.ITEM_VAULT.get();
        }

        static ItemLike stockLink() {
            return AllBlocks.STOCK_LINK.get();
        }

        static TagKey<Item> brass() {
            return AllTags.commonItemTag("brass_ingots");
        }

        static TagKey<Item> brassSheet() {
            return AllTags.commonItemTag("brass_plates");
        }

        static TagKey<Item> iron() {
            return Tags.Items.INGOTS_IRON;
        }

        static TagKey<Item> ironNugget() {
            return Tags.Items.NUGGETS_IRON;
        }

        static TagKey<Item> zinc() {
            return AllTags.commonItemTag("zinc_ingots");
        }

        static TagKey<Item> ironSheet() {
            return AllTags.commonItemTag("iron_plates");
        }

        static TagKey<Item> sturdySheet() {
            return AllTags.commonItemTag("obsidian_plates");
        }

        static ItemLike brassCasing() {
            return AllBlocks.BRASS_CASING.get();
        }

        static ItemLike cardboard() {
            return AllItems.CARDBOARD.get();
        }

        static ItemLike railwayCasing() {
            return AllBlocks.RAILWAY_CASING.get();
        }

        static ItemLike electronTube() {
            return AllItems.ELECTRON_TUBE.get();
        }

        static ItemLike precisionMechanism() {
            return AllItems.PRECISION_MECHANISM.get();
        }

        static TagKey<Item> brassBlock() {
            return AllTags.commonItemTag("brass_blocks");
        }

        static TagKey<Item> zincBlock() {
            return AllTags.commonItemTag("zinc_blocks");
        }

        static TagKey<Item> wheatFlour() {
            return AllTags.commonItemTag("flours/wheat");
        }

        static TagKey<Item> copper() {
            return Tags.Items.INGOTS_COPPER;
        }

        static TagKey<Item> copperNugget() {
            return AllTags.commonItemTag("copper_nuggets");
        }

        static TagKey<Item> copperBlock() {
            return Tags.Items.STORAGE_BLOCKS_COPPER;
        }

        static TagKey<Item> copperSheet() {
            return AllTags.commonItemTag("copper_plates");
        }

        static TagKey<Item> brassNugget() {
            return AllTags.commonItemTag("brass_nuggets");
        }

        static TagKey<Item> zincNugget() {
            return AllTags.commonItemTag("zinc_nuggets");
        }

        static ItemLike copperCasing() {
            return AllBlocks.COPPER_CASING.get();
        }

        static ItemLike refinedRadiance() {
            return AllItems.REFINED_RADIANCE.get();
        }

        static ItemLike shadowSteel() {
            return AllItems.SHADOW_STEEL.get();
        }

        static Ingredient netherite() {
            return Ingredient.of(Tags.Items.INGOTS_NETHERITE);
        }
    }
}
