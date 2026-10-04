package com.simibubi.create.content.logistics.packager;

import com.simibubi.create.api.packager.InventoryIdentifier;

import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

/** An item inventory, possibly with an associated InventoryIdentifier. */
public record IdentifiedInventory(@Nullable InventoryIdentifier identifier, IItemHandler handler) {}
