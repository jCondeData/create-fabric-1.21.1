package com.simibubi.create.foundation.data;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import com.simibubi.create.api.registry.CreateDataMaps;

import io.github.fabricators_of_create.porting_lib.data.DataMapProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

/**
 * fabric: built on Porting Lib's data maps. Upstream generates NeoForge's {@code oxidizables} /
 * {@code waxables} here, which mean nothing on Fabric (copper pairs are registered at runtime with
 * Fabric's {@code OxidizableBlocksRegistry}, see {@code CopperRegistries#inject}). Instead this
 * provider generates Create's own blaze burner fuel data maps, which upstream generates through
 * Registrate's {@code ItemBuilder#dataMap} (missing from Registrate-Fabric).
 */
public class CreateDatamapProvider extends DataMapProvider {
    public CreateDatamapProvider(
            PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(Provider provider) {
        final Builder<BlazeBurnerFuel, Item> superheated =
                builder(CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS);
        superheated.add(AllItems.BLAZE_CAKE.getKey(), new BlazeBurnerFuel(3200), false);
    }
}
