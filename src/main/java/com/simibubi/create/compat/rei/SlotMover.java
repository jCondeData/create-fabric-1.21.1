package com.simibubi.create.compat.rei;

import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider;

import java.util.Collection;

/**
 * Allows a {@link AbstractSimiContainerScreen} to specify an area in getExtraArea() that will be
 * avoided by REI
 *
 * <p>Name is taken from CoFHCore's 1.12 implementation.
 */
public class SlotMover implements ExclusionZonesProvider<AbstractSimiContainerScreen<?>> {
    @Override
    public Collection<Rectangle> provide(AbstractSimiContainerScreen<?> containerScreen) {
        return containerScreen.getExtraAreas().stream()
                .map(r -> new Rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight()))
                .toList();
    }
}
