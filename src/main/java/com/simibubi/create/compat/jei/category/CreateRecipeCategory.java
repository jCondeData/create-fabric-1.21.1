package com.simibubi.create.compat.jei.category;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;

import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CreateRecipeCategory<T extends Recipe<?>>
        implements IRecipeCategory<RecipeHolder<T>> {
    private static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
    private static final IDrawable CHANCE_SLOT = asDrawable(AllGuiTextures.JEI_CHANCE_SLOT);

    protected final RecipeType<RecipeHolder<T>> type;
    protected final Component title;
    protected final IDrawable background;
    protected final IDrawable icon;

    private final Supplier<List<RecipeHolder<T>>> recipes;
    private final List<Supplier<? extends ItemStack>> catalysts;

    public CreateRecipeCategory(Info<T> info) {
        this.type = info.recipeType();
        this.title = info.title();
        this.background = info.background();
        this.icon = info.icon();
        this.recipes = info.recipes();
        this.catalysts = info.catalysts();
    }

    @NotNull
    @Override
    public RecipeType<RecipeHolder<T>> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder, RecipeHolder<T> holder, IFocusGroup focuses) {
        setRecipe(builder, holder.value(), focuses);
    }

    @Override
    public void draw(
            RecipeHolder<T> holder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics gui,
            double mouseX,
            double mouseY) {
        draw(holder.value(), recipeSlotsView, gui, mouseX, mouseY);
    }

    @Override
    public List<Component> getTooltipStrings(
            RecipeHolder<T> holder,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY) {
        return getTooltipStrings(holder.value(), recipeSlotsView, mouseX, mouseY);
    }

    protected abstract void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

    protected abstract void draw(
            T recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics gui,
            double mouseX,
            double mouseY);

    protected List<Component> getTooltipStrings(
            T recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return List.of();
    }

    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(type, recipes.get());
    }

    public void registerCatalysts(IRecipeCatalystRegistration registration) {
        catalysts.forEach(s -> registration.addRecipeCatalyst(s.get(), type));
    }

    public static IDrawable getRenderedSlot() {
        return BASIC_SLOT;
    }

    public static IDrawable getRenderedSlot(ProcessingOutput output) {
        return getRenderedSlot(output.getChance());
    }

    public static IDrawable getRenderedSlot(float chance) {
        if (chance == 1) return BASIC_SLOT;

        return CHANCE_SLOT;
    }

    public static ItemStack getResultItem(Recipe<?> recipe) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        return recipe.getResultItem(level.registryAccess());
    }

    public static IRecipeSlotRichTooltipCallback addStochasticTooltip(ProcessingOutput output) {
        return (view, tooltip) -> {
            float chance = output.getChance();
            if (chance != 1)
                tooltip.add(
                        CreateLang.translateDirect(
                                        "recipe.processing.chance",
                                        chance < 0.01 ? "<1" : (int) (chance * 100))
                                .withStyle(ChatFormatting.GOLD));
        };
    }

    public static IRecipeSlotBuilder addFluidSlot(
            IRecipeLayoutBuilder builder, int x, int y, FluidIngredient ingredient) {
        return addFluidSlot(builder, x, y, RecipeIngredientRole.INPUT)
                .addIngredients(
                        FabricTypes.FLUID_STACK, toJei(ingredient.getMatchingFluidStacks()));
    }

    public static IRecipeSlotBuilder addFluidSlot(
            IRecipeLayoutBuilder builder, int x, int y, FluidStack stack) {
        return addFluidSlot(builder, x, y, RecipeIngredientRole.OUTPUT)
                .addIngredient(FabricTypes.FLUID_STACK, toJei(stack));
    }

    public static IRecipeSlotBuilder addFluidSlot(
            IRecipeLayoutBuilder builder, int x, int y, RecipeIngredientRole role) {
        return builder.addSlot(role, x, y)
                .setBackground(getRenderedSlot(), -1, -1)
                .setFluidRenderer(1, false, 16, 16); // make fluid take up the full slot
    }

    // fabric: don't need potion tooltip stuff, handled by attribute handler

    public static FluidStack fromJei(IJeiFluidIngredient jei) {
        return new FluidStack(jei.getFluidVariant(), jei.getAmount());
    }

    public static IJeiFluidIngredient toJei(FluidStack stack) {
        return new IJeiFluidIngredient() {
            @Override
            public FluidVariant getFluidVariant() {
                return stack.getVariant();
            }

            @Override
            public long getAmount() {
                return stack.getAmount();
            }
        };
    }

    public static List<FluidStack> fromJei(List<IJeiFluidIngredient> stacks) {
        return stacks.stream().map(CreateRecipeCategory::fromJei).toList();
    }

    public static List<IJeiFluidIngredient> toJei(Collection<FluidStack> stacks) {
        return stacks.stream().map(CreateRecipeCategory::toJei).toList();
    }

    protected static IDrawable asDrawable(AllGuiTextures texture) {
        return new IDrawable() {
            @Override
            public int getWidth() {
                return texture.getWidth();
            }

            @Override
            public int getHeight() {
                return texture.getHeight();
            }

            @Override
            public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
                texture.render(graphics, xOffset, yOffset);
            }
        };
    }

    public record Info<T extends Recipe<?>>(
            RecipeType<RecipeHolder<T>> recipeType,
            Component title,
            IDrawable background,
            IDrawable icon,
            Supplier<List<RecipeHolder<T>>> recipes,
            List<Supplier<? extends ItemStack>> catalysts) {}

    public interface Factory<T extends Recipe<?>> {
        CreateRecipeCategory<T> create(Info<T> info);
    }
}
