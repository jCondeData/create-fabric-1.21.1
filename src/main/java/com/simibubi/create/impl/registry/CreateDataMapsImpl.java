package com.simibubi.create.impl.registry;

import com.simibubi.create.api.registry.CreateDataMaps;

import io.github.fabricators_of_create.porting_lib.resources.data_maps.PortingLibDataMaps;

public class CreateDataMapsImpl {
    // fabric: called from Create#onInitialize instead of NeoForge's RegisterDataMapTypesEvent
    public static void registerDataMaps() {
        PortingLibDataMaps.registerDataMap(CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS);
        PortingLibDataMaps.registerDataMap(CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS);
    }
}
