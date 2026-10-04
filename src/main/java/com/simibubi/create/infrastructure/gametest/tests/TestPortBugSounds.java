package com.simibubi.create.infrastructure.gametest.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.resources.ResourceLocation;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BUG (fails on 3ec980de36): the mod ships Create's .ogg files but no assets/create/sounds.json, so
 * every Create sound event (wrench, mixer, steam engine, train whistles, stock ticker, ...) is
 * silent; the client logs "Missing sound for event: create:..." 79 times. Upstream generates
 * sounds.json with AllSoundEvents.provider in its datagen; the Fabric CreateDatagen never adds it.
 */
@GameTestGroup(path = "qa")
public class TestPortBugSounds {

    @GameTest(template = "flat_7x6x7")
    public static void everyCreateSoundEventIsDefinedInSoundsJson(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> {
                    Optional<Path> file =
                            FabricLoader.getInstance()
                                    .getModContainer(Create.ID)
                                    .orElseThrow()
                                    .findPath("assets/create/sounds.json");
                    helper.assertTrue(
                            file.isPresent(),
                            "assets/create/sounds.json is missing: every Create sound is silent");
                    JsonObject sounds;
                    try (Reader reader = Files.newBufferedReader(file.get())) {
                        sounds = JsonParser.parseReader(reader).getAsJsonObject();
                    } catch (Exception e) {
                        helper.fail("sounds.json unreadable: " + e);
                        return;
                    }
                    List<String> missing = new ArrayList<>();
                    for (ResourceLocation id : BuiltInRegistries.SOUND_EVENT.keySet())
                        if (id.getNamespace().equals(Create.ID) && !sounds.has(id.getPath()))
                            missing.add(id.getPath());
                    helper.assertTrue(
                            missing.isEmpty(),
                            missing.size() + " Create sound events have no sounds: " + missing);
                    helper.succeed();
                });
    }
}
