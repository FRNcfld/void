/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.common.util.NonNullSupplier;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;

@SuppressWarnings("deprecation")
public enum ForgeRenderTypes
{
    ITEM_LAYERED_SOLID(()-> getItemLayeredSolid(TextureAtlas.f_118259_)),
    ITEM_LAYERED_CUTOUT(()-> getItemLayeredCutout(TextureAtlas.f_118259_)),
    ITEM_LAYERED_CUTOUT_MIPPED(()-> getItemLayeredCutoutMipped(TextureAtlas.f_118259_)),
    ITEM_LAYERED_TRANSLUCENT(()-> getItemLayeredTranslucent(TextureAtlas.f_118259_)),
    ITEM_UNSORTED_TRANSLUCENT(()-> getUnsortedTranslucent(TextureAtlas.f_118259_)),
    ITEM_UNLIT_TRANSLUCENT(()-> getUnlitTranslucent(TextureAtlas.f_118259_)),
    ITEM_UNSORTED_UNLIT_TRANSLUCENT(()-> getUnlitTranslucent(TextureAtlas.f_118259_, false)),
    TRANSLUCENT_ON_PARTICLES_TARGET(() -> getTranslucentParticlesTarget(TextureAtlas.f_118259_));

    public static boolean enableTextTextureLinearFiltering = false;

