package com.simibubi.create.compat.rei.category;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MysteriousItemConversionCategory extends CreateRecipeCategory<ConversionRecipe> {

    private static List<RecipeHolder<ConversionRecipe>> recipes;

    // ConversionRecipe lives in compat.jei but has no JEI dependency (AllRecipeTypes uses it too)
    public static List<RecipeHolder<ConversionRecipe>> getRecipes() {
        if (recipes == null) {
            recipes = new ArrayList<>();
            recipes.add(
                    ConversionRecipe.create(
                            AllItems.EMPTY_BLAZE_BURNER.asStack(),
                            AllBlocks.BLAZE_BURNER.asStack()));
            recipes.add(
                    ConversionRecipe.create(
                            AllBlocks.PECULIAR_BELL.asStack(), AllBlocks.HAUNTED_BELL.asStack()));
        }
        return recipes;
    }

    public MysteriousItemConversionCategory(Info<ConversionRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(RecipeLayout builder, ConversionRecipe recipe) {
        builder.addSlot(Role.INPUT, 27, 17)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(Role.OUTPUT, 132, 17)
                .setBackground(getRenderedSlot(), -1, -1)
                .addItemStack(recipe.getRollableResults().get(0).getStack());
    }

    @Override
    public void draw(ConversionRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 20);
        AllGuiTextures.JEI_QUESTION_MARK.render(graphics, 77, 5);
    }
}
