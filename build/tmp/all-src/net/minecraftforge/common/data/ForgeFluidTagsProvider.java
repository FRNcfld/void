/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags.Fluids;

import java.util.concurrent.CompletableFuture;

public final class ForgeFluidTagsProvider extends FluidTagsProvider
{
    public ForgeFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, "forge", existingFileHelper);
    }

    @Override
    public void m_6577_(HolderLookup.Provider lookupProvider)
    {
        m_206424_(Fluids.MILK).m_176839_(ForgeMod.MILK.getId()).m_176839_(ForgeMod.FLOWING_MILK.getId());
    }

    @Override
    public String m_6055_()
    {
        return "Forge Fluid Tags";
    }
}
