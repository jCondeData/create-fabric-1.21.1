package com.simibubi.create.compat.rei;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.foundation.utility.CreateLang;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * REI version of the JEI stock keeper transfer handler: clicking "+" on a recipe while a stock
 * keeper's request screen is open orders its ingredients for crafting, if they are in stock.
 */
public class StockKeeperTransferHandler implements TransferHandler {

    @Override
    public Result handle(Context context) {
        if (!(context.getContainerScreen() instanceof StockKeeperRequestScreen screen))
            return Result.createNotApplicable();

        RecipeHolder<?> recipeHolder = findRecipe(context.getDisplay());
        if (recipeHolder == null) return Result.createNotApplicable();

        Recipe<?> recipe = recipeHolder.value();
        if (recipe.getIngredients().size() > 9) return Result.createNotApplicable();

        for (CraftableBigItemStack cbis : screen.recipesToOrder)
            if (cbis.recipe == recipe)
                return Result.createFailed(
                        CreateLang.translate("gui.stock_keeper.already_ordering_recipe")
                                .component());

        if (screen.itemsToOrder.size() >= 9)
            return Result.createFailed(
                    CreateLang.translate("gui.stock_keeper.slots_full").component());

        InventorySummary summary =
                screen.getMenu().contentHolder.getLastClientsideStockSnapshotAsSummary();
        if (summary == null) return Result.createNotApplicable();

        List<EntryIngredient> missing = findMissingIngredients(recipe, summary);
        if (!missing.isEmpty())
            return Result.createFailed(
                            CreateLang.translate("gui.stock_keeper.not_in_stock").component())
                    .tooltipMissing(missing);

        if (!context.isActuallyCrafting()) return Result.createSuccessful();

        ItemStack result = recipe.getResultItem(context.getMinecraft().level.registryAccess());
        if (result.isEmpty())
            return Result.createFailed(
                    CreateLang.translate("gui.stock_keeper.recipe_result_empty").component());

        CraftableBigItemStack cbis = new CraftableBigItemStack(result, recipe);

        screen.recipesToOrder.add(cbis);
        screen.searchBox.setValue("");
        screen.refreshSearchNextTick = true;
        screen.requestCraftable(
                cbis, context.isStackedCrafting() ? cbis.stack.getMaxStackSize() : 1);

        context.getMinecraft().setScreen(screen);
        return Result.createSuccessful().blocksFurtherHandling();
    }

    @Nullable
    private static RecipeHolder<?> findRecipe(Display display) {
        Object origin = DisplayRegistry.getInstance().getDisplayOrigin(display);
        if (origin instanceof RecipeHolder<?> holder) return holder;
        if (Minecraft.getInstance().level == null) return null;
        return display.getDisplayLocation()
                .flatMap(id -> Minecraft.getInstance().level.getRecipeManager().byKey(id))
                .orElse(null);
    }

    /** Every recipe ingredient needs one matching item from the stock, like a crafting grid. */
    private static List<EntryIngredient> findMissingIngredients(
            Recipe<?> recipe, InventorySummary summary) {
        List<BigItemStack> stock = summary.getStacksByCount();
        int[] remaining = new int[stock.size()];
        for (int i = 0; i < stock.size(); i++) remaining[i] = stock.get(i).count;

        List<EntryIngredient> missing = new ArrayList<>();
        Ingredients:
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            for (int i = 0; i < stock.size(); i++) {
                if (remaining[i] > 0 && ingredient.test(stock.get(i).stack)) {
                    remaining[i]--;
                    continue Ingredients;
                }
            }
            missing.add(EntryIngredients.ofIngredient(ingredient));
        }
        return missing;
    }
}
