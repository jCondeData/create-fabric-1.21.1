package com.simibubi.create.compat.rei;

import com.simibubi.create.content.fluids.potion.PotionFluidHandler;

import dev.architectury.fluid.FluidStack;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.entry.renderer.ForwardingEntryRenderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.entry.EntryStack;

import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws Create's potion fluid with its real color, name and effects. REI asks Architectury for
 * those, and Architectury 13's Fabric {@code FluidStack#getPatch()} drops the components of every
 * non-empty stack, so without this every potion fluid shows up as an "Uncraftable Potion".
 */
public class PotionFluidEntryRenderer extends ForwardingEntryRenderer<FluidStack> {

    public PotionFluidEntryRenderer(EntryRenderer<FluidStack> next) {
        super(next);
    }

    /** The stack as a Fabric variant, components included. */
    public static FluidVariant toVariant(FluidStack stack) {
        return FluidVariant.of(stack.getFluid(), stack.getComponents().asPatch());
    }

    @Override
    public void render(
            EntryStack<FluidStack> entry,
            GuiGraphics graphics,
            Rectangle bounds,
            int mouseX,
            int mouseY,
            float delta) {
        FluidStack stack = entry.getValue();
        if (stack.isEmpty()) return;

        FluidVariant variant = toVariant(stack);
        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(variant);
        if (sprite == null) {
            super.render(entry, graphics, bounds, mouseX, mouseY, delta);
            return;
        }

        float ratio = entry.get(EntryStack.Settings.FLUID_RENDER_RATIO);
        int height = Math.max(1, Math.round(bounds.height * Math.min(1, ratio)));
        int color = FluidVariantRendering.getColor(variant);
        graphics.blit(
                bounds.x,
                bounds.getMaxY() - height,
                0,
                bounds.width,
                height,
                sprite,
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                1f);
    }

    @Override
    @Nullable
    public Tooltip getTooltip(EntryStack<FluidStack> entry, TooltipContext context) {
        Tooltip tooltip = super.getTooltip(entry, context);
        if (tooltip == null || entry.getValue().isEmpty()) return tooltip;

        FluidVariant variant = toVariant(entry.getValue());
        List<Tooltip.Entry> entries = tooltip.entries();
        Component name = FluidVariantAttributes.getName(variant);
        if (entries.isEmpty()) entries.add(Tooltip.entry(name));
        else entries.set(0, Tooltip.entry(name));

        List<Component> effects = new ArrayList<>();
        PotionFluidHandler.addPotionTooltip(variant, effects::add, 1);
        int index = 1;
        for (Component line : effects) entries.add(index++, Tooltip.entry(line));
        return tooltip;
    }
}
