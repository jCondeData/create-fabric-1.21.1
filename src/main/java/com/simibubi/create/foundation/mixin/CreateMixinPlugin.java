package com.simibubi.create.foundation.mixin;

import com.simibubi.create.compat.Mods;

import net.fabricmc.loader.api.FabricLoader;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class CreateMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals(
                "com.simibubi.create.foundation.mixin.fabric.ServerLevelForcedChunksMixin")) {
            return FabricLoader.getInstance()
                    .getModContainer("porting_lib_chunk_loading")
                    .map(
                            mod ->
                                    mod.getMetadata()
                                            .getVersion()
                                            .getFriendlyString()
                                            .equals("3.1.0-beta.90+1.21.1"))
                    .orElse(false);
        }
        if (mixinClassName.contains("ftbchunks")) {
            return Mods.FTBCHUNKS.isLoaded();
        }
        // fabric: gate on the mixin's own package (upstream checks targetClassName, which is
        // Xaero's/Minecraft's class and never matches), so nothing touches Xaero's classes when
        // the world map isn't installed
        if (mixinClassName.startsWith("com.simibubi.create.foundation.mixin.compat.xaeros."))
            return Mods.XAEROWORLDMAP.isLoaded();
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}
}
