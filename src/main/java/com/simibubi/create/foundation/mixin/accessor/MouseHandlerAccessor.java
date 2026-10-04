package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void create$setXPos(double xPos);

    @Accessor("ypos")
    void create$setYPos(double yPos);
}
