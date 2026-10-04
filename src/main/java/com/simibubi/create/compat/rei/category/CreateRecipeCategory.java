package com.simibubi.create.compat.rei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.compat.rei.CreateREI;
import com.simibubi.create.compat.rei.DoubleItemIcon;
import com.simibubi.create.compat.rei.ItemIcon;
import com.simibubi.create.compat.rei.category.RecipeLayout.Role;
import com.simibubi.create.compat.rei.category.RecipeLayout.SlotBuilder;
import com.simibubi.create.compat.rei.display.CreateDisplay;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;

import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * REI counterpart of the JEI {@code CreateRecipeCategory}. Subclasses describe their slots with
 * {@link #setRecipe(RecipeLayout, Recipe)} and draw their decorations with {@link #draw}, using the
 * same coordinates as their JEI twins (relative to the top left corner of the JEI background).
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CreateRecipeCategory<T extends Recipe<?>>
        implements DisplayCategory<CreateDisplay<T>> {

    /** Space between REI's recipe panel border and the JEI-style background area. */
    public static final int PADDING = 4;

    protected final CategoryIdentifier<CreateDisplay<T>> type;
    protected final Component title;
    protected final Renderer icon;

    private final int width;
    private final int height;
    private final Supplier<List<RecipeHolder<T>>> recipes;
    private final List<Supplier<? extends ItemStack>> catalysts;

    public CreateRecipeCategory(Info<T> info) {
        this.type = info.recipeType();
        this.title = info.title();
        this.icon = info.icon();
        this.width = info.width();
        this.height = info.height();
        this.recipes = info.recipes();
        this.catalysts = info.catalysts();
    }

    @Override
    public CategoryIdentifier<CreateDisplay<T>> getCategoryIdentifier() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayWidth(CreateDisplay<T> display) {
        return width + 2 * PADDING;
    }

    @Override
    public int getDisplayHeight() {
        return height + 2 * PADDING;
    }

    /** Width of the JEI background this category was laid out for. */
    public int getBackgroundWidth() {
        return width;
    }

    /** Height of the JEI background this category was laid out for. */
    public int getBackgroundHeight() {
        return height;
    }

    protected abstract void setRecipe(RecipeLayout builder, T recipe);

    protected abstract void draw(T recipe, GuiGraphics graphics, double mouseX, double mouseY);

    /**
     * Like JEI's draw with an {@code IRecipeSlotsView}: also gets the visible slot widgets, in
     * layout order, e.g. to read the entry a slot currently shows.
     */
    protected void draw(
            T recipe, List<Slot> slots, GuiGraphics graphics, double mouseX, double mouseY) {
        draw(recipe, graphics, mouseX, mouseY);
    }

    protected List<Component> getTooltipStrings(T recipe, double mouseX, double mouseY) {
        return List.of();
    }

    public final void buildLayout(RecipeLayout builder, T recipe) {
        setRecipe(builder, recipe);
    }

    public void registerRecipes(DisplayRegistry registry) {
        for (RecipeHolder<T> holder : recipes.get()) {
            CreateDisplay<T> display;
            try {
                display = new CreateDisplay<>(this, holder);
            } catch (Exception e) {
                Create.LOGGER.warn(
                        "Skipping recipe {} in REI category {}: {}",
                        holder.id(),
                        type.getIdentifier(),
                        e.toString());
                continue;
            }
            registry.add(display, holder);
        }
    }

    public void registerCatalysts(CategoryRegistry registry) {
        catalysts.forEach(s -> registry.addWorkstations(type, EntryStacks.of(s.get())));
    }

    @Override
    public List<Widget> setupDisplay(CreateDisplay<T> display, Rectangle bounds) {
        T recipe = display.getRecipe();
        int originX = bounds.x + PADDING;
        int originY = bounds.y + PADDING;
        List<SlotBuilder> slots = display.getLayout().getVisibleSlots();

        List<Slot> slotWidgets = new ArrayList<>(slots.size());
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(
                Widgets.createDrawableWidget(
                        (graphics, mouseX, mouseY, delta) -> {
                            PoseStack poseStack = graphics.pose();
                            poseStack.pushPose();
                            poseStack.translate(originX, originY, 0);
                            draw(recipe, slotWidgets, graphics, mouseX - originX, mouseY - originY);
                            poseStack.popPose();
                        }));
        widgets.add(
                Widgets.createDrawableWidget(
                        (graphics, mouseX, mouseY, delta) -> {
                            for (SlotBuilder slot : slots) {
                                AllGuiTextures background = slot.getBackground();
                                if (background == null) continue;
                                background.render(
                                        graphics,
                                        originX + slot.getX() + slot.getBackgroundX(),
                                        originY + slot.getY() + slot.getBackgroundY());
                            }
                        }));

        for (SlotBuilder slot : slots) {
            int x = originX + slot.getX();
            int y = originY + slot.getY();
            Slot widget =
                    slot.hasCustomSize()
                            ? Widgets.createSlot(
                                    new Rectangle(
                                            x - 1,
                                            y - 1,
                                            slot.getWidth() + 2,
                                            slot.getHeight() + 2))
                            : Widgets.createSlot(new Point(x, y));
            widget.disableBackground().entries(slot.build());
            switch (slot.getRole()) {
                case INPUT, CATALYST -> widget.markInput();
                case OUTPUT -> widget.markOutput();
                case RENDER_ONLY -> widget.unmarkInputOrOutput();
            }
            slotWidgets.add(widget);
        }
        widgets.addAll(slotWidgets);

        widgets.add(
                Widgets.createTooltip(
                        mouse -> {
                            if (!bounds.contains(mouse)) return null;
                            for (Slot slot : slotWidgets)
                                if (slot.containsMouse(mouse)) return null;
                            List<Component> lines =
                                    getTooltipStrings(recipe, mouse.x - originX, mouse.y - originY);
                            return lines.isEmpty() ? null : Tooltip.create(mouse, lines);
                        }));
        return widgets;
    }

    public static AllGuiTextures getRenderedSlot() {
        return AllGuiTextures.JEI_SLOT;
    }

    public static AllGuiTextures getRenderedSlot(ProcessingOutput output) {
        return getRenderedSlot(output.getChance());
    }

    public static AllGuiTextures getRenderedSlot(float chance) {
        if (chance == 1) return AllGuiTextures.JEI_SLOT;

        return AllGuiTextures.JEI_CHANCE_SLOT;
    }

    public static ItemStack getResultItem(Recipe<?> recipe) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        return recipe.getResultItem(level.registryAccess());
    }

    /** Adds the chance line JEI's {@code addStochasticTooltip} adds, if the output is random. */
    public static SlotBuilder addStochasticTooltip(SlotBuilder slot, ProcessingOutput output) {
        float chance = output.getChance();
        if (chance != 1)
            slot.addTooltipLine(
                    CreateLang.translateDirect(
                                    "recipe.processing.chance",
                                    chance < 0.01 ? "<1" : (int) (chance * 100))
                            .withStyle(ChatFormatting.GOLD));
        return slot;
    }

    public static SlotBuilder addOutputSlot(
            RecipeLayout builder, int x, int y, ProcessingOutput output) {
        return addStochasticTooltip(
                builder.addSlot(Role.OUTPUT, x, y)
                        .setBackground(getRenderedSlot(output), -1, -1)
                        .addItemStack(output.getStack()),
                output);
    }

    public static SlotBuilder addFluidSlot(
            RecipeLayout builder, int x, int y, FluidIngredient ingredient) {
        // REI draws fluids filling the whole slot by default, like JEI's setFluidRenderer here
        return builder.addSlot(Role.INPUT, x, y)
                .setBackground(getRenderedSlot(), -1, -1)
                .addFluidStacks(ingredient.getMatchingFluidStacks());
    }

    public static SlotBuilder addFluidSlot(RecipeLayout builder, int x, int y, FluidStack stack) {
        return builder.addSlot(Role.OUTPUT, x, y)
                .setBackground(getRenderedSlot(), -1, -1)
                .addFluidStack(stack);
    }

    public static MutableComponent notConsumedComponent() {
        return CreateLang.translateDirect("recipe.deploying.not_consumed")
                .withStyle(ChatFormatting.GOLD);
    }

    public record Info<T extends Recipe<?>>(
            CategoryIdentifier<CreateDisplay<T>> recipeType,
            Component title,
            Renderer icon,
            int width,
            int height,
            Supplier<List<RecipeHolder<T>>> recipes,
            List<Supplier<? extends ItemStack>> catalysts) {}

    public interface Factory<T extends Recipe<?>> {
        CreateRecipeCategory<T> create(Info<T> info);
    }

    public static class Builder<T extends Recipe<? extends RecipeInput>> {
        private final Class<? extends T> recipeClass;
        private Supplier<Boolean> config = () -> true;

        private Renderer icon;
        private int width;
        private int height;

        private final List<Consumer<List<RecipeHolder<T>>>> recipeListConsumers = new ArrayList<>();
        private final List<Supplier<? extends ItemStack>> catalysts = new ArrayList<>();

        public Builder(Class<? extends T> recipeClass) {
            this.recipeClass = recipeClass;
        }

        public Builder<T> enableWhen(Supplier<Boolean> predicate) {
            this.config = predicate;
            return this;
        }

        public Builder<T> enableWhen(ConfigBool configValue) {
            config = configValue::get;
            return this;
        }

        public Builder<T> addRecipeListConsumer(Consumer<List<RecipeHolder<T>>> consumer) {
            recipeListConsumers.add(consumer);
            return this;
        }

        public Builder<T> addRecipes(Supplier<Collection<? extends RecipeHolder<T>>> collection) {
            return addRecipeListConsumer(recipes -> recipes.addAll(collection.get()));
        }

        public Builder<T> addAllRecipesIf(Predicate<RecipeHolder<T>> pred) {
            return addRecipeListConsumer(
                    recipes ->
                            consumeAllRecipesOfType(
                                    recipe -> {
                                        if (pred.test(recipe)) recipes.add(recipe);
                                    }));
        }

        public Builder<T> addAllRecipesIf(
                Predicate<RecipeHolder<?>> pred,
                Function<RecipeHolder<?>, RecipeHolder<T>> converter) {
            return addRecipeListConsumer(
                    recipes ->
                            CreateREI.consumeAllRecipes(
                                    recipe -> {
                                        if (pred.test(recipe)) {
                                            recipes.add(converter.apply(recipe));
                                        }
                                    }));
        }

        public Builder<T> addTypedRecipes(IRecipeTypeInfo recipeTypeEntry) {
            return addTypedRecipes(recipeTypeEntry::getType);
        }

        public <I extends RecipeInput, R extends Recipe<I>> Builder<T> addTypedRecipes(
                Supplier<net.minecraft.world.item.crafting.RecipeType<R>> recipeType) {
            return addRecipeListConsumer(
                    recipes -> consumeTypedRecipesTyped(recipes::add, recipeType.get()));
        }

        public Builder<T> addTypedRecipes(
                Supplier<net.minecraft.world.item.crafting.RecipeType<T>> recipeType,
                Function<RecipeHolder<?>, RecipeHolder<T>> converter) {
            return addRecipeListConsumer(
                    recipes ->
                            CreateREI.consumeTypedRecipes(
                                    recipe -> recipes.add(converter.apply(recipe)),
                                    recipeType.get()));
        }

        public Builder<T> addTypedRecipesIf(
                Supplier<net.minecraft.world.item.crafting.RecipeType<? extends T>> recipeType,
                Predicate<RecipeHolder<?>> pred) {
            return addRecipeListConsumer(
                    recipes ->
                            consumeTypedRecipesTyped(
                                    recipe -> {
                                        if (pred.test(recipe)) {
                                            recipes.add(recipe);
                                        }
                                    },
                                    recipeType.get()));
        }

        public Builder<T> addTypedRecipesExcluding(
                Supplier<net.minecraft.world.item.crafting.RecipeType<? extends T>> recipeType,
                Supplier<net.minecraft.world.item.crafting.RecipeType<? extends T>> excluded) {
            return addRecipeListConsumer(
                    recipes -> {
                        List<RecipeHolder<?>> excludedRecipes =
                                CreateREI.getTypedRecipes(excluded.get());
                        consumeTypedRecipesTyped(
                                recipe -> {
                                    for (RecipeHolder<?> excludedRecipe : excludedRecipes) {
                                        if (CreateREI.doInputsMatch(
                                                recipe.value(), excludedRecipe.value())) {
                                            return;
                                        }
                                    }
                                    recipes.add(recipe);
                                },
                                recipeType.get());
                    });
        }

        public Builder<T> removeRecipes(
                Supplier<net.minecraft.world.item.crafting.RecipeType<? extends T>> recipeType) {
            return addRecipeListConsumer(
                    recipes -> {
                        List<RecipeHolder<?>> excludedRecipes =
                                CreateREI.getTypedRecipes(recipeType.get());
                        recipes.removeIf(
                                recipe -> {
                                    for (RecipeHolder<?> excludedRecipe : excludedRecipes) {
                                        if (CreateREI.doInputsMatch(
                                                        recipe.value(), excludedRecipe.value())
                                                && CreateREI.doOutputsMatch(
                                                        recipe.value(), excludedRecipe.value()))
                                            return true;
                                    }
                                    return false;
                                });
                    });
        }

        public Builder<T> removeNonAutomation() {
            return addRecipeListConsumer(
                    recipes -> recipes.removeIf(AllRecipeTypes.CAN_BE_AUTOMATED.negate()));
        }

        public Builder<T> catalystStack(Supplier<ItemStack> supplier) {
            catalysts.add(supplier);
            return this;
        }

        public Builder<T> catalyst(Supplier<ItemLike> supplier) {
            return catalystStack(() -> new ItemStack(supplier.get().asItem()));
        }

        public Builder<T> icon(Renderer icon) {
            this.icon = icon;
            return this;
        }

        public Builder<T> itemIcon(ItemLike item) {
            icon(new ItemIcon(() -> new ItemStack(item)));
            return this;
        }

        public Builder<T> doubleItemIcon(ItemLike item1, ItemLike item2) {
            icon(new DoubleItemIcon(() -> new ItemStack(item1), () -> new ItemStack(item2)));
            return this;
        }

        /** Same dimensions as the JEI category's empty background. */
        public Builder<T> emptyBackground(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public CreateRecipeCategory<T> build(String name, Factory<T> factory) {
            return build(Create.asResource(name), factory);
        }

        public CreateRecipeCategory<T> build(ResourceLocation id, Factory<T> factory) {
            Supplier<List<RecipeHolder<T>>> recipesSupplier;
            if (config.get()) {
                recipesSupplier =
                        () -> {
                            List<RecipeHolder<T>> recipes = new ArrayList<>();
                            for (Consumer<List<RecipeHolder<T>>> consumer : recipeListConsumers) {
                                consumer.accept(recipes);
                            }
                            return recipes;
                        };
            } else {
                recipesSupplier = Collections::emptyList;
            }

            Info<T> info =
                    new Info<>(
                            CategoryIdentifier.of(id),
                            Component.translatable(id.getNamespace() + ".recipe." + id.getPath()),
                            icon,
                            width,
                            height,
                            recipesSupplier,
                            catalysts);
            return factory.create(info);
        }

        private void consumeAllRecipesOfType(Consumer<RecipeHolder<T>> consumer) {
            CreateREI.consumeAllRecipes(
                    recipeHolder -> {
                        if (recipeClass.isInstance(recipeHolder.value())) {
                            //noinspection unchecked - this is checked by the if statement
                            consumer.accept((RecipeHolder<T>) recipeHolder);
                        }
                    });
        }

        private void consumeTypedRecipesTyped(
                Consumer<RecipeHolder<T>> consumer,
                net.minecraft.world.item.crafting.RecipeType<?> type) {
            CreateREI.consumeTypedRecipes(
                    recipeHolder -> {
                        if (recipeClass.isInstance(recipeHolder.value())) {
                            //noinspection unchecked - this is checked by the if statement
                            consumer.accept((RecipeHolder<T>) recipeHolder);
                        }
                    },
                    type);
        }
    }
}
