package com.simibubi.create.impl.registry;

import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.simibubi.create.api.registry.CreateRegistries;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import org.jetbrains.annotations.ApiStatus.Internal;

@EventBusSubscriber(bus = Bus.MOD)
public class CreateRegistriesImpl {
    @Internal
    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(
                CreateRegistries.POTATO_PROJECTILE_TYPE,
                PotatoCannonProjectileType.CODEC,
                PotatoCannonProjectileType.CODEC);
    }
}
