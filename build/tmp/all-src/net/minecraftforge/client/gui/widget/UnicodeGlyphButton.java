/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import net.minecraft.client.gui.components.Button.OnPress;

/**
 * This class provides a button that shows a string glyph at the beginning. The glyph can be scaled using the glyphScale parameter.
 *
 * @author bspkrs
 */
public class UnicodeGlyphButton extends ExtendedButton
{
    public String glyph;
    public float  glyphScale;

    public UnicodeGlyphButton(int xPos, int yPos, int width, int height, Component displayString, String glyph, float glyphScale, OnPress handler)
    {
        super(xPos, yPos, width, height, displayString, handler);
        this.glyph = glyph;
        this.glyphScale = glyphScale;
    }

    @Override
    public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if (this.f_93624_)
        {
            Minecraft mc = Minecraft.m_91087_();
            this.f_93622_ = mouseX >= this.m_252754_() && mouseY >= this.m_252907_() && mouseX < this.m_252754_() + this.f_93618_ && mouseY < this.m_252907_() + this.f_93619_;
            int k = !this.f_93623_ ? 0 : (this.m_198029_() ? 2 : 1);
            guiGraphics.blitWithBorder(f_93617_, this.m_252754_(), this.m_252907_(), 0, 46 + k * 20, this.f_93618_, this.f_93619_, 200, 20, 2, 3, 2, 2);

            Component buttonText = this.m_5646_();
            int glyphWidth = (int) (mc.f_91062_.m_92895_(glyph) * glyphScale);
            int strWidth = mc.f_91062_.m_92852_(buttonText);
            int ellipsisWidth = mc.f_91062_.m_92895_("...");
            int totalWidth = strWidth + glyphWidth;

            if (totalWidth > f_93618_ - 6 && totalWidth > ellipsisWidth)
                buttonText = Component.m_237113_(mc.f_91062_.m_92854_(buttonText, f_93618_ - 6 - ellipsisWidth).getString().trim() + "...") ;

            strWidth = mc.f_91062_.m_92852_(buttonText);
            totalWidth = glyphWidth + strWidth;

            guiGraphics.m_280168_().m_85836_();
            guiGraphics.m_280168_().m_85841_(glyphScale, glyphScale, 1.0F);
            guiGraphics.m_280653_(mc.f_91062_, Component.m_237113_(glyph),
                    (int) (((this.m_252754_() + (this.f_93618_ / 2) - (strWidth / 2)) / glyphScale) - (glyphWidth / (2 * glyphScale)) + 2),
                    (int) (((this.m_252907_() + ((this.f_93619_ - 8) / glyphScale) / 2) - 1) / glyphScale), getFGColor());
            guiGraphics.m_280168_().m_85849_();

            guiGraphics.m_280653_(mc.f_91062_, buttonText, (int) (this.m_252754_() + (this.f_93618_ / 2) + (glyphWidth / glyphScale)),
                    this.m_252907_() + (this.f_93619_ - 8) / 2, getFGColor());

        }
    }
}
