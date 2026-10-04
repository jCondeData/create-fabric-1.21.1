package com.simibubi.create.content.equipment.bell;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BasicParticleData<T extends Particle>
        implements ParticleOptions, ICustomParticleDataWithSprite<BasicParticleData<T>> {

    public BasicParticleData() {}

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, BasicParticleData<T>> getStreamCodec() {
        return StreamCodec.unit(this);
    }

    @Override
    public MapCodec<BasicParticleData<T>> getCodec(ParticleType<BasicParticleData<T>> type) {
        return MapCodec.unit(this);
    }

    public interface IBasicParticleFactory<U extends Particle> {
        U makeParticle(
                ClientLevel worldIn,
                double x,
                double y,
                double z,
                double vx,
                double vy,
                double vz,
                SpriteSet sprite);
    }

    @OnlyIn(Dist.CLIENT)
    public abstract IBasicParticleFactory<T> getBasicFactory();

    @Override
    @OnlyIn(Dist.CLIENT)
    public ParticleEngine.SpriteParticleRegistration<BasicParticleData<T>> getMetaFactory() {
        return animatedSprite ->
                (data, worldIn, x, y, z, vx, vy, vz) ->
                        getBasicFactory()
                                .makeParticle(worldIn, x, y, z, vx, vy, vz, animatedSprite);
    }
}
