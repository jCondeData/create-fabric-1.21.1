package com.simibubi.create.content.fluids.transfer;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;

import io.github.fabricators_of_create.porting_lib.transfer.MutableContainerItemContext;

import net.createmod.catnip.data.Pair;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class GenericItemEmptying {

    public static boolean canItemBeEmptied(Level world, ItemStack stack) {
        if (PotionFluidHandler.isPotionItem(stack)) return true;

        if (AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), world).isPresent())
            return true;

        Storage<FluidVariant> contained =
                FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
        return contained != null && contained.nonEmptyViews().iterator().hasNext();
    }

    public static Pair<FluidStack, ItemStack> emptyItem(
            Level level, ItemStack stack, boolean simulate) {
        return emptyItem(level, stack, simulate, null);
    }

    public static Pair<FluidStack, ItemStack> emptyItem(
            Level level, ItemStack stack, boolean simulate, TransactionContext parent) {
        FluidStack resultingFluid = FluidStack.EMPTY;
        ItemStack resultingItem = ItemStack.EMPTY;

        if (PotionFluidHandler.isPotionItem(stack))
            return PotionFluidHandler.emptyPotion(stack, simulate);

        Optional<RecipeHolder<Recipe<SingleRecipeInput>>> recipe =
                AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), level);
        if (recipe.isPresent()) {
            EmptyingRecipe emptyingRecipe = (EmptyingRecipe) recipe.get().value();
            List<ItemStack> results = emptyingRecipe.rollResults(level.random);
            if (!simulate) stack.shrink(1);
            resultingItem = results.isEmpty() ? ItemStack.EMPTY : results.get(0);
            resultingFluid = emptyingRecipe.getResultingFluid();
            return Pair.of(resultingFluid, resultingItem);
        }

        ItemStack split = stack.copy();
        split.setCount(1);
        MutableContainerItemContext ctx = new MutableContainerItemContext(split);
        Storage<FluidVariant> tank = FluidStorage.ITEM.find(split, ctx);
        if (tank == null) return Pair.of(resultingFluid, resultingItem);
        try (Transaction t = Transaction.openNested(parent)) {
            resultingFluid = FluidStack.of(StorageUtil.extractAny(tank, FluidConstants.BUCKET, t));
            int amount = ctx.getItemVariant().isBlank() ? 0 : (int) ctx.getAmount(); // GH#1622
            resultingItem = ctx.getItemVariant().toStack(amount);
            if (!simulate) {
                stack.shrink(1);
                t.commit();
            }

            return Pair.of(resultingFluid, resultingItem);
        }
    }
}
