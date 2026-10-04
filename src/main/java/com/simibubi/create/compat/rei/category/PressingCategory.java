package com.simibubi.create.compat.rei.category;

import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.animations.AnimatedPress;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PressingCategory extends CreateRecipeCategory<PressingRecipe> {

    private final AnimatedPress press = new AnimatedPress(false);

    public PressingCategory(Info<PressingRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(RecipeLayout builder, PressingRecipe recipe) {
        builder.addSlot(Role.INPUT, 27, 51)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getIngredients().get(0));

        List<ProcessingOutput> results = recipe.getRollableResults();
        int i = 0;
        for (ProcessingOutput output : results) {
            addOutputSlot(builder, 131 + 19 * i, 50, output);
            i++;
        }
    }

    @Override
    public void draw(PressingRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 61, 41);
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 54);

        press.draw(graphics, getBackgroundWidth() / 2 - 17, 22);
    }
}
