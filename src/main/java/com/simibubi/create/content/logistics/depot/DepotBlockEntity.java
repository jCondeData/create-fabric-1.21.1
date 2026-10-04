package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DepotBlockEntity extends SmartBlockEntity
        implements SidedStorageBlockEntity, Clearable {
    DepotBehaviour depotBehaviour;

    public DepotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(depotBehaviour = new DepotBehaviour(this));
        depotBehaviour.addSubBehaviours(behaviours);
    }

    @Nullable
    @Override
    public Storage<ItemVariant> getItemStorage(@Nullable Direction direction) {
        return depotBehaviour.itemHandler;
    }

    @Override
    public void clearContent() {
        depotBehaviour.clearContent();
    }

    public ItemStack getHeldItem() {
        return depotBehaviour.getHeldItemStack();
    }

    public void setHeldItem(ItemStack item) {
        TransportedItemStack newStack = new TransportedItemStack(item);
        if (depotBehaviour.heldItem != null) newStack.angle = depotBehaviour.heldItem.angle;
        depotBehaviour.setHeldItem(newStack);
    }
}
