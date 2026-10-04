package com.simibubi.create.compat.rei.category.sequencedAssembly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.recipeViewerCommon.SequencedAssemblySubCategoryType;
import com.simibubi.create.compat.rei.category.CreateRecipeCategory;
import com.simibubi.create.compat.rei.category.RecipeLayout;
import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.animations.AnimatedDeployer;
import com.simibubi.create.compat.rei.category.animations.AnimatedPress;
import com.simibubi.create.compat.rei.category.animations.AnimatedSaw;
import com.simibubi.create.compat.rei.category.animations.AnimatedSpout;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

/** REI twin of {@code JeiSequencedAssemblySubCategory}. */
public abstract class ReiSequencedAssemblySubCategory {

    private final int width;

    public ReiSequencedAssemblySubCategory(int width) {
        this.width = width;
    }

    /**
     * {@link SequencedAssemblySubCategoryType} only carries JEI factories, so map Create's own
     * types here. Steps from other mods' recipe types fall back to a plain step without animation.
     */
    public static Supplier<ReiSequencedAssemblySubCategory> factoryFor(
            SequencedAssemblySubCategoryType type) {
        if (type == SequencedAssemblySubCategoryType.PRESSING) return AssemblyPressing::new;
        if (type == SequencedAssemblySubCategoryType.SPOUTING) return AssemblySpouting::new;
        if (type == SequencedAssemblySubCategoryType.DEPLOYING) return AssemblyDeploying::new;
        if (type == SequencedAssemblySubCategoryType.CUTTING) return AssemblyCutting::new;
        return Generic::new;
    }

    public int getWidth() {
        return width;
    }

    public void setRecipe(RecipeLayout builder, SequencedRecipe<?> recipe, int x) {}

    public abstract void draw(
            SequencedRecipe<?> recipe,
            GuiGraphics graphics,
            double mouseX,
            double mouseY,
            int index);

    public static class Generic extends ReiSequencedAssemblySubCategory {

        public Generic() {
            super(25);
        }

        @Override
        public void draw(
                SequencedRecipe<?> recipe,
                GuiGraphics graphics,
                double mouseX,
                double mouseY,
                int index) {}
    }

    public static class AssemblyPressing extends ReiSequencedAssemblySubCategory {

        AnimatedPress press;

        public AssemblyPressing() {
            super(25);
            press = new AnimatedPress(false);
        }

        @Override
        public void draw(
                SequencedRecipe<?> recipe,
                GuiGraphics graphics,
                double mouseX,
                double mouseY,
                int index) {
            PoseStack ms = graphics.pose();
            press.offset = index;
            ms.pushPose();
            ms.translate(-5, 50, 0);
            ms.scale(.6f, .6f, .6f);
            press.draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }
    }

    public static class AssemblySpouting extends ReiSequencedAssemblySubCategory {

        AnimatedSpout spout;

        public AssemblySpouting() {
            super(25);
            spout = new AnimatedSpout();
        }

        @Override
        public void setRecipe(RecipeLayout builder, SequencedRecipe<?> recipe, int x) {
            FluidIngredient fluidIngredient = recipe.getRecipe().getFluidIngredients().get(0);

            CreateRecipeCategory.addFluidSlot(builder, x + 4, 15, fluidIngredient);
        }

        @Override
        public void draw(
                SequencedRecipe<?> recipe,
                GuiGraphics graphics,
                double mouseX,
                double mouseY,
                int index) {
            PoseStack ms = graphics.pose();
            spout.offset = index;
            ms.pushPose();
            ms.translate(-7, 50, 0);
            ms.scale(.75f, .75f, .75f);
            spout.withFluids(
                            recipe.getRecipe()
                                    .getFluidIngredients()
                                    .get(0)
                                    .getMatchingFluidStacks())
                    .draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }
    }

    public static class AssemblyDeploying extends ReiSequencedAssemblySubCategory {

        AnimatedDeployer deployer;

        public AssemblyDeploying() {
            super(25);
            deployer = new AnimatedDeployer();
        }

        @Override
        public void setRecipe(RecipeLayout builder, SequencedRecipe<?> recipe, int x) {
            RecipeLayout.SlotBuilder slot =
                    builder.addSlot(Role.INPUT, x + 4, 15)
                            .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                            .addIngredients(recipe.getRecipe().getIngredients().get(1));

            if (recipe.getAsAssemblyRecipe() instanceof DeployerApplicationRecipe deployerRecipe
                    && deployerRecipe.shouldKeepHeldItem()) {
                slot.insertTooltipLine(CreateRecipeCategory.notConsumedComponent());
            }
        }

        @Override
        public void draw(
                SequencedRecipe<?> recipe,
                GuiGraphics graphics,
                double mouseX,
                double mouseY,
                int index) {
            PoseStack ms = graphics.pose();
            deployer.offset = index;
            ms.pushPose();
            ms.translate(-7, 50, 0);
            ms.scale(.75f, .75f, .75f);
            deployer.draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }
    }

    public static class AssemblyCutting extends ReiSequencedAssemblySubCategory {

        AnimatedSaw saw;

        public AssemblyCutting() {
            super(25);
            saw = new AnimatedSaw();
        }

        @Override
        public void draw(
                SequencedRecipe<?> recipe,
                GuiGraphics graphics,
                double mouseX,
                double mouseY,
                int index) {
            PoseStack ms = graphics.pose();
            ms.pushPose();
            ms.translate(0, 51.5f, 0);
            ms.scale(.6f, .6f, .6f);
            saw.draw(graphics, getWidth() / 2, 30);
            ms.popPose();
        }
    }
}
