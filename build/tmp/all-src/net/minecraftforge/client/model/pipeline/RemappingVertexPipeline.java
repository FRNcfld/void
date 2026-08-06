/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model.pipeline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Vertex pipeline element that remaps incoming data to another format.
 */
public class RemappingVertexPipeline implements VertexConsumer
{
    private static final Set<VertexFormatElement> KNOWN_ELEMENTS = Set.of(DefaultVertexFormat.f_85804_,
            DefaultVertexFormat.f_85805_, DefaultVertexFormat.f_166849_, DefaultVertexFormat.f_85807_,
            DefaultVertexFormat.f_85808_, DefaultVertexFormat.f_85809_, DefaultVertexFormat.f_85810_);
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private final VertexConsumer parent;
    private final VertexFormat targetFormat;

    private final Vector3d position = new Vector3d();
    private final Vector3f normal = new Vector3f();
    private final int[] color = new int[] { 255, 255, 255, 255 };
    private final float[] uv0 = new float[] { 0, 0 };
    private final int[] uv1 = new int[] { OverlayTexture.f_174691_, OverlayTexture.f_174693_ };
    private final int[] uv2 = new int[] { 0, 0 };

    private final Map<VertexFormatElement, Integer> miscElementIds;
    private final int[][] misc;

    public RemappingVertexPipeline(VertexConsumer parent, VertexFormat targetFormat)
    {
        this.parent = parent;
        this.targetFormat = targetFormat;

        this.miscElementIds = new IdentityHashMap<>();
        int i = 0;
        for (var element : targetFormat.m_86023_())
            if (element.m_86048_() != VertexFormatElement.Usage.PADDING && !KNOWN_ELEMENTS.contains(element))
                this.miscElementIds.put(element, i++);
        this.misc = new int[i][];
        Arrays.fill(this.misc, EMPTY_INT_ARRAY);
    }

    @Override
    public VertexConsumer m_5483_(double x, double y, double z)
    {
        position.set(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer m_5601_(float x, float y, float z)
    {
        normal.set(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer m_6122_(int r, int g, int b, int a)
    {
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
        return this;
    }

    @Override
    public VertexConsumer m_7421_(float u, float v)
    {
        uv0[0] = u;
        uv0[1] = v;
        return this;
    }

    @Override
    public VertexConsumer m_7122_(int u, int v)
    {
        uv1[0] = u;
        uv1[1] = v;
        return this;
    }

    @Override
    public VertexConsumer m_7120_(int u, int v)
    {
        uv2[0] = u;
        uv2[1] = v;
        return this;
    }

    @Override
    public VertexConsumer misc(VertexFormatElement element, int... values)
    {
        Integer id = miscElementIds.get(element);
        if (id != null)
            misc[id] = Arrays.copyOf(values, values.length);
        return this;
    }

    @Override
    public void m_5752_()
    {
        for (var element : targetFormat.m_86023_())
        {
            // Ignore padding
            if (element.m_86048_() == VertexFormatElement.Usage.PADDING)
                continue;

            // Try to match and output any of the supported elements, and if that fails, treat as misc
            if (element.equals(DefaultVertexFormat.f_85804_))
                parent.m_5483_(position.x, position.y, position.z);
            else if (element.equals(DefaultVertexFormat.f_85809_))
                parent.m_5601_(normal.x(), normal.y(), normal.z());
            else if (element.equals(DefaultVertexFormat.f_85805_))
                parent.m_6122_(color[0], color[1], color[2], color[3]);
            else if (element.equals(DefaultVertexFormat.f_85806_))
                parent.m_7421_(uv0[0], uv0[1]);
            else if (element.equals(DefaultVertexFormat.f_85807_))
                parent.m_7122_(uv1[0], uv1[1]);
            else if (element.equals(DefaultVertexFormat.f_85808_))
                parent.m_7120_(uv2[0], uv2[1]);
            else
                parent.misc(element, misc[miscElementIds.get(element)]);
        }
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
