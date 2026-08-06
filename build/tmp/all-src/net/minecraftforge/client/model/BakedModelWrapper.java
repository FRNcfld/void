/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wrapper for {@link BakedModel} which delegates all operations to its parent.
 * <p>
 * Useful for creating wrapper baked models which only override certain properties.
 */
public abstract class BakedModelWrapper<T extends BakedModel> implements BakedModel
{
    protected final T originalModel;

    public BakedModelWrapper(T originalModel)
    {
        this.originalModel = originalModel;
    }

    @Override
    public List<BakedQuad> m_213637_(@Nullable BlockState state, @Nullable Direction side, RandomSource rand)
    {
        return originalModel.m_213637_(state, side, rand);
    }

    @Override
    public boolean m_7541_()
    {
        return originalModel.m_7541_();
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state)
    {
        return originalModel.useAmbientOcclusion(state);
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state, RenderType renderType)
    {
        return originalModel.useAmbientOcclusion(state, renderType);
    }

    @Override
    public boolean m_7539_()
    {
        return originalModel.m_7539_();
    }

    @Override
    public boolean m_7547_()
    {
        return originalModel.m_7547_();
    }

    @Override
    public boolean m_7521_()
    {
        return originalModel.m_7521_();
    }

    @Override
    public TextureAtlasSprite m_6160_()
    {
        return originalModel.m_6160_();
    }

    @Override
    public ItemTransforms m_7442_()
    {
        return originalModel.m_7442_();
    }

    @Override
    public ItemOverrides m_7343_()
    {
        return originalModel.m_7343_();
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform)
    {
        return originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data)
    {
        return originalModel.getParticleIcon(data);
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType)
    {
        return originalModel.getQuads(state, side, rand, extraData, renderType);
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData)
    {
        return originalModel.getModelData(level, pos, state, modelData);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data)
    {
        return originalModel.getRenderTypes(state, rand, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous)
    {
        return originalModel.getRenderTypes(itemStack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous)
    {
        return originalModel.getRenderPasses(itemStack, fabulous);
    }
}
