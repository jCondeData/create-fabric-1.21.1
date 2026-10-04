package com.simibubi.create.compat.trainmap;

import com.simibubi.create.compat.Mods;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Train map overlays for map mods. FTB Chunks and JourneyMap were dropped for this pack; Xaero's
 * World Map draws its overlay through {@code XaeroFullscreenMapMixin}, while ticking and the toggle
 * widget's clicks are wired here through Fabric events.
 */
public class TrainMapEvents {

    public static void init() {
        if (!Mods.XAEROWORLDMAP.isLoaded()) return;

        ClientTickEvents.END_CLIENT_TICK.register(TrainMapEvents::tick);
        // Fabric recreates a screen's own events on every init, so re-register each time
        ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) ->
                        ScreenMouseEvents.allowMouseClick(screen)
                                .register(TrainMapEvents::allowMouseClick));
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) return;

        if (Mods.XAEROWORLDMAP.isLoaded()) XaeroTrainMap.tick();
    }

    // fabric: allowMouseClick only fires for presses, replacing the InputConstants.PRESS check
    public static boolean allowMouseClick(Screen screen, double mouseX, double mouseY, int button) {
        if (Mods.XAEROWORLDMAP.isLoaded() && XaeroTrainMap.mouseClick(screen, mouseX, mouseY))
            return false;

        return true;
    }
}
