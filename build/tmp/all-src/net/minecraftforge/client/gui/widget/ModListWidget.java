/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.locale.Language;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.versions.forge.ForgeVersion;
import net.minecraftforge.common.util.MavenVersionStringHelper;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.forgespi.language.IModInfo;

import com.mojang.blaze3d.systems.RenderSystem;

public class ModListWidget extends ObjectSelectionList<ModListWidget.ModEntry>
{
    private static String stripControlCodes(String value) { return net.minecraft.util.StringUtil.m_14406_(value); }
    private static final ResourceLocation VERSION_CHECK_ICONS = new ResourceLocation(ForgeVersion.MOD_ID, "textures/gui/version_check_icons.png");
    private final int listWidth;

    private final ModListScreen parent;

    public ModListWidget(ModListScreen parent, int listWidth, int top, int bottom)
    {
        super(parent.getMinecraftInstance(), listWidth, parent.f_96544_, top, bottom, parent.getFontRenderer().f_92710_ * 2 + 8);
        this.parent = parent;
        this.listWidth = listWidth;
        this.refreshList();
    }

    @Override
    protected int m_5756_()
    {
        return this.listWidth;
    }

    @Override
    public int m_5759_()
    {
        return this.listWidth;
    }

    public void refreshList() {
        this.m_93516_();
        parent.buildModList(this::m_7085_, mod->new ModEntry(mod, this.parent));
    }

    @Override
    protected void m_7733_(GuiGraphics guiGraphics)
    {
        this.parent.m_280273_(guiGraphics);
    }

    public class ModEntry extends ObjectSelectionList.Entry<ModEntry> {
        private final IModInfo modInfo;
        private final ModListScreen parent;

        ModEntry(IModInfo info, ModListScreen parent) {
            this.modInfo = info;
            this.parent = parent;
        }

        @Override
        public Component m_142172_() {
            return Component.m_237110_("narrator.select", modInfo.getDisplayName());
        }

        @Override
        public void m_6311_(GuiGraphics guiGraphics, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isMouseOver, float partialTick)
        {
            Component name = Component.m_237113_(stripControlCodes(modInfo.getDisplayName()));
            Component version = Component.m_237113_(stripControlCodes(MavenVersionStringHelper.artifactVersionToString(modInfo.getVersion())));
            VersionChecker.CheckResult vercheck = VersionChecker.getResult(modInfo);
            Font font = this.parent.getFontRenderer();
            guiGraphics.m_280649_(font, Language.m_128107_().m_5536_(FormattedText.m_130773_(font.m_92854_(name,    listWidth))), left + 3, top + 2, 0xFFFFFF, false);
            guiGraphics.m_280649_(font, Language.m_128107_().m_5536_(FormattedText.m_130773_(font.m_92854_(version, listWidth))), left + 3, top + 2 + font.f_92710_, 0xCCCCCC, false);
            if (vercheck.status().shouldDraw())
            {
                //TODO: Consider adding more icons for visualization
                RenderSystem.setShaderColor(1, 1, 1, 1);
                guiGraphics.m_280168_().m_85836_();
                guiGraphics.m_280163_(VERSION_CHECK_ICONS, getLeft() + f_93388_ - 12, top + entryHeight / 4, vercheck.status().getSheetOffset() * 8, (vercheck.status().isAnimated() && ((System.currentTimeMillis() / 800 & 1)) == 1) ? 8 : 0, 8, 8, 64, 16);
                guiGraphics.m_280168_().m_85849_();
            }
        }

        @Override
        public boolean m_6375_(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_)
        {
            parent.setSelected(this);
            ModListWidget.this.m_6987_(this);
            return false;
        }

        public IModInfo getInfo()
        {
            return modInfo;
        }
    }
}
