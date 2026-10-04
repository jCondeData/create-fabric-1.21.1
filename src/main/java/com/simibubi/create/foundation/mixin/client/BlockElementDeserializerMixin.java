package com.simibubi.create.foundation.mixin.client;

import net.minecraft.client.renderer.block.model.BlockElement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * fabric: the wide gauge track models (assets/railways/models/block/wide_gauge_base, carried over
 * from Blockfield's port) reach about 22 pixels past the block on each side, beyond vanilla's [-16,
 * 32] element limit, so vanilla refuses to load them. Widen the limit to [-32, 48], as Steam 'n'
 * Rails does for the same models.
 */
@Mixin(BlockElement.Deserializer.class)
public class BlockElementDeserializerMixin {
    @ModifyConstant(
            method = {"getFrom", "getTo"},
            constant = @Constant(floatValue = -16.0F))
    private float create$widenLowerBound(float bound) {
        return -32.0F;
    }

    @ModifyConstant(
            method = {"getFrom", "getTo"},
            constant = @Constant(floatValue = 32.0F))
    private float create$widenUpperBound(float bound) {
        return 48.0F;
    }
}
