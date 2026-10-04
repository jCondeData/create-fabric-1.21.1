package com.simibubi.create.foundation.block;

import com.google.common.collect.HashBiMap;

import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.Map;

@ApiStatus.Internal
public class CopperRegistries {
    private static final Map<Holder<Block>, Holder<Block>> WEATHERING = HashBiMap.create();
    private static final Map<Holder<Block>, Holder<Block>> WAXABLE = HashBiMap.create();

    public static Map<Holder<Block>, Holder<Block>> getWeatheringView() {
        return Collections.unmodifiableMap(WEATHERING);
    }

    public static Map<Holder<Block>, Holder<Block>> getWaxableView() {
        return Collections.unmodifiableMap(WAXABLE);
    }

    public static synchronized void addWeathering(Holder<Block> original, Holder<Block> weathered) {
        WEATHERING.put(original, weathered);
    }

    public static synchronized void addWaxable(Holder<Block> original, Holder<Block> waxed) {
        WAXABLE.put(original, waxed);
    }

    /**
     * fabric: replaces NeoForge's OXIDIZABLES/WAXABLES data maps (and their datagen provider). Must
     * run after block registration, see {@code Create#onInitialize}.
     */
    public static void inject() {
        WEATHERING.forEach(
                (now, after) ->
                        OxidizableBlocksRegistry.registerOxidizableBlockPair(
                                now.value(), after.value()));
        WAXABLE.forEach(
                (now, after) ->
                        OxidizableBlocksRegistry.registerWaxableBlockPair(
                                now.value(), after.value()));
    }
}
