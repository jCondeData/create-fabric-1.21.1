package com.simibubi.create.compat.rei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class DoubleItemIcon implements Renderer {
    private final Supplier<ItemStack> primarySupplier;
    private final Supplier<ItemStack> secondarySupplier;
    private ItemStack primaryStack;
    private ItemStack secondaryStack;

    public DoubleItemIcon(Supplier<ItemStack> primary, Supplier<ItemStack> secondary) {
        this.primarySupplier = primary;
        this.secondarySupplier = secondary;
    }

    @Override
    public void render(
            GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
        PoseStack matrixStack = graphics.pose();
        if (primaryStack == null) {
            primaryStack = primarySupplier.get();
            secondaryStack = secondarySupplier.get();
        }

        RenderSystem.enableDepthTest();
        matrixStack.pushPose();
        // same 18x18 layout as the JEI icon, centered in the bounds REI gives us
        matrixStack.translate(bounds.getCenterX() - 9, bounds.getCenterY() - 9, 0);

        matrixStack.pushPose();
        matrixStack.translate(1, 1, 0);
        GuiGameElement.of(primaryStack).render(graphics);
        matrixStack.popPose();

        matrixStack.pushPose();
        matrixStack.translate(10, 10, 100);
        matrixStack.scale(.5f, .5f, .5f);
        GuiGameElement.of(secondaryStack).render(graphics);
        matrixStack.popPose();

        matrixStack.popPose();
    }
}
