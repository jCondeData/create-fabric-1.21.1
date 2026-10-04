package com.simibubi.create.compat.rei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.animations.AnimatedCrafter;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MechanicalCraftingCategory extends CreateRecipeCategory<CraftingRecipe> {

    private final AnimatedCrafter crafter = new AnimatedCrafter();

    public MechanicalCraftingCategory(Info<CraftingRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(RecipeLayout builder, CraftingRecipe recipe) {
        builder.addSlot(Role.OUTPUT, 134, 81).addItemStack(getResultItem(recipe));

        int x = getXPadding(recipe);
        int y = getYPadding(recipe);
        float scale = getScale(recipe);
        // JEI uses a custom ingredient renderer for the scaled grid; REI scales entries to the
        // slot bounds, so the slots just get the scaled size
        int size = (int) (16 * scale);
        int i = 0;

        for (Ingredient ingredient : recipe.getIngredients()) {
            float f = 19 * scale;
            int xPosition = (int) (x + 1 + (i % getWidth(recipe)) * f);
            int yPosition = (int) (y + 1 + (i / getWidth(recipe)) * f);

            builder.addSlot(Role.INPUT, xPosition, yPosition)
                    .setSize(size, size)
                    .addIngredients(ingredient);

            i++;
        }
    }

    static int maxSize = 100;

    public static float getScale(CraftingRecipe recipe) {
        int w = getWidth(recipe);
        int h = getHeight(recipe);
        return Math.min(1, maxSize / (19f * Math.max(w, h)));
    }

    public static int getYPadding(CraftingRecipe recipe) {
        return 3 + 50 - (int) (getScale(recipe) * getHeight(recipe) * 19 * .5);
    }

    public static int getXPadding(CraftingRecipe recipe) {
        return 3 + 50 - (int) (getScale(recipe) * getWidth(recipe) * 19 * .5);
    }

    private static int getWidth(CraftingRecipe recipe) {
        return recipe instanceof ShapedRecipe ? ((ShapedRecipe) recipe).getWidth() : 1;
    }

    private static int getHeight(CraftingRecipe recipe) {
        return recipe instanceof ShapedRecipe ? ((ShapedRecipe) recipe).getHeight() : 1;
    }

    @Override
    public void draw(CraftingRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        float scale = getScale(recipe);
        matrixStack.translate(getXPadding(recipe), getYPadding(recipe), 0);

        for (int row = 0; row < getHeight(recipe); row++)
            for (int col = 0; col < getWidth(recipe); col++) {
                int pIndex = row * getWidth(recipe) + col;
                if (pIndex >= recipe.getIngredients().size()) break;
                if (recipe.getIngredients().get(pIndex).isEmpty()) continue;
                matrixStack.pushPose();
                matrixStack.translate(col * 19 * scale, row * 19 * scale, 0);
                matrixStack.scale(scale, scale, scale);
                AllGuiTextures.JEI_SLOT.render(graphics, 0, 0);
                matrixStack.popPose();
            }

        matrixStack.popPose();

        AllGuiTextures.JEI_SLOT.render(graphics, 133, 80);
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 128, 59);
        crafter.draw(graphics, 129, 25);

        matrixStack.pushPose();
        matrixStack.translate(0, 0, 300);

        int amount = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (Ingredient.EMPTY == ingredient) continue;
            amount++;
        }

        graphics.drawString(Minecraft.getInstance().font, amount + "", 142, 39, 0xFFFFFF);
        matrixStack.popPose();
    }
}
