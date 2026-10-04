package com.simibubi.create.compat.rei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ItemIcon implements Renderer {
    private final Supplier<ItemStack> supplier;
    private ItemStack stack;

    public ItemIcon(Supplier<ItemStack> stack) {
        this.supplier = stack;
    }

    @Override
    public void render(
            GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
        PoseStack matrixStack = graphics.pose();
        if (stack == null) {
            stack = supplier.get();
        }

        RenderSystem.enableDepthTest();
        matrixStack.pushPose();
        matrixStack.translate(bounds.getCenterX() - 8, bounds.getCenterY() - 8, 0);
        GuiGameElement.of(stack).render(graphics);
        matrixStack.popPose();
    }
}
