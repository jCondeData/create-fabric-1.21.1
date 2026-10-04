package com.simibubi.create.foundation.mixin.client;

import com.simibubi.create.content.trains.CameraDistanceModifier;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(
            method =
                    "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"),
            index = 0)
    private float create$modifyCameraOffset(float originalValue) {
        return originalValue * CameraDistanceModifier.getMultiplier();
    }

    // fabric: replaces NeoForge's ViewportEvent.ComputeCameraAngles / porting lib's removed
    // CameraSetupCallback
    @ModifyArgs(
            method =
                    "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                            ordinal = 0))
    private void create$animateCameraAngles(Args args) {
        float partialTicks = AnimationTickHolder.getPartialTicks();
        if (CameraAngleAnimationService.isYawAnimating())
            args.set(0, CameraAngleAnimationService.getYaw(partialTicks));
        if (CameraAngleAnimationService.isPitchAnimating())
            args.set(1, CameraAngleAnimationService.getPitch(partialTicks));
    }
}
