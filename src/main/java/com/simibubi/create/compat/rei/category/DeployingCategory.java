package com.simibubi.create.compat.rei.category;

import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.animations.AnimatedDeployer;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class DeployingCategory extends CreateRecipeCategory<DeployerApplicationRecipe> {

    private final AnimatedDeployer deployer = new AnimatedDeployer();

    public DeployingCategory(Info<DeployerApplicationRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(RecipeLayout builder, DeployerApplicationRecipe recipe) {
        builder.addSlot(Role.INPUT, 27, 51)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getProcessedItem());
        RecipeLayout.SlotBuilder handItemSlot =
                builder.addSlot(Role.INPUT, 51, 5)
                        .setBackground(getRenderedSlot(), -1, -1)
                        .addIngredients(recipe.getRequiredHeldItem());

        List<ProcessingOutput> results = recipe.getRollableResults();
        boolean single = results.size() == 1;
        for (int i = 0; i < results.size(); i++) {
            ProcessingOutput output = results.get(i);
            int xOffset = i % 2 == 0 ? 0 : 19;
            int yOffset = (i / 2) * -19;
            addOutputSlot(builder, single ? 132 : 132 + xOffset, 51 + yOffset, output);
        }

        if (recipe.shouldKeepHeldItem()) handItemSlot.insertTooltipLine(notConsumedComponent());
    }

    @Override
    public void draw(
            DeployerApplicationRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
        AllGuiTextures.JEI_DOWN_ARROW.render(
                graphics, 126, 29 + (recipe.getRollableResults().size() > 2 ? -19 : 0));
        deployer.draw(graphics, getBackgroundWidth() / 2 - 13, 22);
    }
}
