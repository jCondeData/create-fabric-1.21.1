package com.simibubi.create.infrastructure.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.data.DamageTypeTagGen;
import com.simibubi.create.foundation.data.TagLangGen;
import com.simibubi.create.foundation.data.recipe.CreateMechanicalCraftingRecipeGen;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import com.simibubi.create.foundation.data.recipe.CreateSequencedAssemblyRecipeGen;
import com.simibubi.create.foundation.data.recipe.CreateStandardRecipeGen;
import com.simibubi.create.foundation.mixin.accessor.LootTableProviderSubProvidersAccessor;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;

import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;

import net.createmod.ponder.foundation.PonderIndex;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

public class CreateDatagen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        ExistingFileHelper helper = ExistingFileHelper.withResourcesFromArg();
        // Registrate запрещает addDataGenerator после setupDatagen — дополнительные генераторы
        // раньше
        addExtraRegistrateData();
        TagLangGen.datagen(); // тоже addDataGenerator
        FabricDataGenerator.Pack pack = generator.createPack();
        // fabric: upstream 6.0.4 stopped touching Registrate internals (setDataProvider /
        // AbstractRegistrateAccessor are gone), so use the public setupDatagen. The pack builds the
        // RegistrateDataProvider immediately, so its sub-providers can be adjusted right after.
        Create.registrate().setupDatagen(pack, helper);
        // У Registrate-Fabric 1.3.77 лут-провайдер сломан — в LootTableProvider попадают ванильные
        // сабпровайдеры, а RegistrateBlockLootTables не переопределяет getKnownBlocks, так что
        // валидация требует таблиц для всего реестра. ponytail: лут-таблицы не регенерируем (лежат
        // в src/generated), вернуть, когда Registrate-Fabric починит getKnownBlocks.
        Create.registrate()
                .getDataProvider(ProviderType.LOOT)
                .ifPresent(
                        loot ->
                                ((LootTableProviderSubProvidersAccessor) loot)
                                        .create$setSubProviders(List.of()));
        gatherData(pack, helper);
    }

    public static void gatherData(
            FabricDataGenerator.Pack pack, ExistingFileHelper existingFileHelper) {

        // fabric: pretty much redone, make sure all providers make it through merges

        pack.addProvider(CreateRecipeSerializerTagsProvider::new);
        pack.addProvider(CreateContraptionTypeTagsProvider::new);
        pack.addProvider(CreateMountedItemStorageTypeTagsProvider::new);
        pack.addProvider(CreateEnchantmentTagsProvider::new);
        pack.addProvider(DamageTypeTagGen::new);
        pack.addProvider(AllAdvancements::new);
        pack.addProvider(CreateStandardRecipeGen::new);
        pack.addProvider(CreateMechanicalCraftingRecipeGen::new);
        pack.addProvider(CreateSequencedAssemblyRecipeGen::new);
        // процессинговые рецепты
        CreateRecipeProvider.registerAllProcessing(pack);
        pack.addProvider(GeneratedEntriesProvider::new);
        pack.addProvider(VanillaHatOffsetGenerator::new);

        // fabric: no equivalent to NeoForge data maps or Curios, both providers were dropped
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        GeneratedEntriesProvider.addBootstraps(registryBuilder);
    }

    private static void addExtraRegistrateData() {
        CreateRegistrateTags.addGenerators();

        Create.registrate()
                .addDataGenerator(
                        ProviderType.LANG,
                        provider -> {
                            BiConsumer<String, String> langConsumer = provider::add;

                            provideDefaultLang("interface", langConsumer);
                            provideDefaultLang("tooltips", langConsumer);
                            AllAdvancements.provideLang(langConsumer);
                            AllSoundEvents.provideLang(langConsumer);
                            AllKeys.provideLang(langConsumer);
                            providePonderLang(langConsumer);
                        });
    }

    private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/create/lang/default/" + fileName + ".json";
        JsonElement jsonElement = FilesHelper.loadJsonResource(path);
        if (jsonElement == null) {
            throw new IllegalStateException(
                    String.format("Could not find default lang file: %s", path));
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            consumer.accept(key, value);
        }
    }

    private static void providePonderLang(BiConsumer<String, String> consumer) {
        // Register this since FMLClientSetupEvent does not run during datagen
        PonderIndex.addPlugin(new CreatePonderPlugin());

        PonderIndex.getLangAccess().provideLang(Create.ID, consumer);
    }
}
