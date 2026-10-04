package com.simibubi.create.content.logistics.packager.repackager;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.crate.BottomlessItemHandler;
import com.simibubi.create.content.logistics.packager.PackageDefragmenter;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import com.simibubi.create.content.logistics.packager.PackagingRequest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

public class RepackagerBlockEntity extends PackagerBlockEntity {

    public PackageDefragmenter defragmenter;

    public RepackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        defragmenter = new PackageDefragmenter();
    }

    public boolean unwrapBox(ItemStack box, boolean simulate) {
        if (animationTicks > 0) return false;

        IItemHandler targetInv = targetInventory.getInventory();
        if (targetInv == null || targetInv instanceof PackagerItemHandler) return false;

        boolean targetIsCreativeCrate = targetInv instanceof BottomlessItemHandler;
        boolean anySpace = false;

        for (int slot = 0; slot < targetInv.getSlots(); slot++) {
            ItemStack remainder = targetInv.insertItem(slot, box, simulate);
            if (!remainder.isEmpty()) continue;
            anySpace = true;
            break;
        }

        if (!targetIsCreativeCrate && !anySpace) return false;
        if (simulate) return true;

        previouslyUnwrapped = box;
        animationInward = true;
        animationTicks = CYCLE;
        notifyUpdate();
        return true;
    }

    @Override
    public void recheckIfLinksPresent() {}

    @Override
    public boolean redstoneModeActive() {
        return true;
    }

    public void attemptToSend(List<PackagingRequest> queuedRequests) {
        if (queuedRequests == null && (!heldBox.isEmpty() || animationTicks != 0)) return;

        IItemHandler targetInv = targetInventory.getInventory();
        if (targetInv == null || targetInv instanceof PackagerItemHandler) return;

        attemptToDefrag(targetInv);
        if (heldBox.isEmpty()) return;

        updateSignAddress();
        if (!signBasedAddress.isBlank()) PackageItem.addAddress(heldBox, signBasedAddress);
    }

    protected void attemptToDefrag(IItemHandler targetInv) {
        defragmenter.clear();
        int completedOrderId = -1;

        for (int slot = 0; slot < targetInv.getSlots(); slot++) {
            ItemStack extracted = targetInv.extractItem(slot, 1, true);
            if (extracted.isEmpty() || !PackageItem.isPackage(extracted)) continue;

            if (!defragmenter.isFragmented(extracted)) {
                targetInv.extractItem(slot, 1, false);
                heldBox = extracted.copy();
                animationInward = false;
                animationTicks = CYCLE;
                notifyUpdate();
                return;
            }

            completedOrderId = defragmenter.addPackageFragment(extracted);
            if (completedOrderId != -1) break;
        }

        if (completedOrderId == -1) return;

        List<ItemStack> boxesToExport = defragmenter.repack(completedOrderId);

        for (int slot = 0; slot < targetInv.getSlots(); slot++) {
            ItemStack extracted = targetInv.extractItem(slot, 1, true);
            if (extracted.isEmpty() || !PackageItem.isPackage(extracted)) continue;
            if (PackageItem.getOrderId(extracted) != completedOrderId) continue;
            targetInv.extractItem(slot, 1, false);
        }

        if (boxesToExport.isEmpty()) return;

        heldBox = boxesToExport.get(0).copy();
        animationInward = false;
        animationTicks = CYCLE;

        for (int i = 1; i < boxesToExport.size(); i++)
            ItemHandlerHelper.insertItem(targetInv, boxesToExport.get(i), false);

        notifyUpdate();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AllBlockEntityTypes.REPACKAGER.get(),
                (be, context) -> be.inventory);
    }
}
