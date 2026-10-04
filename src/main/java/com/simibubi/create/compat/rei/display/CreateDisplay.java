package com.simibubi.create.compat.rei.display;

import com.simibubi.create.compat.rei.category.CreateRecipeCategory;
import com.simibubi.create.compat.rei.category.RecipeLayout;
import com.simibubi.create.compat.rei.category.RecipeLayout.Role;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

/**
 * A Create recipe shown in REI. The slot layout is computed once by the owning category, and the
 * display's input/output entries (used by REI for recipe and usage lookups) are taken from it.
 */
public class CreateDisplay<T extends Recipe<?>> implements Display {
    private final RecipeHolder<T> holder;
    private final CategoryIdentifier<CreateDisplay<T>> categoryId;
    private final RecipeLayout layout;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;

    public CreateDisplay(CreateRecipeCategory<T> category, RecipeHolder<T> holder) {
        this.holder = holder;
        this.categoryId = category.getCategoryIdentifier();
        this.layout = new RecipeLayout();
        category.buildLayout(layout, holder.value());
        // catalysts (e.g. the blaze cake for superheated recipes) count as uses, like in JEI
        this.inputs = layout.collect(Role.INPUT, Role.CATALYST);
        this.outputs = layout.collect(Role.OUTPUT);
        layout.collect(Role.RENDER_ONLY);
    }

    public RecipeHolder<T> getRecipeHolder() {
        return holder;
    }

    public T getRecipe() {
        return holder.value();
    }

    public RecipeLayout getLayout() {
        return layout;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return categoryId;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(holder.id());
    }
}
