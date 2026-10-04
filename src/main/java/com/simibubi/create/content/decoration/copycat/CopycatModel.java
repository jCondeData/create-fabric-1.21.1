package com.simibubi.create.content.decoration.copycat;

import com.simibubi.create.AllBlocks;

import io.github.fabricators_of_create.porting_lib.models.CustomParticleIconModel;

import net.createmod.catnip.data.Iterate;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class CopycatModel extends ForwardingBakedModel implements CustomParticleIconModel {

    public CopycatModel(BakedModel originalModel) {
        wrapped = originalModel;
    }

    private void gatherOcclusionData(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            BlockState material,
            OcclusionData occlusionData,
            CopycatBlock copycatBlock) {
        MutableBlockPos mutablePos = new MutableBlockPos();
        for (Direction face : Iterate.directions) {
            if (!copycatBlock.canFaceBeOccluded(state, face)) continue;
            MutableBlockPos neighbourPos = mutablePos.setWithOffset(pos, face);
            if (!Block.shouldRenderFace(material, level, pos, face, neighbourPos))
                occlusionData.occlude(face);
        }
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(
            BlockAndTintGetter blockView,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            RenderContext context) {
        BlockState material;
        if (blockView instanceof RenderAttachedBlockView attachmentView
                && attachmentView.getBlockEntityRenderAttachment(pos)
                        instanceof BlockState material1) {
            material = material1;
        } else {
            material = AllBlocks.COPYCAT_BASE.getDefaultState();
        }

        OcclusionData occlusionData = new OcclusionData();
        if (state.getBlock() instanceof CopycatBlock copycatBlock) {
            gatherOcclusionData(blockView, pos, state, material, occlusionData, copycatBlock);
        }

        CullFaceRemovalData cullFaceRemovalData = new CullFaceRemovalData();
        if (state.getBlock() instanceof CopycatBlock copycatBlock) {
            for (Direction cullFace : Iterate.directions) {
                if (copycatBlock.shouldFaceAlwaysRender(state, cullFace)) {
                    cullFaceRemovalData.remove(cullFace);
                }
            }
        }

        // fabric: If it is the default state do not push transformations, will cause issues with
        // GhostBlockRenderer
        boolean shouldTransform = material != AllBlocks.COPYCAT_BASE.getDefaultState();

        // fabric: need to change the default render material. This also carries the material's
        // emissivity (NeoForge: QuadTransformers.settingMaxEmissivity). Currently, it seems like
        // there's no way to have different levels of emissivity in vanilla, if that changes, then
        // this will need to as well
        if (shouldTransform)
            context.pushTransform(
                    MaterialFixer.create(material, material.emissiveRendering(blockView, pos)));

        emitBlockQuadsInner(
                blockView,
                state,
                pos,
                randomSupplier,
                context,
                material,
                cullFaceRemovalData,
                occlusionData);

        // fabric: pop the material changer transform
        if (shouldTransform) context.popTransform();
    }

    protected abstract void emitBlockQuadsInner(
            BlockAndTintGetter blockView,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            RenderContext context,
            BlockState material,
            CullFaceRemovalData cullFaceRemovalData,
            OcclusionData occlusionData);

    @Override
    public TextureAtlasSprite getParticleIcon(Object data) {
        if (data instanceof BlockState state) {
            BlockState material = getMaterial(state);

            return getIcon(getModelOf(material), null);
        }

        return CustomParticleIconModel.super.getParticleIcon(data);
    }

    public static TextureAtlasSprite getIcon(BakedModel model, @Nullable Object data) {
        if (model instanceof CustomParticleIconModel particleIconModel)
            return particleIconModel.getParticleIcon(data);
        return model.getParticleIcon();
    }

    @NotNull
    public static BlockState getMaterial(BlockState material) {
        return material == null ? AllBlocks.COPYCAT_BASE.getDefaultState() : material;
    }

    public static BakedModel getModelOf(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }

    protected static class OcclusionData {
        private final boolean[] occluded;

        public OcclusionData() {
            occluded = new boolean[6];
        }

        public void occlude(Direction face) {
            occluded[face.get3DDataValue()] = true;
        }

        public boolean isOccluded(Direction face) {
            return face != null && occluded[face.get3DDataValue()];
        }
    }

    protected static class CullFaceRemovalData {
        private final boolean[] shouldRemove;

        public CullFaceRemovalData() {
            shouldRemove = new boolean[6];
        }

        public void remove(Direction face) {
            shouldRemove[face.get3DDataValue()] = true;
        }

        public boolean shouldRemove(Direction face) {
            return face == null ? false : shouldRemove[face.get3DDataValue()];
        }
    }

    private record MaterialFixer(RenderMaterial materialDefault, boolean emissive)
            implements QuadTransform {
        @Override
        public boolean transform(MutableQuadView quad) {
            if (quad.material().blendMode() == BlendMode.DEFAULT) {
                // default needs to be changed from the Copycat's default (cutout) to the wrapped
                // material's default.
                quad.material(materialDefault);
            } else if (emissive && !quad.material().emissive()) {
                quad.material(finder().copyFrom(quad.material()).emissive(true).find());
            }
            return true;
        }

        public static MaterialFixer create(BlockState materialState, boolean emissive) {
            RenderType type = ItemBlockRenderTypes.getChunkRenderType(materialState);
            BlendMode blendMode = BlendMode.fromRenderLayer(type);
            RenderMaterial renderMaterial =
                    finder().blendMode(0, blendMode).emissive(emissive).find();
            return new MaterialFixer(renderMaterial, emissive);
        }

        private static MaterialFinder finder() {
            return Objects.requireNonNull(RendererAccess.INSTANCE.getRenderer()).materialFinder();
        }
    }
}
