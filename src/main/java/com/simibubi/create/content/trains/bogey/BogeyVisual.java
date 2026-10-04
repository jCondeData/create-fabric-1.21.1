package com.simibubi.create.content.trains.bogey;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.api.instance.Instance;

import net.minecraft.nbt.CompoundTag;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface BogeyVisual {
    void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack);

    void hide();

    void updateLight(int packedLight);

    void collectCrumblingInstances(Consumer<@Nullable Instance> consumer);

    void delete();
}
