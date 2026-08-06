/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;

import java.util.Arrays;
import java.util.List;

/**
 * Transformer for {@link BakedQuad baked quads}.
 *
 * @see QuadTransformers
 */
public interface IQuadTransformer
{
    int STRIDE = DefaultVertexFormat.f_85811_.m_86017_();
    int POSITION = findOffset(DefaultVertexFormat.f_85804_);
    int COLOR = findOffset(DefaultVertexFormat.f_85805_);
    int UV0 = findOffset(DefaultVertexFormat.f_85806_);
    int UV1 = findOffset(DefaultVertexFormat.f_85807_);
    int UV2 = findOffset(DefaultVertexFormat.f_85808_);
    int NORMAL = findOffset(DefaultVertexFormat.f_85809_);

    void processInPlace(BakedQuad quad);

    default void processInPlace(List<BakedQuad> quads)
    {
        for (BakedQuad quad : quads)
            processInPlace(quad);
    }

    default BakedQuad process(BakedQuad quad)
    {
        var copy = copy(quad);
        processInPlace(copy);
        return copy;
    }

    default List<BakedQuad> process(List<BakedQuad> inputs)
    {
        return inputs.stream().map(IQuadTransformer::copy).peek(this::processInPlace).toList();
    }

    default IQuadTransformer andThen(IQuadTransformer other)
    {
        return quad -> {
            processInPlace(quad);
            other.processInPlace(quad);
        };
    }

    private static BakedQuad copy(BakedQuad quad)
    {
        var vertices = quad.m_111303_();
        return new BakedQuad(Arrays.copyOf(vertices, vertices.length), quad.m_111305_(), quad.m_111306_(), quad.m_173410_(), quad.m_111307_(), quad.hasAmbientOcclusion());
    }

    private static int findOffset(VertexFormatElement element)
    {
        // Divide by 4 because we want the int offset
        var index = DefaultVertexFormat.f_85811_.m_86023_().indexOf(element);
        return index < 0 ? -1 : DefaultVertexFormat.f_85811_.getOffset(index) / 4;
    }
}
