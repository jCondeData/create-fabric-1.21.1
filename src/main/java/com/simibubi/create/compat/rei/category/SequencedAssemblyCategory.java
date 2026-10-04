package com.simibubi.create.compat.rei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.sequencedAssembly.ReiSequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SequencedAssemblyCategory extends CreateRecipeCategory<SequencedAssemblyRecipe> {

    // displays are built on REI's reload thread and drawn on the render thread
    Map<ResourceLocation, ReiSequencedAssemblySubCategory> subCategories =
            new ConcurrentHashMap<>();

    public SequencedAssemblyCategory(Info<SequencedAssemblyRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(RecipeLayout builder, SequencedAssemblyRecipe recipe) {
        boolean noRandomOutput = recipe.getOutputChance() == 1;
        int xOffset = noRandomOutput ? 0 : -7;

        builder.addSlot(Role.INPUT, 27 + xOffset, 91)
                .setBackground(getRenderedSlot(), -1, -1)
                .addItemStacks(List.of(recipe.getIngredient().getItems()));
        RecipeLayout.SlotBuilder output =
                builder.addSlot(Role.OUTPUT, 132 + xOffset, 91)
                        .setBackground(getRenderedSlot(recipe.getOutputChance()), -1, -1)
                        .addItemStack(getResultItem(recipe));
        if (!noRandomOutput) output.insertTooltipLine(chanceComponent(recipe.getOutputChance()));

        int width = 0;
        int margin = 3;
        for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence())
            width += getSubCategory(sequencedRecipe).getWidth() + margin;
        width -= margin;
        int x = width / -2 + getBackgroundWidth() / 2;

        for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
            ReiSequencedAssemblySubCategory subCategory = getSubCategory(sequencedRecipe);
            subCategory.setRecipe(builder, sequencedRecipe, x);
            x += subCategory.getWidth() + margin;
        }

        for (int i = 1; i < recipe.getLoops(); i++) {
            for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
                NonNullList<Ingredient> sequencedIngredients =
                        sequencedRecipe.getRecipe().getIngredients();
                for (Ingredient ingredient :
                        sequencedIngredients.subList(1, sequencedIngredients.size()))
                    builder.addInvisibleIngredients(Role.INPUT).addIngredients(ingredient);
                for (FluidIngredient fluidIngredient :
                        sequencedRecipe.getRecipe().getFluidIngredients())
                    builder.addInvisibleIngredients(Role.INPUT)
                            .addFluidStacks(fluidIngredient.getMatchingFluidStacks());
            }
        }
    }

    private ReiSequencedAssemblySubCategory getSubCategory(SequencedRecipe<?> sequencedRecipe) {
        return subCategories.computeIfAbsent(
                RegisteredObjectsHelper.getKeyOrThrow(sequencedRecipe.getRecipe().getSerializer()),
                rl ->
                        ReiSequencedAssemblySubCategory.factoryFor(
                                        sequencedRecipe.getAsAssemblyRecipe().getJEISubCategory())
                                .get());
    }

    final String[] romans = {"I", "II", "III", "IV", "V", "VI", "-"};

    @Override
    public void draw(
            SequencedAssemblyRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();

        matrixStack.pushPose();
        matrixStack.translate(0, 15, 0);
        boolean singleOutput = recipe.getOutputChance() == 1;
        int xOffset = singleOutput ? 0 : -7;
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52 + xOffset, 79);
        if (!singleOutput) {
            AllGuiTextures.JEI_CHANCE_SLOT.render(graphics, 150 + xOffset, 75);
            Component component = Component.literal("?").withStyle(ChatFormatting.BOLD);
            graphics.drawString(
                    font,
                    component,
                    font.width(component) / -2 + 8 + 150 + xOffset,
                    2 + 78,
                    0xefefef);
        }

        if (recipe.getLoops() > 1) {
            matrixStack.pushPose();
            matrixStack.translate(15, 9, 0);
            AllIcons.I_SEQ_REPEAT.render(graphics, 50 + xOffset, 75);
            Component repeat = Component.literal("x" + recipe.getLoops());
            graphics.drawString(font, repeat, 66 + xOffset, 80, 0x888888, false);
            matrixStack.popPose();
        }

        matrixStack.popPose();

        int width = 0;
        int margin = 3;
        for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence())
            width += getSubCategory(sequencedRecipe).getWidth() + margin;
        width -= margin;
        matrixStack.translate(width / -2 + getBackgroundWidth() / 2, 0, 0);

        matrixStack.pushPose();
        List<SequencedRecipe<?>> sequence = recipe.getSequence();
        for (int i = 0; i < sequence.size(); i++) {
            SequencedRecipe<?> sequencedRecipe = sequence.get(i);
            ReiSequencedAssemblySubCategory subCategory = getSubCategory(sequencedRecipe);
            int subWidth = subCategory.getWidth();
            MutableComponent component = Component.literal("" + romans[Math.min(i, 6)]);
            graphics.drawString(
                    font, component, font.width(component) / -2 + subWidth / 2, 2, 0x888888, false);
            subCategory.draw(sequencedRecipe, graphics, mouseX, mouseY, i);
            matrixStack.translate(subWidth + margin, 0, 0);
        }
        matrixStack.popPose();

        matrixStack.popPose();
    }

    @Override
    public List<Component> getTooltipStrings(
            SequencedAssemblyRecipe recipe, double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();

        MutableComponent junk = CreateLang.translateDirect("recipe.assembly.junk");

        boolean singleOutput = recipe.getOutputChance() == 1;
        boolean willRepeat = recipe.getLoops() > 1;

        int xOffset = -7;
        int minX = 150 + xOffset;
        int maxX = minX + 18;
        int minY = 90;
        int maxY = minY + 18;
        if (!singleOutput && mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY) {
            float chance = recipe.getOutputChance();
            tooltip.add(junk);
            tooltip.add(chanceComponent(1 - chance));
            return tooltip;
        }

        minX = 55 + xOffset;
        maxX = minX + 65;
        minY = 92;
        maxY = minY + 24;
        if (willRepeat && mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY) {
            tooltip.add(CreateLang.translateDirect("recipe.assembly.repeat", recipe.getLoops()));
            return tooltip;
        }

        if (mouseY > 5 && mouseY < 84) {
            int width = 0;
            int margin = 3;
            for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence())
                width += getSubCategory(sequencedRecipe).getWidth() + margin;
            width -= margin;
            xOffset = width / 2 + getBackgroundWidth() / -2;

            double relativeX = mouseX + xOffset;
            List<SequencedRecipe<?>> sequence = recipe.getSequence();
            for (int i = 0; i < sequence.size(); i++) {
                SequencedRecipe<?> sequencedRecipe = sequence.get(i);
                ReiSequencedAssemblySubCategory subCategory = getSubCategory(sequencedRecipe);
                if (relativeX >= 0 && relativeX < subCategory.getWidth()) {
                    tooltip.add(CreateLang.translateDirect("recipe.assembly.step", i + 1));
                    tooltip.add(
                            sequencedRecipe
                                    .getAsAssemblyRecipe()
                                    .getDescriptionForAssembly()
                                    .plainCopy()
                                    .withStyle(ChatFormatting.DARK_GREEN));
                    return tooltip;
                }
                relativeX -= subCategory.getWidth() + margin;
            }
        }

        return tooltip;
    }

    protected MutableComponent chanceComponent(float chance) {
        String number =
                chance < 0.01
                        ? "<1"
                        : chance > 0.99 ? ">99" : String.valueOf(Math.round(chance * 100));
        return CreateLang.translateDirect("recipe.processing.chance", number)
                .withStyle(ChatFormatting.GOLD);
    }
}
