package com.simibubi.create.compat.rei.category;

import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.animations.AnimatedCrushingWheels;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.createmod.catnip.layout.LayoutHelper;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CrushingCategory extends CreateRecipeCategory<AbstractCrushingRecipe> {

    private final AnimatedCrushingWheels crushingWheels = new AnimatedCrushingWheels();

    public CrushingCategory(Info<AbstractCrushingRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(RecipeLayout builder, AbstractCrushingRecipe recipe) {
        builder.addSlot(Role.INPUT, 51, 3)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getIngredients().get(0));

        int xOffset = getBackgroundWidth() / 2;
        int yOffset = 86;

        layoutOutput(recipe)
                .forEach(
                        layoutEntry ->
                                addOutputSlot(
                                        builder,
                                        (xOffset) + layoutEntry.posX() + 1,
                                        yOffset + layoutEntry.posY() + 1,
                                        layoutEntry.output()));
    }

    private List<LayoutEntry> layoutOutput(StandardProcessingRecipe<?> recipe) {
        int size = recipe.getRollableResults().size();
        List<LayoutEntry> positions = new ArrayList<>(size);

        LayoutHelper layout = LayoutHelper.centeredHorizontal(size, 1, 18, 18, 1);
        for (ProcessingOutput result : recipe.getRollableResults()) {
            positions.add(new LayoutEntry(result, layout.getX(), layout.getY()));
            layout.next();
        }

        return positions;
    }

    private record LayoutEntry(ProcessingOutput output, int posX, int posY) {}

    @Override
    public void draw(
            AbstractCrushingRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 72, 7);

        crushingWheels.draw(graphics, 62, 59);
    }
}
