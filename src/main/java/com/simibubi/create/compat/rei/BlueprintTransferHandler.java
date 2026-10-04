package com.simibubi.create.compat.rei;

import com.simibubi.create.content.equipment.blueprint.BlueprintAssignCompleteRecipePacket;
import com.simibubi.create.content.equipment.blueprint.BlueprintScreen;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Clicking "+" on a crafting recipe while a blueprint is open assigns that recipe to it. */
public class BlueprintTransferHandler implements TransferHandler {
    @Override
    public Result handle(Context context) {
        if (!(context.getContainerScreen() instanceof BlueprintScreen blueprint))
            return Result.createNotApplicable();

        Display display = context.getDisplay();
        if (!display.getCategoryIdentifier().equals(BuiltinPlugin.CRAFTING))
            return Result.createNotApplicable();

        Optional<ResourceLocation> recipeId = display.getDisplayLocation();
        if (recipeId.isEmpty()) return Result.createNotApplicable();

        if (context.isActuallyCrafting()) {
            CatnipServices.NETWORK.sendToServer(
                    new BlueprintAssignCompleteRecipePacket(recipeId.get()));
            context.getMinecraft().setScreen(blueprint);
        }
        return Result.createSuccessful().blocksFurtherHandling();
    }
}
