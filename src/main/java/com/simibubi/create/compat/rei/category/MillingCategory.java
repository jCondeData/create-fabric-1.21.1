package com.simibubi.create.compat.rei.category;

import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.animations.AnimatedMillstone;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MillingCategory extends CreateRecipeCategory<AbstractCrushingRecipe> {

    private final AnimatedMillstone millstone = new AnimatedMillstone();

    public MillingCategory(Info<AbstractCrushingRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(RecipeLayout builder, AbstractCrushingRecipe recipe) {
        builder.addSlot(Role.INPUT, 15, 9)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getIngredients().get(0));

        List<ProcessingOutput> results = recipe.getRollableResults();
        boolean single = results.size() == 1;
        int i = 0;
        for (ProcessingOutput output : results) {
            int xOffset = i % 2 == 0 ? 0 : 19;
            int yOffset = (i / 2) * -19;

            addOutputSlot(builder, single ? 139 : 133 + xOffset, 27 + yOffset, output);

            i++;
        }
    }

    @Override
    public void draw(
            AbstractCrushingRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_ARROW.render(graphics, 85, 32);
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 43, 4);
        millstone.draw(graphics, 48, 27);
    }
}
