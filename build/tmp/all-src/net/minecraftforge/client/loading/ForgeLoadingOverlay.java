/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.loading;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.fml.earlydisplay.ColourScheme;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.progress.ProgressMeter;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This is an implementation of the LoadingOverlay that calls back into the early window rendering, as part of the
 * game loading cycle. We completely replace the {@link #render(GuiGraphics, int, int, float)} call from the parent
 * with one of our own, that allows us to blend our early loading screen into the main window, in the same manner as
 * the Mojang screen. It also allows us to see and tick appropriately as the later stages of the loading system run.
 *
 * It is somewhat a copy of the superclass render method.
 */
public class ForgeLoadingOverlay extends LoadingOverlay {
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final DisplayWindow displayWindow;
    private final ProgressMeter progress;
    private long fadeOutStart = -1L;

    public ForgeLoadingOverlay(final Minecraft mc, final ReloadInstance reloader, final Consumer<Optional<Throwable>> errorConsumer, DisplayWindow displayWindow) {
        super(mc, reloader, errorConsumer, false);
        this.minecraft = mc;
        this.reload = reloader;
        this.onFinish = errorConsumer;
        this.displayWindow = displayWindow;
        displayWindow.addMojangTexture(mc.m_91097_().m_118506_(new ResourceLocation("textures/gui/title/mojangstudios.png")).m_117963_());
        this.progress = StartupMessageManager.prependProgressBar("Minecraft Progress", 100);
    }

    public static Supplier<LoadingOverlay> newInstance(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> handler, DisplayWindow window) {
        return ()->new ForgeLoadingOverlay(mc.get(), ri.get(), handler, window);
    }

    @Override
    public void m_88315_(final @NotNull GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        long millis = Util.m_137550_();
        float fadeouttimer = this.fadeOutStart > -1L ? (float)(millis - this.fadeOutStart) / 1000.0F : -1.0F;
        progress.setAbsolute(Mth.m_14045_((int)(this.reload.m_7750_() * 100f), 0, 100));
        var fade = 1.0F - Mth.m_14036_(fadeouttimer - 1.0F, 0.0F, 1.0F);
        var colour = this.displayWindow.context().colourScheme().background();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);
        if (fadeouttimer >= 1.0F) {
            if (this.minecraft.f_91080_ != null) {
                this.minecraft.f_91080_.m_88315_(graphics, 0, 0, partialTick);
            }
            displayWindow.render(0xff);
        } else {
            GlStateManager._clearColor(colour.redf(), colour.greenf(), colour.bluef(), 1f);
            GlStateManager._clear(GlConst.GL_COLOR_BUFFER_BIT, Minecraft.f_91002_);
            displayWindow.render(0xFF);
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA);
        var fbWidth = this.minecraft.m_91268_().m_85441_();
        var fbHeight = this.minecraft.m_91268_().m_85442_();
        GL30C.glViewport(0, 0, fbWidth, fbHeight);
        final var twidth = this.displayWindow.context().width();
        final var theight = this.displayWindow.context().height();
        var wscale = (float)fbWidth / twidth;
        var hscale = (float)fbHeight / theight;
        var scale = this.displayWindow.context().scale() * Math.min(wscale, hscale) / 2f;
        var wleft = Mth.m_14036_(fbWidth * 0.5f - scale * twidth, 0, fbWidth);
        var wtop = Mth.m_14036_(fbHeight * 0.5f - scale * theight, 0, fbHeight);
        var wright = Mth.m_14036_(fbWidth * 0.5f + scale * twidth, 0, fbWidth);
        var wbottom = Mth.m_14036_(fbHeight * 0.5f + scale * theight, 0, fbHeight);
        GlStateManager.glActiveTexture(GlConst.GL_TEXTURE0);
        RenderSystem.disableCull();
        BufferBuilder bufferbuilder = Tesselator.m_85913_().m_85915_();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);
        RenderSystem.getModelViewMatrix().identity();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, fbWidth, 0.0F, fbHeight, 0.1f, -0.1f), VertexSorting.f_276633_);
        RenderSystem.setShader(GameRenderer::m_172811_);
        // This is fill in around the edges - it's empty solid colour
        bufferbuilder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        // top box from hpos
        addQuad(bufferbuilder, 0, fbWidth, wtop, fbHeight, colour, fade);
        // bottom box to hpos
        addQuad(bufferbuilder, 0, fbWidth, 0, wtop, colour, fade);
        // left box to wpos
        addQuad(bufferbuilder, 0, wleft, wtop, wbottom, colour, fade);
        // right box from wpos
        addQuad(bufferbuilder, wright, fbWidth, wtop, wbottom, colour, fade);
        BufferUploader.m_231202_(bufferbuilder.m_231175_());

        // This is the actual screen data from the loading screen
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::m_172820_);
        RenderSystem.setShaderTexture(0, displayWindow.getFramebufferTextureId());
        bufferbuilder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85819_);
        bufferbuilder.m_5483_(wleft, wbottom, 0f).m_7421_(0, 0).m_85950_(1f, 1f, 1f, fade).m_5752_();
        bufferbuilder.m_5483_(wright, wbottom, 0f).m_7421_(1, 0).m_85950_(1f, 1f, 1f, fade).m_5752_();
        bufferbuilder.m_5483_(wright, wtop, 0f).m_7421_(1, 1).m_85950_(1f, 1f, 1f, fade).m_5752_();
        bufferbuilder.m_5483_(wleft, wtop, 0f).m_7421_(0, 1).m_85950_(1f, 1f, 1f, fade).m_5752_();
        GL30C.glTexParameterIi(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_NEAREST);
        GL30C.glTexParameterIi(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);
        BufferUploader.m_231202_(bufferbuilder.m_231175_());
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1f);

        if (fadeouttimer >= 2.0F) {
            this.minecraft.m_91150_(null);
            this.displayWindow.close();
        }

        if (this.fadeOutStart == -1L && this.reload.m_7746_()) {
            progress.complete();
            this.fadeOutStart = Util.m_137550_();
            try {
                this.reload.m_7748_();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.onFinish.accept(Optional.of(throwable));
            }

            if (this.minecraft.f_91080_ != null) {
                this.minecraft.f_91080_.m_6575_(this.minecraft, this.minecraft.m_91268_().m_85445_(), this.minecraft.m_91268_().m_85446_());
            }
        }
    }

    private static void addQuad(BufferVertexConsumer bufferbuilder, float x0, float x1, float y0, float y1, ColourScheme.Colour colour, float fade) {
        bufferbuilder.m_5483_(x0, y0, 0f).m_85950_(colour.redf(), colour.greenf(), colour.bluef(), fade).m_5752_();
        bufferbuilder.m_5483_(x0, y1, 0f).m_85950_(colour.redf(), colour.greenf(), colour.bluef(), fade).m_5752_();
        bufferbuilder.m_5483_(x1, y1, 0f).m_85950_(colour.redf(), colour.greenf(), colour.bluef(), fade).m_5752_();
        bufferbuilder.m_5483_(x1, y0, 0f).m_85950_(colour.redf(), colour.greenf(), colour.bluef(), fade).m_5752_();
    }
}
