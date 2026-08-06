/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public class CreativeModeTabSearchRegistry
{
    private static final Map<CreativeModeTab, SearchRegistry.Key<ItemStack>> NAME_SEARCH_KEYS = new IdentityHashMap<>();
    private static final Map<CreativeModeTab, SearchRegistry.Key<ItemStack>> TAG_SEARCH_KEYS = new IdentityHashMap<>();

    public static Map<CreativeModeTab, SearchRegistry.Key<ItemStack>> getNameSearchKeys() {
        Map<CreativeModeTab, SearchRegistry.Key<ItemStack>> nameSearchKeys = new IdentityHashMap<>();

        nameSearchKeys.put(CreativeModeTabs.m_258007_(), getNameSearchKey(CreativeModeTabs.m_258007_()));

        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs())
        {
            SearchRegistry.Key<ItemStack> nameSearchKey = getNameSearchKey(tab);
            if (nameSearchKey != null)
                nameSearchKeys.put(tab, nameSearchKey);
        }

        return nameSearchKeys;
    }

    public static Map<CreativeModeTab, SearchRegistry.Key<ItemStack>> getTagSearchKeys() {
        Map<CreativeModeTab, SearchRegistry.Key<ItemStack>> tagSearchKeys = new IdentityHashMap<>();

        tagSearchKeys.put(CreativeModeTabs.m_258007_(), getTagSearchKey(CreativeModeTabs.m_258007_()));

        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs())
        {
            SearchRegistry.Key<ItemStack> tagSearchKey = getTagSearchKey(tab);
            if (tagSearchKey != null)
                tagSearchKeys.put(tab, tagSearchKey);
        }

        return tagSearchKeys;
    }

    @Nullable
    public static SearchRegistry.Key<ItemStack> getNameSearchKey(CreativeModeTab tab)
    {
        if (tab == CreativeModeTabs.m_258007_())
            return SearchRegistry.f_119941_;

        if (!tab.hasSearchBar())
            return null;

        return NAME_SEARCH_KEYS.computeIfAbsent(tab, k -> new SearchRegistry.Key<>());
    }

    @Nullable
    public static SearchRegistry.Key<ItemStack> getTagSearchKey(CreativeModeTab tab)
    {
        if (tab == CreativeModeTabs.m_258007_())
            return SearchRegistry.f_119942_;

        if (!tab.hasSearchBar())
            return null;

        return TAG_SEARCH_KEYS.computeIfAbsent(tab, k -> new SearchRegistry.Key<>());
    }

    // We do it this way to prevent unwanted classloading
    @ApiStatus.Internal
    public static void createSearchTrees() {
        Minecraft.m_91087_().m_91271_();
    }
}
