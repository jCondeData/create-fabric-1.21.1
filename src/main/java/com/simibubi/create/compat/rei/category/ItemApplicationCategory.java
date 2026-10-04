package com.simibubi.create.compat.rei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ItemApplicationCategory extends CreateRecipeCategory<ItemApplicationRecipe> {

    public ItemApplicationCategory(Info<ItemApplicationRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(RecipeLayout builder, ItemApplicationRecipe recipe) {
        builder.addSlot(Role.INPUT, 27, 38)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getProcessedItem());

        RecipeLayout.SlotBuilder heldItem =
                builder.addSlot(Role.INPUT, 51, 5)
                        .setBackground(getRenderedSlot(), -1, -1)
                        .addIngredients(recipe.getRequiredHeldItem());
        if (recipe.shouldKeepHeldItem()) heldItem.insertTooltipLine(notConsumedComponent());

        List<ProcessingOutput> results = recipe.getRollableResults();
        boolean single = results.size() == 1;
        for (int i = 0; i < results.size(); i++) {
            ProcessingOutput output = results.get(i);
            int xOffset = i % 2 == 0 ? 0 : 19;
            int yOffset = (i / 2) * -19;
            addOutputSlot(builder, single ? 132 : 132 + xOffset, 38 + yOffset, output);
        }
    }

    @Override
    public void draw(
            ItemApplicationRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {}

    @Override
    protected void draw(
            ItemApplicationRecipe recipe,
            List<Slot> slots,
            GuiGraphics graphics,
            double mouseX,
            double mouseY) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 62, 47);
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 74, 10);

        if (slots.isEmpty()) return;
        EntryStack<?> displayedIngredient = slots.get(0).getCurrentEntry();
        if (displayedIngredient.getType() != VanillaEntryTypes.ITEM) return;

        Item item = displayedIngredient.<ItemStack>castValue().getItem();
        if (!(item instanceof BlockItem blockItem)) return;

        BlockState state = blockItem.getBlock().defaultBlockState();

        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(74, 51, 100);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale = 20;

        GuiGameElement.of(state)
                .lighting(AnimatedKinetics.DEFAULT_LIGHTING)
                .scale(scale)
                .render(graphics);

        matrixStack.popPose();
    }
}
