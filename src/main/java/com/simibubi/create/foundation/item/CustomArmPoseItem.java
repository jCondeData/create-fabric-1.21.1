package com.simibubi.create.foundation.item;

import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public interface CustomArmPoseItem {
    @Nullable
    ArmPose getArmPose(ItemStack stack, AbstractClientPlayer player, InteractionHand hand);
}
