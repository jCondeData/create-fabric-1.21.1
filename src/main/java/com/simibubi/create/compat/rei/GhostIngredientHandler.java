package com.simibubi.create.compat.rei;

import com.simibubi.create.content.equipment.blueprint.BlueprintScreen;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemScreen;
import com.simibubi.create.content.logistics.filter.AbstractFilterScreen;
import com.simibubi.create.content.logistics.filter.AttributeFilterScreen;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerScreen;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.AbstractContainerScreenAccessor;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Lets items be dragged from REI into Create's ghost item slots (filters, blueprints, ...). */
public class GhostIngredientHandler<T extends GhostItemMenu<?>>
        implements DraggableStackVisitor<AbstractSimiContainerScreen<T>> {

    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
        return screen instanceof AbstractFilterScreen
                || screen instanceof BlueprintScreen
                || screen instanceof LinkedControllerScreen
                || screen instanceof ScheduleScreen
                || screen instanceof RedstoneRequesterScreen
                || screen instanceof FactoryPanelSetItemScreen;
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(
            DraggingContext<AbstractSimiContainerScreen<T>> context, DraggableStack stack) {
        Point cursor = context.getCurrentPosition();
        if (cursor == null || !(stack.getStack().getValue() instanceof ItemStack item))
            return DraggedAcceptorResult.PASS;

        for (GhostTarget<T> target : getTargets(context.getScreen())) {
            if (target.area.contains(cursor)) {
                target.accept(item);
                return DraggedAcceptorResult.CONSUMED;
            }
        }
        return DraggedAcceptorResult.PASS;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(
            DraggingContext<AbstractSimiContainerScreen<T>> context, DraggableStack stack) {
        if (!(stack.getStack().getValue() instanceof ItemStack)) return Stream.empty();
        return getTargets(context.getScreen()).stream()
                .map(target -> BoundsProvider.ofRectangle(target.area));
    }

    private List<GhostTarget<T>> getTargets(AbstractSimiContainerScreen<T> gui) {
        boolean isAttributeFilter = gui instanceof AttributeFilterScreen;
        List<GhostTarget<T>> targets = new ArrayList<>();
        for (int i = 36; i < gui.getMenu().slots.size(); i++) {
            if (gui.getMenu().slots.get(i).isActive())
                targets.add(new GhostTarget<>(gui, i - 36, isAttributeFilter));
            // Only accept items in 1st slot. 2nd is used for functionality, don't wanna
            // override that one
            if (isAttributeFilter) break;
        }
        return targets;
    }

    private static class GhostTarget<T extends GhostItemMenu<?>> {
        private final Rectangle area;
        private final AbstractSimiContainerScreen<T> gui;
        private final int slotIndex;
        private final boolean isAttributeFilter;

        public GhostTarget(
                AbstractSimiContainerScreen<T> gui, int slotIndex, boolean isAttributeFilter) {
            this.gui = gui;
            this.slotIndex = slotIndex;
            this.isAttributeFilter = isAttributeFilter;
            Slot slot = gui.getMenu().slots.get(slotIndex + 36);
            AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) gui;
            this.area =
                    new Rectangle(
                            access.port_lib$getGuiLeft() + slot.x,
                            access.port_lib$getGuiTop() + slot.y,
                            16,
                            16);
        }

        public void accept(ItemStack ingredient) {
            ItemStack stack = ingredient.copy();
            stack.setCount(1);
            gui.getMenu().ghostInventory.setStackInSlot(slotIndex, stack);

            if (isAttributeFilter) return;

            // sync new filter contents with server
            CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(stack, slotIndex));
        }
    }
}
