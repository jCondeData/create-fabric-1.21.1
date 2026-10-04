package com.simibubi.create.content.logistics.packager;

import com.simibubi.create.api.packager.InventoryIdentifier;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

import org.jetbrains.annotations.Nullable;

/** An item inventory, possibly with an associated InventoryIdentifier. */
public record IdentifiedInventory(
        @Nullable InventoryIdentifier identifier, Storage<ItemVariant> handler) {}
