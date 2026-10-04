package com.simibubi.create.foundation.blockEntity.behaviour;

import net.minecraft.network.chat.Component;

import java.util.List;

public record ValueSettingsBoard(
        Component title,
        int maxValue,
        int milestoneInterval,
        List<Component> rows,
        ValueSettingsFormatter formatter) {}
