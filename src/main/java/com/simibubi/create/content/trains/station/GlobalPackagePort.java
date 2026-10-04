package com.simibubi.create.content.trains.station;

import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.fabric.transfer.item.ItemStackHandler;
import com.simibubi.create.infrastructure.fabric.transfer.item.SlottedStackStorage;

public class GlobalPackagePort {
    public String address = "";
    public ItemStackHandler offlineBuffer = new ItemStackHandler(18);
    public boolean primed = false;
    private boolean restoring = false;

    // fabric: stacks are copied between the handlers, the transfer-backed ItemStackHandler indexes
    // its slots by content and transfer extraction shrinks stacks in place
    public void restoreOfflineBuffer(SlottedStackStorage inventory) {
        if (!primed) return;

        restoring = true;

        for (int slot = 0; slot < offlineBuffer.getSlotCount(); slot++) {
            inventory.setStackInSlot(slot, offlineBuffer.getStackInSlot(slot).copy());
        }

        restoring = false;
        primed = false;
    }

    public void saveOfflineBuffer(SlottedStackStorage inventory) {
        /*
         * Each time restoreOfflineBuffer changes a slot, the inventory
         * calls this method. We must filter out those calls to prevent
         * overwriting later slots which haven't been restored yet and
         * to avoid unnecessary work.
         */
        if (restoring) return;

        // TODO: Call save method on individual slots rather than iterating
        for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
            offlineBuffer.setStackInSlot(slot, inventory.getStackInSlot(slot).copy());
        }

        Create.RAILWAYS.markTracksDirty();
    }
}
