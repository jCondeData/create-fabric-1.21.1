package com.simibubi.create.content.processing.recipe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;

/** Проверка обеих форм результата: {@code ./gradlew codecCheck}. */
public class ProcessingOutputCodecCheck {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        ProcessingOutput bare = parse("{\"count\":8,\"id\":\"minecraft:stone\"}");
        if (bare.getStack().getCount() != 8)
            throw new AssertionError("bare count " + bare.getStack().getCount());

        ProcessingOutput field = parse("{\"item\":{\"id\":\"minecraft:stone\"},\"count\":8}");
        if (field.getStack().getCount() != 8)
            throw new AssertionError("field count " + field.getStack().getCount());

        if (bare.getStack().getItem() != Items.STONE || field.getStack().getItem() != Items.STONE)
            throw new AssertionError("result item must survive both codec forms");
        ProcessingOutput defaultCount = parse("{\"item\":{\"id\":\"minecraft:stone\"}}");
        if (defaultCount.getStack().getCount() != 1 || defaultCount.getChance() != 1)
            throw new AssertionError("omitted count and chance must default to one");
        ProcessingOutput probabilistic =
                parse("{\"item\":{\"id\":\"minecraft:stone\"},\"count\":3,\"chance\":0.25}");
        String probabilisticJson =
                ProcessingOutput.CODEC
                        .encodeStart(JsonOps.INSTANCE, probabilistic)
                        .getOrThrow()
                        .toString();
        ProcessingOutput roundTrip = parse(probabilisticJson);
        if (roundTrip.getStack().getCount() != 3 || roundTrip.getChance() != 0.25f)
            throw new AssertionError("round-trip must preserve count and chance");
        if (ProcessingOutput.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{}"))
                .error()
                .isEmpty()) throw new AssertionError("missing item must be rejected");

        String encoded =
                ProcessingOutput.CODEC.encodeStart(JsonOps.INSTANCE, bare).getOrThrow().toString();
        // Upstream 6.0.4 switched the written form to {"id": ..., "count": ...}; the old
        // {"item": {...}} form is still accepted when reading (checked above).
        if (!encoded.startsWith("{\"id\":\"minecraft:stone\""))
            throw new AssertionError("encoded not in 6.0.4 id form: " + encoded);

        System.out.println("OK encoded=" + encoded);
    }

    private static ProcessingOutput parse(String json) {
        return ProcessingOutput.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow();
    }
}
