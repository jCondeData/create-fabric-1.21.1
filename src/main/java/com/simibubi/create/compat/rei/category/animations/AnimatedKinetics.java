package com.simibubi.create.compat.rei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.CustomLightingSettings;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.ILightingSettings;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** REI copy of the JEI animation base; draws at an offset relative to the category origin. */
public abstract class AnimatedKinetics {

    public int offset = 0;

    public static final ILightingSettings DEFAULT_LIGHTING =
            CustomLightingSettings.builder()
                    .firstLightRotation(12.5f, -45.0f)
                    .secondLightRotation(-20.0f, -50.0f)
                    .build();

    /**
     * <b>Only use this method outside of subclasses.</b> Use {@link #blockElement(BlockState)} if
     * calling from inside a subclass.
     */
    public static GuiGameElement.GuiRenderBuilder defaultBlockElement(BlockState state) {
        return GuiGameElement.of(state).lighting(DEFAULT_LIGHTING);
    }

    /**
     * <b>Only use this method outside of subclasses.</b> Use {@link #blockElement(PartialModel)} if
     * calling from inside a subclass.
     */
    public static GuiGameElement.GuiRenderBuilder defaultBlockElement(PartialModel partial) {
        return GuiGameElement.of(partial).lighting(DEFAULT_LIGHTING);
    }

    public static float getCurrentAngle() {
        return (AnimationTickHolder.getRenderTime() * 4f) % 360;
    }

    protected BlockState shaft(Axis axis) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
    }

    protected PartialModel cogwheel() {
        return AllPartialModels.SHAFTLESS_COGWHEEL;
    }

    protected GuiGameElement.GuiRenderBuilder blockElement(BlockState state) {
        return defaultBlockElement(state);
    }

    protected GuiGameElement.GuiRenderBuilder blockElement(PartialModel partial) {
        return defaultBlockElement(partial);
    }

    public int getWidth() {
        return 50;
    }

    public int getHeight() {
        return 50;
    }

    public abstract void draw(GuiGraphics graphics, int xOffset, int yOffset);
}
