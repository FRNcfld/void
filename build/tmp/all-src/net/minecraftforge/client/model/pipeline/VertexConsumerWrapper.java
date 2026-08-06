/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model.pipeline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * Wrapper for {@link VertexConsumer} which delegates all operations to its parent.
 * <p>
 * Useful for defining custom pipeline elements that only process certain data.
 */
public abstract class VertexConsumerWrapper implements VertexConsumer
{
    protected final VertexConsumer parent;

    public VertexConsumerWrapper(VertexConsumer parent)
    {
        this.parent = parent;
    }

    @Override
    public VertexConsumer m_5483_(double x, double y, double z)
    {
        parent.m_5483_(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer m_6122_(int r, int g, int b, int a)
    {
        parent.m_6122_(r, g, b, a);
        return this;
    }

    @Override
    public VertexConsumer m_7421_(float u, float v)
    {
        parent.m_7421_(u, v);
        return this;
    }

    @Override
    public VertexConsumer m_7122_(int u, int v)
    {
        parent.m_7122_(u, v);
        return this;
    }

    @Override
    public VertexConsumer m_7120_(int u, int v)
    {
        parent.m_7120_(u, v);
        return this;
    }

    @Override
    public VertexConsumer m_5601_(float x, float y, float z)
    {
        parent.m_5601_(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer misc(VertexFormatElement element, int... values)
    {
        parent.misc(element, values);
        return this;
    }

    @Override
    public void m_5752_()
    {
        parent.m_5752_();
    }

    @Override
    public void m_7404_(int r, int g, int b, int a)
    {
        parent.m_7404_(r, g, b, a);
    }

    @Override
    public void m_141991_()
    {
        parent.m_141991_();
    }
}
