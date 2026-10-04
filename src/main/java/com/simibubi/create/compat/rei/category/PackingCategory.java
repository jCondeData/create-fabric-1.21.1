package com.simibubi.create.compat.rei.category;

import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.rei.category.animations.AnimatedPress;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PackingCategory extends BasinCategory {

    private final AnimatedPress press = new AnimatedPress(true);
    private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();
    private final PackingType type;

    enum PackingType {
        COMPACTING,
        AUTO_SQUARE
    }

    public static PackingCategory standard(Info<BasinRecipe> info) {
        return new PackingCategory(info, PackingType.COMPACTING);
    }

    public static PackingCategory autoSquare(Info<BasinRecipe> info) {
        return new PackingCategory(info, PackingType.AUTO_SQUARE);
    }

    protected PackingCategory(Info<BasinRecipe> info, PackingType type) {
        super(info, type != PackingType.AUTO_SQUARE);
        this.type = type;
    }

    @Override
    public void setRecipe(RecipeLayout builder, BasinRecipe recipe) {
        if (type == PackingType.COMPACTING) {
            super.setRecipe(builder, recipe);
            return;
        }

        int i = 0;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int size = ingredients.size();
        int rows = size == 4 ? 2 : 3;
        while (i < size) {
            Ingredient ingredient = ingredients.get(i);
            builder.addSlot(
                            Role.INPUT,
                            (rows == 2 ? 27 : 18) + (i % rows) * 19,
                            51 - (i / rows) * 19)
                    .setBackground(getRenderedSlot(), -1, -1)
                    .addIngredients(ingredient);

            i++;
        }

        builder.addSlot(Role.OUTPUT, 142, 51)
                .setBackground(getRenderedSlot(), -1, -1)
                .addItemStack(getResultItem(recipe));
    }

    @Override
    public void draw(BasinRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        if (type == PackingType.COMPACTING) {
            super.draw(recipe, graphics, mouseX, mouseY);
        } else {
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 32);
            AllGuiTextures.JEI_SHADOW.render(graphics, 81, 68);
        }

        HeatCondition requiredHeat = recipe.getRequiredHeat();
        if (requiredHeat != HeatCondition.NONE)
            heater.withHeat(requiredHeat.visualizeAsBlazeBurner())
                    .draw(graphics, getBackgroundWidth() / 2 + 3, 55);
        press.draw(graphics, getBackgroundWidth() / 2 + 3, 34);
    }
}
