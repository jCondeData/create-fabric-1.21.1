package com.simibubi.create.compat.curios;

import com.simibubi.create.Create;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import top.theillusivec4.curios.api.CuriosDataProvider;

import java.util.concurrent.CompletableFuture;

public class CuriosDataGenerator extends CuriosDataProvider {
    public CuriosDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries,
            ExistingFileHelper fileHelper) {
        super(Create.ID, output, fileHelper, registries);
    }

    @Override
    public void generate(Provider registries, ExistingFileHelper fileHelper) {
        createEntities("players").addPlayer().addSlots("head");
    }
}