    /**
     * @return A RenderType fit for multi-layer solid item rendering.
     */
    public static RenderType getItemLayeredSolid(ResourceLocation textureLocation)
    {
        return Internal.LAYERED_ITEM_SOLID.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer cutout item item rendering.
     */
    public static RenderType getItemLayeredCutout(ResourceLocation textureLocation)
    {
        return Internal.LAYERED_ITEM_CUTOUT.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer cutout-mipped item rendering.
     */
    public static RenderType getItemLayeredCutoutMipped(ResourceLocation textureLocation)
    {
        return Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer translucent item rendering.
     */
    public static RenderType getItemLayeredTranslucent(ResourceLocation textureLocation)
    {
        return Internal.LAYERED_ITEM_TRANSLUCENT.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with depth sorting disabled.
     */
    public static RenderType getUnsortedTranslucent(ResourceLocation textureLocation)
    {
        return Internal.UNSORTED_TRANSLUCENT.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with diffuse lighting disabled
     * so that fullbright quads look correct.
     */
    public static RenderType getUnlitTranslucent(ResourceLocation textureLocation)
    {
        return Internal.UNLIT_TRANSLUCENT_SORTED.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with diffuse lighting disabled
     * so that fullbright quads look correct.
     * @param sortingEnabled If false, depth sorting will not be performed.
     */
    public static RenderType getUnlitTranslucent(ResourceLocation textureLocation, boolean sortingEnabled)
    {
        return (sortingEnabled ? Internal.UNLIT_TRANSLUCENT_SORTED : Internal.UNLIT_TRANSLUCENT_UNSORTED).apply(textureLocation);
    }

    /**
     * @return Same as {@link RenderType#entityCutout(ResourceLocation)}, but with mipmapping enabled.
     */
    public static RenderType getEntityCutoutMipped(ResourceLocation textureLocation)
    {
        return Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
    }

    /**
     * @return Replacement of {@link RenderType#text(ResourceLocation)}, but with optional linear texture filtering.
     */
    public static RenderType getText(ResourceLocation locationIn)
    {
        return Internal.TEXT.apply(locationIn);
    }

    /**
     * @return Replacement of {@link RenderType#textIntensity(ResourceLocation)}, but with optional linear texture filtering.
     */
    public static RenderType getTextIntensity(ResourceLocation locationIn)
    {
        return Internal.TEXT_INTENSITY.apply(locationIn);
    }

    /**
     * @return Replacement of {@link RenderType#textPolygonOffset(ResourceLocation)}, but with optional linear texture filtering.
     */
    public static RenderType getTextPolygonOffset(ResourceLocation locationIn)
    {
        return Internal.TEXT_POLYGON_OFFSET.apply(locationIn);
    }

    /**
     * @return Replacement of {@link RenderType#textIntensityPolygonOffset(ResourceLocation)}, but with optional linear texture filtering.
     */
    public static RenderType getTextIntensityPolygonOffset(ResourceLocation locationIn)
    {
        return Internal.TEXT_INTENSITY_POLYGON_OFFSET.apply(locationIn);
    }

    /**
     * @return Replacement of {@link RenderType#textSeeThrough(ResourceLocation)}, but with optional linear texture filtering.
     */
    public static RenderType getTextSeeThrough(ResourceLocation locationIn)
    {
        return Internal.TEXT_SEETHROUGH.apply(locationIn);
    }

    /**
     * @return Replacement of {@link RenderType#textIntensitySeeThrough(ResourceLocation)}, but with optional linear texture filtering.
     */
    public static RenderType getTextIntensitySeeThrough(ResourceLocation locationIn)
    {
        return Internal.TEXT_INTENSITY_SEETHROUGH.apply(locationIn);
    }

    /**
     * @return A variation of {@link RenderType#translucent()} that uses {@link OutputStateShard#PARTICLES_TARGET} to allow fabulous transparency sorting when using {@link RenderLevelStageEvent}
     */
    public static RenderType getTranslucentParticlesTarget(ResourceLocation locationIn)
    {
        return Internal.TRANSLUCENT_PARTICLES_TARGET.apply(locationIn);
    }

    // ----------------------------------------
    //  Implementation details below this line
    // ----------------------------------------

    private final NonNullSupplier<RenderType> renderTypeSupplier;

    ForgeRenderTypes(NonNullSupplier<RenderType> renderTypeSupplier)
    {
        // Wrap in a Lazy<> to avoid running the supplier more than once.
        this.renderTypeSupplier = NonNullLazy.of(renderTypeSupplier);
    }

    public RenderType get()
    {
        return renderTypeSupplier.get();
    }


    private static class Internal extends RenderType
    {
        private static final ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_UNLIT_SHADER = new ShaderStateShard(ForgeHooksClient.ClientEvents::getEntityTranslucentUnlitShader);

        private Internal(String name, VertexFormat fmt, VertexFormat.Mode glMode, int size, boolean doCrumbling, boolean depthSorting, Runnable onEnable, Runnable onDisable)
        {
            super(name, fmt, glMode, size, doCrumbling, depthSorting, onEnable, onDisable);
            throw new IllegalStateException("This class must not be instantiated");
        }

        public static Function<ResourceLocation, RenderType> UNSORTED_TRANSLUCENT = Util.m_143827_(Internal::unsortedTranslucent);
        private static RenderType unsortedTranslucent(ResourceLocation textureLocation)
        {
            final boolean sortingEnabled = false;
            CompositeState renderState = CompositeState.m_110628_()
                    .m_173292_(RenderType.f_173066_)
                    .m_173290_(new TextureStateShard(textureLocation, false, false))
                    .m_110685_(f_110139_)
                    .m_110661_(f_110110_)
                    .m_110671_(f_110152_)
                    .m_110677_(f_110154_)
                    .m_110691_(true);
            return m_173215_("forge_entity_unsorted_translucent", DefaultVertexFormat.f_85812_, VertexFormat.Mode.QUADS, 256, true, sortingEnabled, renderState);
        }

        private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.m_143821_((p_173227_, p_173228_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.m_110628_()
                    .m_173292_(f_173066_)
                    .m_173290_(new RenderStateShard.TextureStateShard(p_173227_, false, false))
                    .m_110685_(f_110139_)
                    .m_110661_(f_110110_)
                    .m_110671_(f_110152_)
                    .m_110677_(f_110154_)
                    .m_110691_(p_173228_);
            return m_173215_("entity_translucent", DefaultVertexFormat.f_85812_, VertexFormat.Mode.QUADS, 256, true, true, rendertype$compositestate);
        });

        public static Function<ResourceLocation, RenderType> UNLIT_TRANSLUCENT_SORTED = Util.m_143827_(tex -> Internal.unlitTranslucent(tex, true));
        public static Function<ResourceLocation, RenderType> UNLIT_TRANSLUCENT_UNSORTED = Util.m_143827_(tex -> Internal.unlitTranslucent(tex, false));
        private static RenderType unlitTranslucent(ResourceLocation textureLocation, boolean sortingEnabled)
        {
            CompositeState renderState = CompositeState.m_110628_()
                    .m_173292_(RENDERTYPE_ENTITY_TRANSLUCENT_UNLIT_SHADER)
                    .m_173290_(new TextureStateShard(textureLocation, false, false))
                    .m_110685_(f_110139_)
                    .m_110661_(f_110110_)
                    .m_110671_(f_110152_)
                    .m_110677_(f_110154_)
                    .m_110691_(true);
            return m_173215_("forge_entity_unlit_translucent", DefaultVertexFormat.f_85812_, VertexFormat.Mode.QUADS, 256, true, sortingEnabled, renderState);
        }

        public static Function<ResourceLocation, RenderType> LAYERED_ITEM_SOLID = Util.m_143827_(Internal::layeredItemSolid);
        private static RenderType layeredItemSolid(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(RenderType.f_173112_)
                    .m_173290_(new RenderStateShard.TextureStateShard(locationIn, false, false))
                    .m_110685_(f_110134_)
                    .m_110671_(f_110152_)
                    .m_110677_(f_110154_)
                    .m_110691_(true);
            return m_173215_("forge_item_entity_solid", DefaultVertexFormat.f_85812_, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> LAYERED_ITEM_CUTOUT = Util.m_143827_(Internal::layeredItemCutout);
        private static RenderType layeredItemCutout(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(RenderType.f_173113_)
                    .m_173290_(new RenderStateShard.TextureStateShard(locationIn, false, false))
                    .m_110685_(f_110134_)
                    .m_110671_(f_110152_)
                    .m_110677_(f_110154_)
                    .m_110691_(true);
            return m_173215_("forge_item_entity_cutout", DefaultVertexFormat.f_85812_, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> LAYERED_ITEM_CUTOUT_MIPPED = Util.m_143827_(Internal::layeredItemCutoutMipped);
        private static RenderType layeredItemCutoutMipped(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(RenderType.f_173067_)
                    .m_173290_(new RenderStateShard.TextureStateShard(locationIn, false, true))
                    .m_110685_(f_110134_)
                    .m_110671_(f_110152_)
                    .m_110677_(f_110154_)
                    .m_110691_(true);
            return m_173215_("forge_item_entity_cutout_mipped", DefaultVertexFormat.f_85812_, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> LAYERED_ITEM_TRANSLUCENT = Util.m_143827_(Internal::layeredItemTranslucent);
        private static RenderType layeredItemTranslucent(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(RenderType.f_173066_)
                    .m_173290_(new RenderStateShard.TextureStateShard(locationIn, false, false))
                    .m_110685_(f_110139_)
                    .m_110671_(f_110152_)
                    .m_110677_(f_110154_)
                    .m_110691_(true);
            return m_173215_("forge_item_entity_translucent_cull", DefaultVertexFormat.f_85812_, VertexFormat.Mode.QUADS, 256, true, true, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> TEXT = Util.m_143827_(Internal::getText);
        private static RenderType getText(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(f_173086_)
                    .m_173290_(new CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
                    .m_110685_(f_110139_)
                    .m_110671_(f_110152_)
                    .m_110691_(false);
            return m_173215_("forge_text", DefaultVertexFormat.f_85820_, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> TEXT_INTENSITY = Util.m_143827_(Internal::getTextIntensity);
        private static RenderType getTextIntensity(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(f_173087_)
                    .m_173290_(new CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
                    .m_110685_(f_110139_)
                    .m_110671_(f_110152_)
                    .m_110691_(false);
            return m_173215_("text_intensity", DefaultVertexFormat.f_85820_, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> TEXT_POLYGON_OFFSET = Util.m_143827_(Internal::getTextPolygonOffset);
        private static RenderType getTextPolygonOffset(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(f_173086_)
                    .m_173290_(new CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
                    .m_110685_(f_110139_)
                    .m_110671_(f_110152_)
                    .m_110669_(f_110118_)
                    .m_110691_(false);
            return m_173215_("text_intensity", DefaultVertexFormat.f_85820_, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.m_143827_(Internal::getTextIntensityPolygonOffset);
        private static RenderType getTextIntensityPolygonOffset(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(f_173087_)
                    .m_173290_(new CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
                    .m_110685_(f_110139_)
                    .m_110671_(f_110152_)
                    .m_110669_(f_110118_)
                    .m_110691_(false);
            return m_173215_("text_intensity", DefaultVertexFormat.f_85820_, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> TEXT_SEETHROUGH = Util.m_143827_(Internal::getTextSeeThrough);
        private static RenderType getTextSeeThrough(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(f_173088_)
                    .m_173290_(new CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
                    .m_110685_(f_110139_)
                    .m_110671_(f_110152_)
                    .m_110663_(f_110111_)
                    .m_110687_(f_110115_)
                    .m_110691_(false);
            return m_173215_("forge_text_see_through", DefaultVertexFormat.f_85820_, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> TEXT_INTENSITY_SEETHROUGH = Util.m_143827_(Internal::getTextIntensitySeeThrough);
        private static RenderType getTextIntensitySeeThrough(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(f_173090_)
                    .m_173290_(new CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
                    .m_110685_(f_110139_)
                    .m_110671_(f_110152_)
                    .m_110663_(f_110111_)
                    .m_110687_(f_110115_)
                    .m_110691_(false);
            return m_173215_("forge_text_see_through", DefaultVertexFormat.f_85820_, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
        }

        public static Function<ResourceLocation, RenderType> TRANSLUCENT_PARTICLES_TARGET = Util.m_143827_(Internal::getTranslucentParticlesTarget);
        private static RenderType getTranslucentParticlesTarget(ResourceLocation locationIn) {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.m_110628_()
                    .m_173292_(f_173108_)
                    .m_173290_(new RenderStateShard.TextureStateShard(locationIn, false, true))
                    .m_110685_(f_110139_)
                    .m_110671_(f_110152_)
                    .m_110675_(f_110126_)
                    .m_110691_(true);
            return m_173215_("forge_translucent_particles_target", DefaultVertexFormat.f_85811_, VertexFormat.Mode.QUADS, 2097152, true, true, rendertype$state);
        }
    }

    private static class CustomizableTextureState extends TextureStateShard
    {
        private CustomizableTextureState(ResourceLocation resLoc, Supplier<Boolean> blur, Supplier<Boolean> mipmap)
        {
            super(resLoc, blur.get(), mipmap.get());
            this.f_110131_ = () -> {
                this.f_110329_ = blur.get();
                this.f_110330_ = mipmap.get();
                TextureManager texturemanager = Minecraft.m_91087_().m_91097_();
                texturemanager.m_118506_(resLoc).m_117960_(this.f_110329_, this.f_110330_);
                RenderSystem.setShaderTexture(0, resLoc);
            };
        }
    }
}
