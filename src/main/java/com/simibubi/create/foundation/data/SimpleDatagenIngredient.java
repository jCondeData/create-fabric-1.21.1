package com.simibubi.create.foundation.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.DatagenMod;
import com.simibubi.create.foundation.data.recipe.Mods;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An ingredient that matches an item of another mod purely by id, so recipes can be generated for
 * items that may not exist in this instance. Replaces the NeoForge {@code ICustomIngredient}
 * version.
 */
public class SimpleDatagenIngredient implements CustomIngredient {

    public static final ResourceLocation ID = Create.asResource("simple_datagen");

    public static final CustomIngredientSerializer<SimpleDatagenIngredient> SERIALIZER =
            new CustomIngredientSerializer<>() {
                private final MapCodec<SimpleDatagenIngredient> codec =
                        RecordCodecBuilder.mapCodec(
                                instance ->
                                        instance.group(
                                                        ResourceLocation.CODEC
                                                                .fieldOf("item")
                                                                .forGetter(
                                                                        SimpleDatagenIngredient
                                                                                ::asResource))
                                                .apply(instance, SimpleDatagenIngredient::of));

                private final StreamCodec<RegistryFriendlyByteBuf, SimpleDatagenIngredient>
                        streamCodec =
                                StreamCodec.of(
                                        (buf, value) ->
                                                buf.writeResourceLocation(value.asResource()),
                                        buf -> of(buf.readResourceLocation()));

                @Override
                public ResourceLocation getIdentifier() {
                    return ID;
                }

                @Override
                public MapCodec<SimpleDatagenIngredient> getCodec(boolean allowEmpty) {
                    return codec;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, SimpleDatagenIngredient>
                        getPacketCodec() {
                    return streamCodec;
                }
            };

    private final DatagenMod mod;
    private final String id;

    public SimpleDatagenIngredient(DatagenMod mod, String id) {
        this.mod = mod;
        this.id = id;
    }

    private static SimpleDatagenIngredient of(ResourceLocation location) {
        for (Mods mod : Mods.values())
            if (mod.getId().equals(location.getNamespace()))
                return new SimpleDatagenIngredient(mod, location.getPath());
        throw new IllegalArgumentException(
                "ID " + location.getNamespace() + " doesn't correspond to any compat mod");
    }

    public ResourceLocation asResource() {
        return mod.asResource(id);
    }

    @Override
    public boolean test(@NotNull ItemStack stack) {
        return asResource().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        return List.of();
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
