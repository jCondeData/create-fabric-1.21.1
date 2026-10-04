package com.simibubi.create.compat.rei.category;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;
import com.simibubi.create.infrastructure.fabric.util.FluidUnit;

import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A small REI stand-in for JEI's {@code IRecipeLayoutBuilder}, so the REI categories can mirror the
 * JEI categories' slot layouts one to one. Slot coordinates are relative to the category origin
 * (the top left corner of the JEI background), exactly like in JEI.
 */
public class RecipeLayout {

    public enum Role {
        INPUT,
        OUTPUT,
        CATALYST,
        RENDER_ONLY
    }

    private final List<SlotBuilder> slots = new ArrayList<>();

    public SlotBuilder addSlot(Role role, int x, int y) {
        SlotBuilder slot = new SlotBuilder(role, x, y, true);
        slots.add(slot);
        return slot;
    }

    /** Ingredients that are part of the recipe (for lookups) but not drawn as a slot. */
    public SlotBuilder addInvisibleIngredients(Role role) {
        SlotBuilder slot = new SlotBuilder(role, 0, 0, false);
        slots.add(slot);
        return slot;
    }

    public List<SlotBuilder> getSlots() {
        return slots;
    }

    public List<SlotBuilder> getVisibleSlots() {
        return slots.stream().filter(SlotBuilder::isVisible).toList();
    }

    /** Finalizes tooltips and collects the entries of all slots with one of the given roles. */
    public List<EntryIngredient> collect(Role... roles) {
        List<EntryIngredient> result = new ArrayList<>();
        for (SlotBuilder slot : slots) {
            for (Role role : roles) {
                if (slot.role == role) {
                    EntryIngredient ingredient = slot.build();
                    if (!ingredient.isEmpty()) result.add(ingredient);
                    break;
                }
            }
        }
        return result;
    }

    public static EntryStack<dev.architectury.fluid.FluidStack> toRei(FluidStack stack) {
        return EntryStacks.of(
                dev.architectury.fluid.FluidStack.create(
                        stack.getFluid(), stack.getAmount(), stack.getComponentsPatch()));
    }

    public static FluidStack fromRei(dev.architectury.fluid.FluidStack stack) {
        // not stack.getPatch(): Architectury 13 returns an empty patch for non-empty stacks
        return new FluidStack(
                FluidVariant.of(stack.getFluid(), stack.getComponents().asPatch()),
                stack.getAmount());
    }

    /** REI adds the raw amount as a literal line built from its "%d Unit" translation. */
    private static boolean isReiFluidAmountLine(Tooltip.Entry entry, long amount) {
        if (!entry.isText()) return false;
        String text = entry.getAsText().getString();
        return text.equals(I18n.get("tooltip.rei.fluid_amount", amount))
                || text.equals(I18n.get("tooltip.rei.fluid_amount.forge", amount));
    }

    public static class SlotBuilder {
        private final Role role;
        private final int x;
        private final int y;
        private final boolean visible;
        private final List<EntryStack<?>> entries = new ArrayList<>();
        private final List<Component> appendedTooltip = new ArrayList<>();
        private final List<Component> insertedTooltip = new ArrayList<>();

        @Nullable private AllGuiTextures background;
        private int backgroundX;
        private int backgroundY;
        private int width = 16;
        private int height = 16;
        @Nullable private EntryIngredient built;

        private SlotBuilder(Role role, int x, int y, boolean visible) {
            this.role = role;
            this.x = x;
            this.y = y;
            this.visible = visible;
        }

        public SlotBuilder setBackground(AllGuiTextures background, int xOffset, int yOffset) {
            this.background = background;
            this.backgroundX = xOffset;
            this.backgroundY = yOffset;
            return this;
        }

        /** Renders the entries into a box of the given size instead of the usual 16x16. */
        public SlotBuilder setSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public SlotBuilder addIngredients(Ingredient ingredient) {
            entries.addAll(EntryIngredients.ofIngredient(ingredient));
            return this;
        }

        public SlotBuilder addItemStack(ItemStack stack) {
            entries.add(EntryStacks.of(stack));
            return this;
        }

        public SlotBuilder addItemStacks(Collection<ItemStack> stacks) {
            for (ItemStack stack : stacks) entries.add(EntryStacks.of(stack));
            return this;
        }

        public SlotBuilder addFluidStack(FluidStack stack) {
            entries.add(toRei(stack));
            return this;
        }

        public SlotBuilder addFluidStacks(Collection<FluidStack> stacks) {
            for (FluidStack stack : stacks) entries.add(toRei(stack));
            return this;
        }

        /** Adds a line at the end of every entry's tooltip. */
        public SlotBuilder addTooltipLine(Component line) {
            appendedTooltip.add(line);
            return this;
        }

        /** Adds a line right below the entry name, like JEI's {@code tooltip.add(1, line)}. */
        public SlotBuilder insertTooltipLine(Component line) {
            insertedTooltip.add(line);
            return this;
        }

        public Role getRole() {
            return role;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean hasCustomSize() {
            return width != 16 || height != 16;
        }

        public boolean isVisible() {
            return visible;
        }

        @Nullable
        public AllGuiTextures getBackground() {
            return background;
        }

        public int getBackgroundX() {
            return backgroundX;
        }

        public int getBackgroundY() {
            return backgroundY;
        }

        public EntryIngredient build() {
            if (built != null) return built;
            for (EntryStack<?> entry : entries) applyTooltips(entry);
            built = EntryIngredient.of(entries);
            return built;
        }

        private void applyTooltips(EntryStack<?> entry) {
            if (!appendedTooltip.isEmpty()) entry.tooltip(List.copyOf(appendedTooltip));

            boolean isFluid = entry.getType() == VanillaEntryTypes.FLUID;
            if (!isFluid && insertedTooltip.isEmpty()) return;

            // potion names and effects are added by PotionFluidEntryRenderer
            long amount =
                    isFluid ? entry.<dev.architectury.fluid.FluidStack>castValue().getAmount() : 0;
            List<Component> lines = List.copyOf(insertedTooltip);
            entry.tooltipProcessor(
                    (stack, tooltip) -> {
                        List<Tooltip.Entry> tooltipEntries = tooltip.entries();
                        int index = Math.min(1, tooltipEntries.size());
                        if (isFluid) {
                            // REI prints the raw droplet count ("27000 Unit"); show the amount in
                            // Create's configured fluid unit instead, like Create's own tooltips
                            tooltipEntries.removeIf(e -> isReiFluidAmountLine(e, amount));
                            tooltipEntries.add(
                                    index++, Tooltip.entry(fluidAmountComponent(amount)));
                        }
                        for (Component line : lines)
                            tooltipEntries.add(index++, Tooltip.entry(line));
                        return tooltip;
                    });
        }

        private static Component fluidAmountComponent(long amount) {
            FluidUnit unit = AllConfigs.client().fluidUnitType.get();
            // FluidUnit.name lacks the "create." prefix of the lang keys, so translate directly
            String unitKey =
                    unit == FluidUnit.MILLIBUCKETS
                            ? "generic.unit.millibuckets"
                            : "generic.unit.droplets";
            return CreateLang.number(unit.convert(amount))
                    .add(CreateLang.translateDirect(unitKey))
                    .style(ChatFormatting.GOLD)
                    .component();
        }
    }
}
