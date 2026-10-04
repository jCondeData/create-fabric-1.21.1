package com.simibubi.create.infrastructure.fabric.transfer.fluid;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

/** Mutable combination of a fluid and an amount, paralleling {@link ItemStack}. */
public final class FluidStack implements DataComponentHolder {
    private static final Logger logger = LogUtils.getLogger();

    public static final FluidStack EMPTY = new FluidStack(FluidVariant.blank(), 0);

    private static final Codec<FluidStack> FABRIC_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            FluidVariant.CODEC
                                                    .fieldOf("fluid")
                                                    .forGetter(FluidStack::getVariant),
                                            Codec.LONG
                                                    .fieldOf("amount")
                                                    .forGetter(FluidStack::getAmount))
                                    .apply(i, FluidStack::new));

    /**
     * Read-only fallback for NeoForge's FluidStack format ({@code {"id", "amount" (mB),
     * "components"}}), as found in upstream Create's gametest structures and in schematics saved on
     * NeoForge. Amounts are converted from millibuckets to droplets.
     */
    private static final Codec<FluidStack> NEOFORGE_CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BuiltInRegistries.FLUID
                                                    .byNameCodec()
                                                    .fieldOf("id")
                                                    .forGetter(s -> s.getVariant().getFluid()),
                                            Codec.INT
                                                    .fieldOf("amount")
                                                    .forGetter(
                                                            s ->
                                                                    (int)
                                                                            (s.getAmount()
                                                                                    / (FluidConstants
                                                                                                    .BUCKET
                                                                                            / 1000))),
                                            DataComponentPatch.CODEC
                                                    .optionalFieldOf(
                                                            "components", DataComponentPatch.EMPTY)
                                                    .forGetter(s -> s.getVariant().getComponents()))
                                    .apply(
                                            i,
                                            (fluid, amount, components) ->
                                                    new FluidStack(
                                                            FluidVariant.of(fluid, components),
                                                            amount
                                                                    * (FluidConstants.BUCKET
                                                                            / 1000))));

    /** Writes the Fabric format; also reads NeoForge's. */
    public static final Codec<FluidStack> CODEC =
            Codec.withAlternative(FABRIC_CODEC, NEOFORGE_CODEC);

    /**
     * An empty compound tag decodes to {@link #EMPTY}, mirroring {@code ItemStack.OPTIONAL_CODEC}.
     */
    public static final Codec<FluidStack> OPTIONAL_CODEC =
            ExtraCodecs.optionalEmptyMap(CODEC)
                    .xmap(
                            optional -> optional.orElse(EMPTY),
                            stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public FluidStack decode(RegistryFriendlyByteBuf buf) {
                    long amount = buf.readVarLong();
                    return amount <= 0
                            ? EMPTY
                            : new FluidStack(FluidVariant.PACKET_CODEC.decode(buf), amount);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, FluidStack stack) {
                    if (stack.isEmpty()) {
                        buf.writeVarLong(0);
                        return;
                    }
                    buf.writeVarLong(stack.getAmount());
                    FluidVariant.PACKET_CODEC.encode(buf, stack.getVariant());
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC =
            OPTIONAL_STREAM_CODEC;

    private final FluidVariant variant;
    private long amount;

    public FluidStack(FluidVariant variant, long amount) {
        this.variant = variant;
        this.setAmount(amount);
    }

    public FluidStack(Fluid fluid, long amount) {
        this(FluidVariant.of(fluid), amount);
    }

    public FluidStack(Holder<Fluid> fluid, long amount, DataComponentPatch components) {
        this(FluidVariant.of(fluid.value(), components), amount);
    }

    public FluidStack(StorageView<FluidVariant> view) {
        this(view.getResource(), view.getAmount());
    }

    public FluidStack(ResourceAmount<FluidVariant> resource) {
        this(resource.resource(), resource.amount());
    }

    public FluidVariant getVariant() {
        return this.variant;
    }

    public Fluid getFluid() {
        return this.variant.getFluid();
    }

    @Override
    public DataComponentMap getComponents() {
        return !this.isEmpty() ? this.variant.getComponentMap() : DataComponentMap.EMPTY;
    }

    public DataComponentPatch getComponentsPatch() {
        return !this.isEmpty() ? this.variant.getComponents() : DataComponentPatch.EMPTY;
    }

    public long getAmount() {
        return this.amount;
    }

    public void setAmount(long amount) {
        this.amount = Math.max(amount, 0);
    }

    public void shrink(long amount) {
        this.setAmount(this.amount - amount);
    }

    public Component getHoverName() {
        return FluidVariantAttributes.getName(this.variant);
    }

    public boolean isEmpty() {
        return this.variant.isBlank() || this.amount <= 0;
    }

    public FluidStack copy() {
        return this.isEmpty() ? EMPTY : new FluidStack(this.variant, this.amount);
    }

    public FluidStack copyWithAmount(long amount) {
        FluidStack copy = this.copy();
        if (!copy.isEmpty()) {
            copy.setAmount(amount);
        }
        return copy;
    }

    public Tag save(Provider registries, Tag output) {
        if (this.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty FluidStack");
        } else {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.encode(this, ops, output).getOrThrow();
        }
    }

    public Tag save(Provider registries) {
        if (this.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty FluidStack");
        } else {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.encodeStart(ops, this).getOrThrow();
        }
    }

    public Tag saveOptional(Provider registries) {
        return this.isEmpty() ? new CompoundTag() : this.save(registries, new CompoundTag());
    }

    /**
     * {@link FluidVariant} is immutable, so component edits return a new stack instead of mutating
     * in place like NeoForge's {@code FluidStack#set}.
     */
    public <T> FluidStack with(DataComponentType<T> type, @Nullable T value) {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry :
                this.getComponentsPatch().entrySet()) {
            if (entry.getKey() == type) continue;
            copyEntry(builder, entry);
        }
        if (value != null) builder.set(type, value);
        else builder.remove(type);
        return new FluidStack(FluidVariant.of(this.getFluid(), builder.build()), this.amount);
    }

    public FluidStack without(DataComponentType<?> type) {
        return with(cast(type), null);
    }

    @SuppressWarnings("unchecked")
    private static <T> DataComponentType<T> cast(DataComponentType<?> type) {
        return (DataComponentType<T>) type;
    }

    @SuppressWarnings("unchecked")
    private static <T> void copyEntry(
            DataComponentPatch.Builder builder,
            Map.Entry<DataComponentType<?>, Optional<?>> entry) {
        DataComponentType<T> type = (DataComponentType<T>) entry.getKey();
        Optional<T> value = (Optional<T>) entry.getValue();
        if (value.isPresent()) builder.set(type, value.get());
        else builder.remove(type);
    }

    /** Whether this stack would accept the given variant, i.e. it is empty or already holds it. */
    public boolean canFill(FluidVariant other) {
        return this.isEmpty() || this.variant.equals(other);
    }

    public boolean isFluidEqual(FluidStack other) {
        return isSameFluidSameComponents(this, other);
    }

    public boolean isComponentsPatchEmpty() {
        return !this.variant.hasComponents();
    }

    public static boolean isSameFluidSameComponents(FluidStack first, FluidStack second) {
        if (!first.variant.isOf(second.variant.getFluid())) return false;

        return first.variant.componentsMatch(second.variant.getComponents());
    }

    public static Optional<FluidStack> parse(HolderLookup.Provider registries, Tag tag) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return CODEC.parse(ops, tag)
                .resultOrPartial(error -> logger.error("Failed to read invalid fluid: {}", error));
    }

    public static FluidStack parseOptional(HolderLookup.Provider registries, CompoundTag tag) {
        return tag.isEmpty() ? EMPTY : parse(registries, tag).orElse(EMPTY);
    }

    public static FluidStack of(@Nullable ResourceAmount<FluidVariant> resource) {
        return resource == null ? EMPTY : new FluidStack(resource);
    }
}
