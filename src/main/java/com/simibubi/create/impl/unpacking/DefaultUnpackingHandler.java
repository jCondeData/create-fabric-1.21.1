package com.simibubi.create.impl.unpacking;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum DefaultUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(
            Level level,
            BlockPos pos,
            BlockState state,
            Direction side,
            List<ItemStack> items,
            @Nullable PackageOrderWithCrafts orderContext,
            boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(pos);

        Storage<ItemVariant> targetInv = ItemStorage.SIDED.find(level, pos, state, targetBE, side);
        if (targetInv == null) return false;

        // fabric: packagers unpack while the transaction that inserted the box is still open, where
        // Transaction.openOuter() would throw. Nest into that transaction instead: a simulated run
        // is always rolled back, a real run commits into the caller's transaction and is undone
        // with it. No transfer operations are allowed from a close callback, so refuse there.
        Transaction.Lifecycle lifecycle = Transaction.getLifecycle();
        if (lifecycle != Transaction.Lifecycle.NONE && lifecycle != Transaction.Lifecycle.OPEN)
            return false;

        try (Transaction t = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            for (ItemStack stack : items) {
                long inserted = targetInv.insert(ItemVariant.of(stack), stack.getCount(), t);
                if (inserted != stack.getCount()) {
                    return false;
                }
            }

            if (!simulate) {
                t.commit();
            }
        }

        return true;
    }
}
