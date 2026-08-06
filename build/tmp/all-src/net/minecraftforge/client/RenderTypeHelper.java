/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

/**
 * Provides helper functions replacing those in {@link ItemBlockRenderTypes}.
 */
public final class RenderTypeHelper
{
    /**
     * Provides a {@link RenderType} using {@link DefaultVertexFormat#NEW_ENTITY} for the given {@link DefaultVertexFormat#BLOCK} format.
     * This should be called for each {@link RenderType} returned by {@link BakedModel#getRenderTypes(BlockState, RandomSource, ModelData)}.
     * <p>
     * Mimics the behavior of vanilla's {@link ItemBlockRenderTypes#getRenderType(BlockState, boolean)}.
     */
    @NotNull
    public static RenderType getEntityRenderType(RenderType chunkRenderType, boolean cull)
    {
        if (chunkRenderType != RenderType.m_110466_())
            return Sheets.m_110790_();
        return cull || !Minecraft.m_91085_() ? Sheets.m_110792_() : Sheets.m_110791_();
    }

    /**
     * Provides a {@link RenderType} fit for rendering moving blocks given the specified chunk render type.
     * This should be called for each {@link RenderType} returned by {@link BakedModel#getRenderTypes(BlockState, RandomSource, ModelData)}.
     * <p>
     * Mimics the behavior of vanilla's {@link ItemBlockRenderTypes#getMovingBlockRenderType(BlockState)}.
     */
    @NotNull
    public static RenderType getMovingBlockRenderType(RenderType renderType)
    {
        if (renderType == RenderType.m_110466_())
            return RenderType.m_110469_();
        return renderType;
    }

    /**
     * Provides a fallback {@link RenderType} for the given {@link ItemStack} in the case that none is explicitly specified.
     * <p>
     * Mimics the behavior of vanilla's {@link ItemBlockRenderTypes#getRenderType(ItemStack, boolean)}
     * but removes the need to query the model again if the item is a {@link BlockItem}.
     */
    @NotNull
    public static RenderType getFallbackItemRenderType(ItemStack stack, BakedModel model, boolean cull)
    {
        if (stack.m_41720_() instanceof BlockItem blockItem)
        {
            var renderTypes = model.getRenderTypes(blockItem.m_40614_().m_49966_(), RandomSource.m_216335_(42), ModelData.EMPTY);
            if (renderTypes.contains(RenderType.m_110466_()))
                return getEntityRenderType(RenderType.m_110466_(), cull);
            return Sheets.m_110790_();
        }
        return cull ? Sheets.m_110792_() : Sheets.m_110791_();
    }

    private RenderTypeHelper()
    {
    }
}
