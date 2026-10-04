package com.simibubi.create.foundation.mixin.compat.journeymap;

import journeymap.client.render.map.MapRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapRenderer.class)
public interface MapRendererAccessor {
    @Accessor("centerBlockX")
    double create$getCenterBlockX();

    @Accessor("centerBlockZ")
    double create$getCenterBlockZ();
}
