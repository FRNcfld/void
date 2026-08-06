/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.data.tags.TagsProvider.TagLookup;

public final class ForgeItemTagsProvider extends ItemTagsProvider
{
    public ForgeItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagProvider, ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, blockTagProvider, "forge", existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void m_6577_(HolderLookup.Provider lookupProvider)
    {
        m_206421_(Tags.Blocks.BARRELS, Tags.Items.BARRELS);
        m_206421_(Tags.Blocks.BARRELS_WOODEN, Tags.Items.BARRELS_WOODEN);
        m_206424_(Tags.Items.BONES).m_255245_(Items.f_42500_);
        m_206421_(Tags.Blocks.BOOKSHELVES, Tags.Items.BOOKSHELVES);
        m_206421_(Tags.Blocks.CHESTS, Tags.Items.CHESTS);
        m_206421_(Tags.Blocks.CHESTS_ENDER, Tags.Items.CHESTS_ENDER);
        m_206421_(Tags.Blocks.CHESTS_TRAPPED, Tags.Items.CHESTS_TRAPPED);
        m_206421_(Tags.Blocks.CHESTS_WOODEN, Tags.Items.CHESTS_WOODEN);
        m_206421_(Tags.Blocks.COBBLESTONE, Tags.Items.COBBLESTONE);
        m_206421_(Tags.Blocks.COBBLESTONE_NORMAL, Tags.Items.COBBLESTONE_NORMAL);
        m_206421_(Tags.Blocks.COBBLESTONE_INFESTED, Tags.Items.COBBLESTONE_INFESTED);
        m_206421_(Tags.Blocks.COBBLESTONE_MOSSY, Tags.Items.COBBLESTONE_MOSSY);
        m_206421_(Tags.Blocks.COBBLESTONE_DEEPSLATE, Tags.Items.COBBLESTONE_DEEPSLATE);
        m_206424_(Tags.Items.CROPS).addTags(Tags.Items.CROPS_BEETROOT, Tags.Items.CROPS_CARROT, Tags.Items.CROPS_NETHER_WART, Tags.Items.CROPS_POTATO, Tags.Items.CROPS_WHEAT);
        m_206424_(Tags.Items.CROPS_BEETROOT).m_255245_(Items.f_42732_);
        m_206424_(Tags.Items.CROPS_CARROT).m_255245_(Items.f_42619_);
        m_206424_(Tags.Items.CROPS_NETHER_WART).m_255245_(Items.f_42588_);
        m_206424_(Tags.Items.CROPS_POTATO).m_255245_(Items.f_42620_);
        m_206424_(Tags.Items.CROPS_WHEAT).m_255245_(Items.f_42405_);
        m_206424_(Tags.Items.DUSTS).addTags(Tags.Items.DUSTS_GLOWSTONE, Tags.Items.DUSTS_PRISMARINE, Tags.Items.DUSTS_REDSTONE);
        m_206424_(Tags.Items.DUSTS_GLOWSTONE).m_255245_(Items.f_42525_);
        m_206424_(Tags.Items.DUSTS_PRISMARINE).m_255245_(Items.f_42695_);
        m_206424_(Tags.Items.DUSTS_REDSTONE).m_255245_(Items.f_42451_);
        addColored(m_206424_(Tags.Items.DYES)::addTags, Tags.Items.DYES, "{color}_dye");
        m_206424_(Tags.Items.EGGS).m_255245_(Items.f_42521_);
        m_206424_(Tags.Items.ENCHANTING_FUELS).m_206428_(Tags.Items.GEMS_LAPIS);
        m_206421_(Tags.Blocks.END_STONES, Tags.Items.END_STONES);
        m_206424_(Tags.Items.ENDER_PEARLS).m_255245_(Items.f_42584_);
        m_206424_(Tags.Items.FEATHERS).m_255245_(Items.f_42402_);
        m_206421_(Tags.Blocks.FENCE_GATES, Tags.Items.FENCE_GATES);
        m_206421_(Tags.Blocks.FENCE_GATES_WOODEN, Tags.Items.FENCE_GATES_WOODEN);
        m_206421_(Tags.Blocks.FENCES, Tags.Items.FENCES);
        m_206421_(Tags.Blocks.FENCES_NETHER_BRICK, Tags.Items.FENCES_NETHER_BRICK);
        m_206421_(Tags.Blocks.FENCES_WOODEN, Tags.Items.FENCES_WOODEN);
        m_206424_(Tags.Items.GEMS).addTags(Tags.Items.GEMS_AMETHYST, Tags.Items.GEMS_DIAMOND, Tags.Items.GEMS_EMERALD, Tags.Items.GEMS_LAPIS, Tags.Items.GEMS_PRISMARINE, Tags.Items.GEMS_QUARTZ);
        m_206424_(Tags.Items.GEMS_AMETHYST).m_255245_(Items.f_151049_);
        m_206424_(Tags.Items.GEMS_DIAMOND).m_255245_(Items.f_42415_);
        m_206424_(Tags.Items.GEMS_EMERALD).m_255245_(Items.f_42616_);
        m_206424_(Tags.Items.GEMS_LAPIS).m_255245_(Items.f_42534_);
        m_206424_(Tags.Items.GEMS_PRISMARINE).m_255245_(Items.f_42696_);
        m_206424_(Tags.Items.GEMS_QUARTZ).m_255245_(Items.f_42692_);
        m_206421_(Tags.Blocks.GLASS, Tags.Items.GLASS);
        m_206421_(Tags.Blocks.GLASS_TINTED, Tags.Items.GLASS_TINTED);
        m_206421_(Tags.Blocks.GLASS_SILICA, Tags.Items.GLASS_SILICA);
        copyColored(Tags.Blocks.GLASS, Tags.Items.GLASS);
        m_206421_(Tags.Blocks.GLASS_PANES, Tags.Items.GLASS_PANES);
        copyColored(Tags.Blocks.GLASS_PANES, Tags.Items.GLASS_PANES);
        m_206421_(Tags.Blocks.GRAVEL, Tags.Items.GRAVEL);
        m_206424_(Tags.Items.GUNPOWDER).m_255245_(Items.f_42403_);
        m_206424_(Tags.Items.HEADS).m_255179_(Items.f_42682_, Items.f_42683_, Items.f_42680_, Items.f_42678_, Items.f_42679_, Items.f_42681_);
        m_206424_(Tags.Items.INGOTS).addTags(Tags.Items.INGOTS_BRICK, Tags.Items.INGOTS_COPPER, Tags.Items.INGOTS_GOLD, Tags.Items.INGOTS_IRON, Tags.Items.INGOTS_NETHERITE, Tags.Items.INGOTS_NETHER_BRICK);
        m_206424_(Tags.Items.INGOTS_BRICK).m_255245_(Items.f_42460_);
        m_206424_(Tags.Items.INGOTS_COPPER).m_255245_(Items.f_151052_);
        m_206424_(Tags.Items.INGOTS_GOLD).m_255245_(Items.f_42417_);
        m_206424_(Tags.Items.INGOTS_IRON).m_255245_(Items.f_42416_);
        m_206424_(Tags.Items.INGOTS_NETHERITE).m_255245_(Items.f_42418_);
        m_206424_(Tags.Items.INGOTS_NETHER_BRICK).m_255245_(Items.f_42691_);
        m_206424_(Tags.Items.LEATHER).m_255245_(Items.f_42454_);
        m_206424_(Tags.Items.MUSHROOMS).m_255179_(Items.f_41952_, Items.f_41953_);
        m_206424_(Tags.Items.NETHER_STARS).m_255245_(Items.f_42686_);
        m_206421_(Tags.Blocks.NETHERRACK, Tags.Items.NETHERRACK);
        m_206424_(Tags.Items.NUGGETS).addTags(Tags.Items.NUGGETS_IRON, Tags.Items.NUGGETS_GOLD);
        m_206424_(Tags.Items.NUGGETS_IRON).m_255245_(Items.f_42749_);
        m_206424_(Tags.Items.NUGGETS_GOLD).m_255245_(Items.f_42587_);
        m_206421_(Tags.Blocks.OBSIDIAN, Tags.Items.OBSIDIAN);
        m_206421_(Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE, Tags.Items.ORE_BEARING_GROUND_DEEPSLATE);
        m_206421_(Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK, Tags.Items.ORE_BEARING_GROUND_NETHERRACK);
        m_206421_(Tags.Blocks.ORE_BEARING_GROUND_STONE, Tags.Items.ORE_BEARING_GROUND_STONE);
        m_206421_(Tags.Blocks.ORE_RATES_DENSE, Tags.Items.ORE_RATES_DENSE);
        m_206421_(Tags.Blocks.ORE_RATES_SINGULAR, Tags.Items.ORE_RATES_SINGULAR);
        m_206421_(Tags.Blocks.ORE_RATES_SPARSE, Tags.Items.ORE_RATES_SPARSE);
        m_206421_(Tags.Blocks.ORES, Tags.Items.ORES);
        m_206421_(Tags.Blocks.ORES_COAL, Tags.Items.ORES_COAL);
        m_206421_(Tags.Blocks.ORES_COPPER, Tags.Items.ORES_COPPER);
        m_206421_(Tags.Blocks.ORES_DIAMOND, Tags.Items.ORES_DIAMOND);
        m_206421_(Tags.Blocks.ORES_EMERALD, Tags.Items.ORES_EMERALD);
        m_206421_(Tags.Blocks.ORES_GOLD, Tags.Items.ORES_GOLD);
        m_206421_(Tags.Blocks.ORES_IRON, Tags.Items.ORES_IRON);
        m_206421_(Tags.Blocks.ORES_LAPIS, Tags.Items.ORES_LAPIS);
        m_206421_(Tags.Blocks.ORES_QUARTZ, Tags.Items.ORES_QUARTZ);
        m_206421_(Tags.Blocks.ORES_REDSTONE, Tags.Items.ORES_REDSTONE);
        m_206421_(Tags.Blocks.ORES_NETHERITE_SCRAP, Tags.Items.ORES_NETHERITE_SCRAP);
        m_206421_(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE, Tags.Items.ORES_IN_GROUND_DEEPSLATE);
        m_206421_(Tags.Blocks.ORES_IN_GROUND_NETHERRACK, Tags.Items.ORES_IN_GROUND_NETHERRACK);
        m_206421_(Tags.Blocks.ORES_IN_GROUND_STONE, Tags.Items.ORES_IN_GROUND_STONE);
        m_206424_(Tags.Items.RAW_MATERIALS).addTags(Tags.Items.RAW_MATERIALS_COPPER, Tags.Items.RAW_MATERIALS_GOLD, Tags.Items.RAW_MATERIALS_IRON);
        m_206424_(Tags.Items.RAW_MATERIALS_COPPER).m_255245_(Items.f_151051_);
        m_206424_(Tags.Items.RAW_MATERIALS_GOLD).m_255245_(Items.f_151053_);
        m_206424_(Tags.Items.RAW_MATERIALS_IRON).m_255245_(Items.f_151050_);
        m_206424_(Tags.Items.RODS).addTags(Tags.Items.RODS_BLAZE, Tags.Items.RODS_WOODEN);
        m_206424_(Tags.Items.RODS_BLAZE).m_255245_(Items.f_42585_);
        m_206424_(Tags.Items.RODS_WOODEN).m_255245_(Items.f_42398_);
        m_206421_(Tags.Blocks.SAND, Tags.Items.SAND);
        m_206421_(Tags.Blocks.SAND_COLORLESS, Tags.Items.SAND_COLORLESS);
        m_206421_(Tags.Blocks.SAND_RED, Tags.Items.SAND_RED);
        m_206421_(Tags.Blocks.SANDSTONE, Tags.Items.SANDSTONE);
        m_206424_(Tags.Items.SEEDS).addTags(Tags.Items.SEEDS_BEETROOT, Tags.Items.SEEDS_MELON, Tags.Items.SEEDS_PUMPKIN, Tags.Items.SEEDS_WHEAT);
        m_206424_(Tags.Items.SEEDS_BEETROOT).m_255245_(Items.f_42733_);
        m_206424_(Tags.Items.SEEDS_MELON).m_255245_(Items.f_42578_);
        m_206424_(Tags.Items.SEEDS_PUMPKIN).m_255245_(Items.f_42577_);
        m_206424_(Tags.Items.SEEDS_WHEAT).m_255245_(Items.f_42404_);
        m_206424_(Tags.Items.SHEARS).m_255245_(Items.f_42574_);
        m_206424_(Tags.Items.SLIMEBALLS).m_255245_(Items.f_42518_);
        m_206421_(Tags.Blocks.STAINED_GLASS, Tags.Items.STAINED_GLASS);
        m_206421_(Tags.Blocks.STAINED_GLASS_PANES, Tags.Items.STAINED_GLASS_PANES);
        m_206421_(Tags.Blocks.STONE, Tags.Items.STONE);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS, Tags.Items.STORAGE_BLOCKS);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_AMETHYST, Tags.Items.STORAGE_BLOCKS_AMETHYST);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_COAL, Tags.Items.STORAGE_BLOCKS_COAL);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_COPPER, Tags.Items.STORAGE_BLOCKS_COPPER);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_DIAMOND, Tags.Items.STORAGE_BLOCKS_DIAMOND);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_EMERALD, Tags.Items.STORAGE_BLOCKS_EMERALD);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_GOLD, Tags.Items.STORAGE_BLOCKS_GOLD);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_IRON, Tags.Items.STORAGE_BLOCKS_IRON);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_LAPIS, Tags.Items.STORAGE_BLOCKS_LAPIS);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_QUARTZ, Tags.Items.STORAGE_BLOCKS_QUARTZ);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_REDSTONE, Tags.Items.STORAGE_BLOCKS_REDSTONE);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER, Tags.Items.STORAGE_BLOCKS_RAW_COPPER);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD, Tags.Items.STORAGE_BLOCKS_RAW_GOLD);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON, Tags.Items.STORAGE_BLOCKS_RAW_IRON);
        m_206421_(Tags.Blocks.STORAGE_BLOCKS_NETHERITE, Tags.Items.STORAGE_BLOCKS_NETHERITE);
        m_206424_(Tags.Items.STRING).m_255245_(Items.f_42401_);
        m_206424_(Tags.Items.TOOLS_SHIELDS).m_255245_(Items.f_42740_);
        m_206424_(Tags.Items.TOOLS_BOWS).m_255245_(Items.f_42411_);
        m_206424_(Tags.Items.TOOLS_CROSSBOWS).m_255245_(Items.f_42717_);
        m_206424_(Tags.Items.TOOLS_FISHING_RODS).m_255245_(Items.f_42523_);
        m_206424_(Tags.Items.TOOLS_TRIDENTS).m_255245_(Items.f_42713_);
        m_206424_(Tags.Items.TOOLS)
            .addTags(ItemTags.f_271388_, ItemTags.f_271207_, ItemTags.f_271360_, ItemTags.f_271138_, ItemTags.f_271298_)
            .addTags(Tags.Items.TOOLS_SHIELDS, Tags.Items.TOOLS_BOWS, Tags.Items.TOOLS_CROSSBOWS, Tags.Items.TOOLS_FISHING_RODS, Tags.Items.TOOLS_TRIDENTS);
        m_206424_(Tags.Items.ARMORS_HELMETS).m_255179_(Items.f_42407_, Items.f_42354_, Items.f_42464_, Items.f_42468_, Items.f_42476_, Items.f_42472_, Items.f_42480_);
        m_206424_(Tags.Items.ARMORS_CHESTPLATES).m_255179_(Items.f_42408_, Items.f_42465_, Items.f_42469_, Items.f_42477_, Items.f_42473_, Items.f_42481_);
        m_206424_(Tags.Items.ARMORS_LEGGINGS).m_255179_(Items.f_42462_, Items.f_42466_, Items.f_42470_, Items.f_42478_, Items.f_42474_, Items.f_42482_);
        m_206424_(Tags.Items.ARMORS_BOOTS).m_255179_(Items.f_42463_, Items.f_42467_, Items.f_42471_, Items.f_42479_, Items.f_42475_, Items.f_42483_);
        m_206424_(Tags.Items.ARMORS).addTags(Tags.Items.ARMORS_HELMETS, Tags.Items.ARMORS_CHESTPLATES, Tags.Items.ARMORS_LEGGINGS, Tags.Items.ARMORS_BOOTS);
    }

    private void addColored(Consumer<TagKey<Item>> consumer, TagKey<Item> group, String pattern)
    {
        String prefix = group.f_203868_().m_135815_().toUpperCase(Locale.ENGLISH) + '_';
        for (DyeColor color  : DyeColor.values())
        {
            ResourceLocation key = new ResourceLocation("minecraft", pattern.replace("{color}",  color.m_41065_()));
            TagKey<Item> tag = getForgeItemTag(prefix + color.m_41065_());
            Item item = ForgeRegistries.ITEMS.getValue(key);
            if (item == null || item  == Items.f_41852_)
                throw new IllegalStateException("Unknown vanilla item: " + key.toString());
            m_206424_(tag).m_255245_(item);
            consumer.accept(tag);
        }
    }

    private void copyColored(TagKey<Block> blockGroup, TagKey<Item> itemGroup)
    {
        String blockPre = blockGroup.f_203868_().m_135815_().toUpperCase(Locale.ENGLISH) + '_';
        String itemPre = itemGroup.f_203868_().m_135815_().toUpperCase(Locale.ENGLISH) + '_';
        for (DyeColor color  : DyeColor.values())
        {
            TagKey<Block> from = getForgeBlockTag(blockPre + color.m_41065_());
            TagKey<Item> to = getForgeItemTag(itemPre + color.m_41065_());
            m_206421_(from, to);
        }
        m_206421_(getForgeBlockTag(blockPre + "colorless"), getForgeItemTag(itemPre + "colorless"));
    }

    @SuppressWarnings("unchecked")
    private TagKey<Block> getForgeBlockTag(String name)
    {
        try
        {
            name = name.toUpperCase(Locale.ENGLISH);
            return (TagKey<Block>)Tags.Blocks.class.getDeclaredField(name).get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
        {
            throw new IllegalStateException(Tags.Blocks.class.getName() + " is missing tag name: " + name);
        }
    }

    @SuppressWarnings("unchecked")
    private TagKey<Item> getForgeItemTag(String name)
    {
        try
        {
            name = name.toUpperCase(Locale.ENGLISH);
            return (TagKey<Item>)Tags.Items.class.getDeclaredField(name).get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
        {
            throw new IllegalStateException(Tags.Items.class.getName() + " is missing tag name: " + name);
        }
    }

    @Override
    public String m_6055_()
    {
        return "Forge Item Tags";
    }
}
