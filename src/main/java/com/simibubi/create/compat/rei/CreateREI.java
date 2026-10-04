package com.simibubi.create.compat.rei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllCreativeModeTabs.RegistrateDisplayItemsGenerator;
import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.ToolboxColoringRecipeMaker;
import com.simibubi.create.compat.rei.category.BlockCuttingCategory;
import com.simibubi.create.compat.rei.category.BlockCuttingCategory.CondensedBlockCuttingRecipe;
import com.simibubi.create.compat.rei.category.CreateRecipeCategory;
import com.simibubi.create.compat.rei.category.CreateRecipeCategory.Factory;
import com.simibubi.create.compat.rei.category.CrushingCategory;
import com.simibubi.create.compat.rei.category.DeployingCategory;
import com.simibubi.create.compat.rei.category.FanBlastingCategory;
import com.simibubi.create.compat.rei.category.FanHauntingCategory;
import com.simibubi.create.compat.rei.category.FanSmokingCategory;
import com.simibubi.create.compat.rei.category.FanWashingCategory;
import com.simibubi.create.compat.rei.category.ItemApplicationCategory;
import com.simibubi.create.compat.rei.category.ItemDrainCategory;
import com.simibubi.create.compat.rei.category.MechanicalCraftingCategory;
import com.simibubi.create.compat.rei.category.MillingCategory;
import com.simibubi.create.compat.rei.category.MixingCategory;
import com.simibubi.create.compat.rei.category.MysteriousItemConversionCategory;
import com.simibubi.create.compat.rei.category.PackingCategory;
import com.simibubi.create.compat.rei.category.PolishingCategory;
import com.simibubi.create.compat.rei.category.PressingCategory;
import com.simibubi.create.compat.rei.category.ProcessingViaFanCategory;
import com.simibubi.create.compat.rei.category.RecipeLayout;
import com.simibubi.create.compat.rei.category.SawingCategory;
import com.simibubi.create.compat.rei.category.SequencedAssemblyCategory;
import com.simibubi.create.compat.rei.category.SpoutCategory;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionMixingRecipes;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.kinetics.fan.processing.HauntingRecipe;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.data.recipe.LogStrippingFakeRecipes;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.RecipeGenericsUtil;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;

import dev.architectury.event.CompoundEventResult;

