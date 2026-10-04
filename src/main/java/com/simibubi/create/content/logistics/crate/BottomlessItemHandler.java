package com.simibubi.create.content.logistics.crate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BottomlessItemHandler extends ItemStackHandler {

    private Supplier<ItemStack> suppliedItemStack;

    public BottomlessItemHandler(Supplier<ItemStack> suppliedItemStack) {
        this.suppliedItemStack = suppliedItemStack;
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack stack = suppliedItemStack.get();
        if (slot == 1) return ItemStack.EMPTY;
        if (stack == null) return ItemStack.EMPTY;
        if (!stack.isEmpty())
            return stack.copyWithCount(stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 64));
        return stack;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {}

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = suppliedItemStack.get();
        if (slot == 1) return ItemStack.EMPTY;
        if (stack == null) return ItemStack.EMPTY;
        if (!stack.isEmpty())
            return stack.copyWithCount(
                    Math.min(stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 64), amount));
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
