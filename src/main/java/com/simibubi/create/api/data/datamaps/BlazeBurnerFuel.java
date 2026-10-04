package com.simibubi.create.api.data.datamaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.registry.CreateDataMaps;

import net.minecraft.util.ExtraCodecs;

/**
 * Data map value for {@linkplain CreateDataMaps#REGULAR_BLAZE_BURNER_FUELS regular} and {@linkplain
 * CreateDataMaps#SUPERHEATED_BLAZE_BURNER_FUELS superheated} blaze burner fuels.
 *
 * @param burnTime how long (in ticks) the item will burn for
 */
public record BlazeBurnerFuel(int burnTime) {
    public static final Codec<BlazeBurnerFuel> BURN_TIME_CODEC =
            ExtraCodecs.POSITIVE_INT.xmap(BlazeBurnerFuel::new, BlazeBurnerFuel::burnTime);
    public static final Codec<BlazeBurnerFuel> CODEC =
            Codec.withAlternative(
                    RecordCodecBuilder.create(
                            i ->
                                    i.group(
                                                    ExtraCodecs.POSITIVE_INT
                                                            .fieldOf("burn_time")
                                                            .forGetter(BlazeBurnerFuel::burnTime))
                                            .apply(i, BlazeBurnerFuel::new)),
                    BURN_TIME_CODEC);
}