import me.shedaniel.rei.api.client.entry.renderer.EntryRendererRegistry;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.FluidComparatorRegistry;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * REI plugin, the counterpart of {@code CreateJEI}. Loaded only through REI's {@code rei_client}
 * entrypoint, so nothing here is touched when REI is absent or on a dedicated server.
 */
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class CreateREI implements REIClientPlugin {

    private static final ResourceLocation ID = Create.asResource("rei_plugin");

    private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

    private void loadCategories() {
        allCategories.clear();

        CreateRecipeCategory<?>
                milling =
                        builder(AbstractCrushingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.MILLING)
                                .catalyst(AllBlocks.MILLSTONE::get)
                                .doubleItemIcon(
                                        AllBlocks.MILLSTONE.get(), AllItems.WHEAT_FLOUR.get())
                                .emptyBackground(177, 53)
                                .build("milling", MillingCategory::new),
                crushing =
                        builder(AbstractCrushingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.CRUSHING)
                                .addTypedRecipesExcluding(
                                        AllRecipeTypes.MILLING::getType,
                                        AllRecipeTypes.CRUSHING::getType)
                                .catalyst(AllBlocks.CRUSHING_WHEEL::get)
                                .doubleItemIcon(
                                        AllBlocks.CRUSHING_WHEEL.get(), AllItems.CRUSHED_GOLD.get())
                                .emptyBackground(177, 100)
                                .build("crushing", CrushingCategory::new),
                pressing =
                        builder(PressingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.PRESSING)
                                .catalyst(AllBlocks.MECHANICAL_PRESS::get)
                                .doubleItemIcon(
                                        AllBlocks.MECHANICAL_PRESS.get(), AllItems.IRON_SHEET.get())
                                .emptyBackground(177, 70)
                                .build("pressing", PressingCategory::new),
                washing =
                        builder(SplashingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.SPLASHING)
                                .catalystStack(ProcessingViaFanCategory.getFan("fan_washing"))
                                .doubleItemIcon(AllItems.PROPELLER.get(), Items.WATER_BUCKET)
                                .emptyBackground(178, 72)
                                .build("fan_washing", FanWashingCategory::new),
                smoking =
                        builder(SmokingRecipe.class)
                                .addTypedRecipes(() -> RecipeType.SMOKING)
                                .removeNonAutomation()
                                .catalystStack(ProcessingViaFanCategory.getFan("fan_smoking"))
                                .doubleItemIcon(AllItems.PROPELLER.get(), Items.CAMPFIRE)
                                .emptyBackground(178, 72)
                                .build("fan_smoking", FanSmokingCategory::new),
                blasting =
                        builder(AbstractCookingRecipe.class)
                                .addTypedRecipesExcluding(
                                        () -> RecipeType.SMELTING, () -> RecipeType.BLASTING)
                                .addTypedRecipes(() -> RecipeType.BLASTING)
                                .removeRecipes(() -> RecipeType.SMOKING)
                                .removeNonAutomation()
                                .catalystStack(ProcessingViaFanCategory.getFan("fan_blasting"))
                                .doubleItemIcon(AllItems.PROPELLER.get(), Items.LAVA_BUCKET)
                                .emptyBackground(178, 72)
                                .build("fan_blasting", FanBlastingCategory::new),
                haunting =
                        builder(HauntingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.HAUNTING)
                                .catalystStack(ProcessingViaFanCategory.getFan("fan_haunting"))
                                .doubleItemIcon(AllItems.PROPELLER.get(), Items.SOUL_CAMPFIRE)
                                .emptyBackground(178, 72)
                                .build("fan_haunting", FanHauntingCategory::new),
                mixing =
                        builder(BasinRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.MIXING)
                                .catalyst(AllBlocks.MECHANICAL_MIXER::get)
                                .catalyst(AllBlocks.BASIN::get)
                                .doubleItemIcon(
                                        AllBlocks.MECHANICAL_MIXER.get(), AllBlocks.BASIN.get())
                                .emptyBackground(177, 103)
                                .build("mixing", MixingCategory::standard),
                autoShapeless =
                        builder(BasinRecipe.class)
                                .enableWhen(AllConfigs.server().recipes.allowShapelessInMixer)
                                .addAllRecipesIf(
                                        r ->
                                                r.value() instanceof CraftingRecipe
                                                        && !(r.value() instanceof ShapedRecipe)
                                                        && r.value().getIngredients().size() > 1
                                                        && !MechanicalPressBlockEntity.canCompress(
                                                                r.value())
                                                        && !AllRecipeTypes.shouldIgnoreInAutomation(
                                                                r),
                                        BasinRecipe::convertShapeless)
                                .catalyst(AllBlocks.MECHANICAL_MIXER::get)
                                .catalyst(AllBlocks.BASIN::get)
                                .doubleItemIcon(
                                        AllBlocks.MECHANICAL_MIXER.get(), Items.CRAFTING_TABLE)
                                .emptyBackground(177, 85)
                                .build("automatic_shapeless", MixingCategory::autoShapeless),
                brewing =
                        builder(BasinRecipe.class)
                                .enableWhen(AllConfigs.server().recipes.allowBrewingInMixer)
                                .addRecipes(
                                        () -> {
                                            Level level = Minecraft.getInstance().level;
                                            if (level == null) return List.of();
                                            return RecipeGenericsUtil.cast(
                                                    PotionMixingRecipes.createRecipes(level));
                                        })
                                .catalyst(AllBlocks.MECHANICAL_MIXER::get)
                                .catalyst(AllBlocks.BASIN::get)
                                .doubleItemIcon(
                                        AllBlocks.MECHANICAL_MIXER.get(), Blocks.BREWING_STAND)
                                .emptyBackground(177, 103)
                                .build("automatic_brewing", MixingCategory::autoBrewing),
                packing =
                        builder(BasinRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.COMPACTING)
                                .catalyst(AllBlocks.MECHANICAL_PRESS::get)
                                .catalyst(AllBlocks.BASIN::get)
                                .doubleItemIcon(
                                        AllBlocks.MECHANICAL_PRESS.get(), AllBlocks.BASIN.get())
                                .emptyBackground(177, 103)
                                .build("packing", PackingCategory::standard),
                autoSquare =
                        builder(BasinRecipe.class)
                                .enableWhen(AllConfigs.server().recipes.allowShapedSquareInPress)
                                .addAllRecipesIf(
                                        r ->
                                                (r.value() instanceof CraftingRecipe)
                                                        && !(r.value()
                                                                instanceof MechanicalCraftingRecipe)
                                                        && MechanicalPressBlockEntity.canCompress(
                                                                r.value())
                                                        && !AllRecipeTypes.shouldIgnoreInAutomation(
                                                                r),
                                        BasinRecipe::convertShapeless)
                                .catalyst(AllBlocks.MECHANICAL_PRESS::get)
                                .catalyst(AllBlocks.BASIN::get)
                                .doubleItemIcon(
                                        AllBlocks.MECHANICAL_PRESS.get(), Blocks.CRAFTING_TABLE)
                                .emptyBackground(177, 85)
                                .build("automatic_packing", PackingCategory::autoSquare),
                sawing =
                        builder(CuttingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.CUTTING)
                                .catalyst(AllBlocks.MECHANICAL_SAW::get)
                                .doubleItemIcon(AllBlocks.MECHANICAL_SAW.get(), Items.OAK_LOG)
                                .emptyBackground(177, 70)
                                .build("sawing", SawingCategory::new),
                blockCutting =
                        builder(CondensedBlockCuttingRecipe.class)
                                .enableWhen(AllConfigs.server().recipes.allowStonecuttingOnSaw)
                                .addRecipes(
                                        () ->
                                                BlockCuttingCategory.condenseRecipes(
                                                        getTypedRecipesExcluding(
                                                                RecipeType.STONECUTTING,
                                                                AllRecipeTypes
                                                                        ::shouldIgnoreInAutomation)))
                                .catalyst(AllBlocks.MECHANICAL_SAW::get)
                                .doubleItemIcon(
                                        AllBlocks.MECHANICAL_SAW.get(), Items.STONE_BRICK_STAIRS)
                                .emptyBackground(177, 70)
                                .build("block_cutting", BlockCuttingCategory::new),
                polishing =
                        builder(SandPaperPolishingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.SANDPAPER_POLISHING)
                                .catalyst(AllItems.SAND_PAPER::get)
                                .catalyst(AllItems.RED_SAND_PAPER::get)
                                .itemIcon(AllItems.SAND_PAPER.get())
                                .emptyBackground(177, 55)
                                .build("sandpaper_polishing", PolishingCategory::new),
                item_application =
                        builder(ItemApplicationRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.ITEM_APPLICATION)
                                .addRecipes(
                                        () ->
                                                RecipeGenericsUtil.cast(
                                                        LogStrippingFakeRecipes.createRecipes()))
                                .itemIcon(AllItems.BRASS_HAND.get())
                                .emptyBackground(177, 60)
                                .build("item_application", ItemApplicationCategory::new),
                deploying =
                        builder(DeployerApplicationRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.DEPLOYING)
                                .addTypedRecipes(
                                        AllRecipeTypes.SANDPAPER_POLISHING::getType,
                                        DeployerApplicationRecipe::convert)
                                .addTypedRecipes(
                                        AllRecipeTypes.ITEM_APPLICATION::getType,
                                        ManualApplicationRecipe::asDeploying)
                                .removeNonAutomation()
                                .catalyst(AllBlocks.DEPLOYER::get)
                                .catalyst(AllBlocks.DEPOT::get)
                                .catalyst(AllItems.BELT_CONNECTOR::get)
                                .itemIcon(AllBlocks.DEPLOYER.get())
                                .emptyBackground(177, 70)
                                .build("deploying", DeployingCategory::new),
                spoutFilling =
                        builder(FillingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.FILLING)
                                .addRecipeListConsumer(
                                        recipes -> SpoutCategory.consumeRecipes(recipes::add))
                                .catalyst(AllBlocks.SPOUT::get)
                                .doubleItemIcon(AllBlocks.SPOUT.get(), Items.WATER_BUCKET)
                                .emptyBackground(177, 70)
                                .build("spout_filling", SpoutCategory::new),
                draining =
                        builder(EmptyingRecipe.class)
                                .addRecipeListConsumer(
                                        recipes -> ItemDrainCategory.consumeRecipes(recipes::add))
                                .addTypedRecipes(AllRecipeTypes.EMPTYING)
                                .catalyst(AllBlocks.ITEM_DRAIN::get)
                                .doubleItemIcon(AllBlocks.ITEM_DRAIN.get(), Items.WATER_BUCKET)
                                .emptyBackground(177, 50)
                                .build("draining", ItemDrainCategory::new),
                autoShaped =
                        builder(CraftingRecipe.class)
                                .enableWhen(
                                        AllConfigs.server().recipes.allowRegularCraftingInCrafter)
                                .addAllRecipesIf(
                                        r ->
                                                r.value() instanceof CraftingRecipe
                                                        && !(r.value() instanceof ShapedRecipe)
                                                        && r.value().getIngredients().size() == 1
                                                        && !AllRecipeTypes.shouldIgnoreInAutomation(
                                                                r))
                                .addTypedRecipesIf(
                                        () -> RecipeType.CRAFTING,
                                        recipe ->
                                                recipe.value() instanceof ShapedRecipe
                                                        && !AllRecipeTypes.shouldIgnoreInAutomation(
                                                                recipe))
                                .catalyst(AllBlocks.MECHANICAL_CRAFTER::get)
                                .itemIcon(AllBlocks.MECHANICAL_CRAFTER.get())
                                .emptyBackground(177, 107)
                                .build("automatic_shaped", MechanicalCraftingCategory::new),
                mechanicalCrafting =
                        builder(CraftingRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.MECHANICAL_CRAFTING)
                                .catalyst(AllBlocks.MECHANICAL_CRAFTER::get)
                                .itemIcon(AllBlocks.MECHANICAL_CRAFTER.get())
                                .emptyBackground(177, 107)
                                .build("mechanical_crafting", MechanicalCraftingCategory::new),
                seqAssembly =
                        builder(SequencedAssemblyRecipe.class)
                                .addTypedRecipes(AllRecipeTypes.SEQUENCED_ASSEMBLY)
                                .itemIcon(AllItems.PRECISION_MECHANISM.get())
                                .emptyBackground(180, 115)
                                .build("sequenced_assembly", SequencedAssemblyCategory::new),
                mysteryConversion =
                        builder(ConversionRecipe.class)
                                .addRecipes(MysteriousItemConversionCategory::getRecipes)
                                .itemIcon(AllBlocks.PECULIAR_BELL.get())
                                .emptyBackground(177, 50)
                                .build("mystery_conversion", MysteriousItemConversionCategory::new);
    }

    private <T extends Recipe<? extends RecipeInput>> CategoryBuilder<T> builder(
            Class<T> recipeClass) {
        return new CategoryBuilder<>(recipeClass);
    }

    @Override
    public String getPluginProviderName() {
        return ID.toString();
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        allCategories.clear();
        // the categories read Create's server config and the recipes need a world; REI only
        // has those after joining one
        if (Minecraft.getInstance().level == null) return;
        loadCategories();
        allCategories.forEach(
                category -> {
                    registry.add(category);
                    category.registerCatalysts(registry);
                });
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (Minecraft.getInstance().level == null) return;
        allCategories.forEach(c -> c.registerRecipes(registry));

        ToolboxColoringRecipeMaker.createRecipes()
                .forEach(
                        recipe -> {
                            for (Display display : registry.tryFillDisplay(recipe)) {
                                if (Objects.equals(
                                        display.getCategoryIdentifier(), BuiltinPlugin.CRAFTING))
                                    registry.add(display, recipe);
                            }
                        });
    }

    @Override
    public void registerEntryRenderers(EntryRendererRegistry registry) {
        Fluid potionSource = AllFluids.POTION.get().getSource();
        Fluid potionFlowing = AllFluids.POTION.get().getFlowing();
        registry.register(
                VanillaEntryTypes.FLUID,
                (entry, last) -> {
                    Fluid fluid = entry.getValue().getFluid();
                    return fluid == potionSource || fluid == potionFlowing
                            ? new PotionFluidEntryRenderer(last)
                            : last;
                });
    }

    @Override
    public void registerFluidComparators(FluidComparatorRegistry registry) {
        // REI counterpart of JEI's PotionFluidSubtypeInterpreter: potion fluids differ by their
        // components, otherwise REI merges all of them into a single entry. (REI 16's
        // EntryComparator.fluidComponents() is a stub that always returns 0, so hash them here.)
        EntryComparator<DataComponentMap> components = EntryComparator.component();
        PotionFluid potionFluid = AllFluids.POTION.get();
        registry.register(
                (context, stack) -> components.hash(context, stack.getComponents()),
                potionFluid.getSource(),
                potionFluid.getFlowing());
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        // REI lists every registered item, not just the creative tab contents like JEI does, so
        // hide the same items Create keeps out of its creative tabs
        Predicate<Item> hiddenItems = RegistrateDisplayItemsGenerator.makeExclusionPredicate();
        PotionFluid potionFluid = AllFluids.POTION.get();
        Set<Fluid> hiddenFluids =
                Set.of(
                        potionFluid.getSource(),
                        potionFluid.getFlowing(),
                        AllFluids.TEA.get().getSource(),
                        AllFluids.TEA.get().getFlowing());
        registry.removeEntryIf(
                entry -> {
                    if (entry.getType() == VanillaEntryTypes.ITEM) {
                        ItemStack stack = entry.castValue();
                        return hiddenItems.test(stack.getItem());
                    }
                    if (entry.getType() == VanillaEntryTypes.FLUID) {
                        dev.architectury.fluid.FluidStack stack = entry.castValue();
                        return hiddenFluids.contains(stack.getFluid());
                    }
                    return false;
                });

        // JEI also lists one fluid per potion here. REI can't: it normalizes list entries with
        // Architectury's FluidStack#copyWithAmount, which drops the components on Fabric, so
        // every potion would show up as the same "Uncraftable Potion". Potion fluids still show
        // up (and can be looked up) inside recipes.
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(AbstractSimiContainerScreen.class, new SlotMover());
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDraggableStackVisitor(new GhostIngredientHandler<>());

        // lets REI look up the items shown in the stock keeper's request screen
        registry.registerFocusedStack(
                (screen, mouse) -> {
                    if (!(screen instanceof StockKeeperRequestScreen stockKeeper))
                        return CompoundEventResult.pass();
                    return stockKeeper
                            .getHoveredIngredient(mouse.x, mouse.y)
                            .<CompoundEventResult<EntryStack<?>>>map(
                                    pair ->
                                            CompoundEventResult.interruptTrue(
                                                    EntryStacks.of(pair.getFirst())))
                            .orElseGet(CompoundEventResult::pass);
                });
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new BlueprintTransferHandler());
        registry.register(new StockKeeperTransferHandler());
    }

    private class CategoryBuilder<T extends Recipe<? extends RecipeInput>>
            extends CreateRecipeCategory.Builder<T> {
        public CategoryBuilder(Class<? extends T> recipeClass) {
            super(recipeClass);
        }

        @Override
        public CreateRecipeCategory<T> build(ResourceLocation id, Factory<T> factory) {
            CreateRecipeCategory<T> category = super.build(id, factory);
            allCategories.add(category);
            return category;
        }
    }

    private static RecipeManager recipeManager() {
        return DisplayRegistry.getInstance().getRecipeManager();
    }

    public static void consumeAllRecipes(Consumer<? super RecipeHolder<?>> consumer) {
        recipeManager().getRecipes().forEach(consumer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void consumeTypedRecipes(Consumer<RecipeHolder<?>> consumer, RecipeType<?> type) {
        List<? extends RecipeHolder<?>> recipes =
                recipeManager().getAllRecipesFor((RecipeType) type);
        recipes.forEach(consumer);
    }

    public static List<RecipeHolder<?>> getTypedRecipes(RecipeType<?> type) {
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        consumeTypedRecipes(recipes::add, type);
        return recipes;
    }

    public static List<RecipeHolder<?>> getTypedRecipesExcluding(
            RecipeType<?> type, Predicate<RecipeHolder<?>> exclusionPred) {
        List<RecipeHolder<?>> recipes = getTypedRecipes(type);
        recipes.removeIf(exclusionPred);
        return recipes;
    }

    public static boolean doInputsMatch(Recipe<?> recipe1, Recipe<?> recipe2) {
        if (recipe1.getIngredients().isEmpty() || recipe2.getIngredients().isEmpty()) {
            return false;
        }
        ItemStack[] matchingStacks = recipe1.getIngredients().getFirst().getItems();
        if (matchingStacks.length == 0) {
            return false;
        }
        return recipe2.getIngredients().getFirst().test(matchingStacks[0]);
    }

    public static boolean doOutputsMatch(Recipe<?> recipe1, Recipe<?> recipe2) {
        RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
        return ItemHelper.sameItem(
                recipe1.getResultItem(registryAccess), recipe2.getResultItem(registryAccess));
    }

    /**
     * All fluid entries REI knows about, as Create fluid stacks (JEI: all FLUID_STACK ingredients).
     */
    public static List<FluidStack> getAllFluids() {
        return EntryRegistry.getInstance()
                .getEntryStacks()
                .filter(entry -> entry.getType() == VanillaEntryTypes.FLUID)
                .map(entry -> RecipeLayout.fromRei(entry.castValue()))
                .toList();
    }

    /** All item entries REI knows about (JEI: all ITEM_STACK ingredients). */
    public static List<ItemStack> getAllItems() {
        return EntryRegistry.getInstance()
                .getEntryStacks()
                .filter(entry -> entry.getType() == VanillaEntryTypes.ITEM)
                .map(entry -> (ItemStack) entry.castValue())
                .toList();
    }
}
