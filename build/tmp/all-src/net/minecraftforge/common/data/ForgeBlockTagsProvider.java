/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// We typically don't do static imports as S2S can't remap them {as they are not qualified}, however this conflicts with vanilla and our tag class names, and our tags don't get obfed so its one line of warning.
import static net.minecraftforge.common.Tags.Blocks.*;

public final class ForgeBlockTagsProvider extends BlockTagsProvider
{
    public ForgeBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, "forge", existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void m_6577_(HolderLookup.Provider p_256380_)
    {
        m_206424_(BARRELS).m_206428_(BARRELS_WOODEN);
        m_206424_(BARRELS_WOODEN).m_255245_(Blocks.f_50618_);
        m_206424_(BOOKSHELVES).m_255245_(Blocks.f_50078_);
        m_206424_(CHESTS).addTags(CHESTS_ENDER, CHESTS_TRAPPED, CHESTS_WOODEN);
        m_206424_(CHESTS_ENDER).m_255245_(Blocks.f_50265_);
        m_206424_(CHESTS_TRAPPED).m_255245_(Blocks.f_50325_);
        m_206424_(CHESTS_WOODEN).m_255179_(Blocks.f_50087_, Blocks.f_50325_);
        m_206424_(CHORUS_ADDITIONALLY_GROWS_ON).addTags(END_STONES);
        m_206424_(COBBLESTONE).addTags(COBBLESTONE_NORMAL, COBBLESTONE_INFESTED, COBBLESTONE_MOSSY, COBBLESTONE_DEEPSLATE);
        m_206424_(COBBLESTONE_NORMAL).m_255245_(Blocks.f_50652_);
        m_206424_(COBBLESTONE_INFESTED).m_255245_(Blocks.f_50227_);
        m_206424_(COBBLESTONE_MOSSY).m_255245_(Blocks.f_50079_);
        m_206424_(COBBLESTONE_DEEPSLATE).m_255245_(Blocks.f_152551_);
        m_206424_(END_STONES).m_255245_(Blocks.f_50259_);
        m_206424_(ENDERMAN_PLACE_ON_BLACKLIST);
        m_206424_(FENCE_GATES).addTags(FENCE_GATES_WOODEN);
        m_206424_(FENCE_GATES_WOODEN).m_255179_(Blocks.f_50192_, Blocks.f_50474_, Blocks.f_50475_, Blocks.f_50476_, Blocks.f_50477_, Blocks.f_50478_, Blocks.f_50665_, Blocks.f_50666_, Blocks.f_220850_, Blocks.f_244313_, Blocks.f_271274_);
        m_206424_(FENCES).addTags(FENCES_NETHER_BRICK, FENCES_WOODEN);
        m_206424_(FENCES_NETHER_BRICK).m_255245_(Blocks.f_50198_);
        m_206424_(FENCES_WOODEN).m_206428_(BlockTags.f_13098_);
        m_206424_(GLASS).addTags(GLASS_COLORLESS, STAINED_GLASS, GLASS_TINTED);
        m_206424_(GLASS_COLORLESS).m_255245_(Blocks.f_50058_);
        m_206424_(GLASS_SILICA).m_255179_(Blocks.f_50058_, Blocks.f_50215_, Blocks.f_50211_, Blocks.f_50212_, Blocks.f_50209_, Blocks.f_50207_, Blocks.f_50213_, Blocks.f_50203_, Blocks.f_50208_, Blocks.f_50205_, Blocks.f_50202_, Blocks.f_50148_, Blocks.f_50206_, Blocks.f_50210_, Blocks.f_50214_, Blocks.f_50147_, Blocks.f_50204_);
        m_206424_(GLASS_TINTED).m_255245_(Blocks.f_152498_);
        addColored(m_206424_(STAINED_GLASS)::m_255245_, GLASS, "{color}_stained_glass");
        m_206424_(GLASS_PANES).addTags(GLASS_PANES_COLORLESS, STAINED_GLASS_PANES);
        m_206424_(GLASS_PANES_COLORLESS).m_255245_(Blocks.f_50185_);
        addColored(m_206424_(STAINED_GLASS_PANES)::m_255245_, GLASS_PANES, "{color}_stained_glass_pane");
        m_206424_(GRAVEL).m_255245_(Blocks.f_49994_);
        m_206424_(NETHERRACK).m_255245_(Blocks.f_50134_);
        m_206424_(OBSIDIAN).m_255245_(Blocks.f_50080_);
        m_206424_(ORE_BEARING_GROUND_DEEPSLATE).m_255245_(Blocks.f_152550_);
        m_206424_(ORE_BEARING_GROUND_NETHERRACK).m_255245_(Blocks.f_50134_);
        m_206424_(ORE_BEARING_GROUND_STONE).m_255245_(Blocks.f_50069_);
        m_206424_(ORE_RATES_DENSE).m_255179_(Blocks.f_152505_, Blocks.f_152506_, Blocks.f_152472_, Blocks.f_152473_, Blocks.f_50059_, Blocks.f_50173_);
        m_206424_(ORE_RATES_SINGULAR).m_255179_(Blocks.f_50722_, Blocks.f_49997_, Blocks.f_152469_, Blocks.f_152474_, Blocks.f_152479_, Blocks.f_152467_, Blocks.f_152468_, Blocks.f_50089_, Blocks.f_50264_, Blocks.f_49995_, Blocks.f_49996_, Blocks.f_50331_);
        m_206424_(ORE_RATES_SPARSE).m_255245_(Blocks.f_49998_);
        m_206424_(ORES).addTags(ORES_COAL, ORES_COPPER, ORES_DIAMOND, ORES_EMERALD, ORES_GOLD, ORES_IRON, ORES_LAPIS, ORES_REDSTONE, ORES_QUARTZ, ORES_NETHERITE_SCRAP);
        m_206424_(ORES_COAL).m_206428_(BlockTags.f_144262_);
        m_206424_(ORES_COPPER).m_206428_(BlockTags.f_144264_);
        m_206424_(ORES_DIAMOND).m_206428_(BlockTags.f_144259_);
        m_206424_(ORES_EMERALD).m_206428_(BlockTags.f_144263_);
        m_206424_(ORES_GOLD).m_206428_(BlockTags.f_13043_);
        m_206424_(ORES_IRON).m_206428_(BlockTags.f_144258_);
        m_206424_(ORES_LAPIS).m_206428_(BlockTags.f_144261_);
        m_206424_(ORES_QUARTZ).m_255245_(Blocks.f_50331_);
        m_206424_(ORES_REDSTONE).m_206428_(BlockTags.f_144260_);
        m_206424_(ORES_NETHERITE_SCRAP).m_255245_(Blocks.f_50722_);
        m_206424_(ORES_IN_GROUND_DEEPSLATE).m_255179_(Blocks.f_152469_, Blocks.f_152506_, Blocks.f_152474_, Blocks.f_152479_, Blocks.f_152467_, Blocks.f_152468_, Blocks.f_152472_, Blocks.f_152473_);
        m_206424_(ORES_IN_GROUND_NETHERRACK).m_255179_(Blocks.f_49998_, Blocks.f_50331_);
        m_206424_(ORES_IN_GROUND_STONE).m_255179_(Blocks.f_49997_, Blocks.f_152505_, Blocks.f_50089_, Blocks.f_50264_, Blocks.f_49995_, Blocks.f_49996_, Blocks.f_50059_, Blocks.f_50173_);
        m_206424_(SAND).addTags(SAND_COLORLESS, SAND_RED);
        m_206424_(SAND_COLORLESS).m_255245_(Blocks.f_49992_);
        m_206424_(SAND_RED).m_255245_(Blocks.f_49993_);
        m_206424_(SANDSTONE).m_255179_(Blocks.f_50062_, Blocks.f_50064_, Blocks.f_50063_, Blocks.f_50471_, Blocks.f_50394_, Blocks.f_50396_, Blocks.f_50395_, Blocks.f_50473_);
        m_206424_(STONE).m_255179_(Blocks.f_50334_, Blocks.f_50228_, Blocks.f_50122_, Blocks.f_50226_, Blocks.f_50069_, Blocks.f_50387_, Blocks.f_50281_, Blocks.f_50175_, Blocks.f_152550_, Blocks.f_152555_, Blocks.f_152596_, Blocks.f_152496_);
        m_206424_(STORAGE_BLOCKS).addTags(STORAGE_BLOCKS_AMETHYST, STORAGE_BLOCKS_COAL, STORAGE_BLOCKS_COPPER, STORAGE_BLOCKS_DIAMOND, STORAGE_BLOCKS_EMERALD, STORAGE_BLOCKS_GOLD, STORAGE_BLOCKS_IRON, STORAGE_BLOCKS_LAPIS, STORAGE_BLOCKS_QUARTZ, STORAGE_BLOCKS_RAW_COPPER, STORAGE_BLOCKS_RAW_GOLD, STORAGE_BLOCKS_RAW_IRON, STORAGE_BLOCKS_REDSTONE, STORAGE_BLOCKS_NETHERITE);
        m_206424_(STORAGE_BLOCKS_AMETHYST).m_255245_(Blocks.f_152490_);
        m_206424_(STORAGE_BLOCKS_COAL).m_255245_(Blocks.f_50353_);
        m_206424_(STORAGE_BLOCKS_COPPER).m_255245_(Blocks.f_152504_);
        m_206424_(STORAGE_BLOCKS_DIAMOND).m_255245_(Blocks.f_50090_);
        m_206424_(STORAGE_BLOCKS_EMERALD).m_255245_(Blocks.f_50268_);
        m_206424_(STORAGE_BLOCKS_GOLD).m_255245_(Blocks.f_50074_);
        m_206424_(STORAGE_BLOCKS_IRON).m_255245_(Blocks.f_50075_);
        m_206424_(STORAGE_BLOCKS_LAPIS).m_255245_(Blocks.f_50060_);
        m_206424_(STORAGE_BLOCKS_QUARTZ).m_255245_(Blocks.f_50333_);
        m_206424_(STORAGE_BLOCKS_RAW_COPPER).m_255245_(Blocks.f_152599_);
        m_206424_(STORAGE_BLOCKS_RAW_GOLD).m_255245_(Blocks.f_152600_);
        m_206424_(STORAGE_BLOCKS_RAW_IRON).m_255245_(Blocks.f_152598_);
        m_206424_(STORAGE_BLOCKS_REDSTONE).m_255245_(Blocks.f_50330_);
        m_206424_(STORAGE_BLOCKS_NETHERITE).m_255245_(Blocks.f_50721_);
    }

    private void addColored(Consumer<Block> consumer, TagKey<Block> group, String pattern)
    {
        String prefix = group.f_203868_().m_135815_().toUpperCase(Locale.ENGLISH) + '_';
        for (DyeColor color  : DyeColor.values())
        {
            ResourceLocation key = new ResourceLocation("minecraft", pattern.replace("{color}",  color.m_41065_()));
            TagKey<Block> tag = getForgeTag(prefix + color.m_41065_());
            Block block = ForgeRegistries.BLOCKS.getValue(key);
            if (block == null || block  == Blocks.f_50016_)
                throw new IllegalStateException("Unknown vanilla block: " + key.toString());
            m_206424_(tag).m_255245_(block);
            consumer.accept(block);
        }
    }

    @SuppressWarnings("unchecked")
    private TagKey<Block> getForgeTag(String name)
    {
        try
        {
            name = name.toUpperCase(Locale.ENGLISH);
            return (TagKey<Block>) Tags.Blocks.class.getDeclaredField(name).get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
        {
            throw new IllegalStateException(Tags.Blocks.class.getName() + " is missing tag name: " + name);
        }
    }

    @Override
    public String m_6055_()
    {
        return "Forge Block Tags";
    }
}
