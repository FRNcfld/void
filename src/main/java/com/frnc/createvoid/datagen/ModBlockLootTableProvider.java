package com.frnc.createvoid.datagen;

import com.frnc.createvoid.block.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.VOID_BLOCK.get());
        dropSelf(ModBlocks.ANDESITE_MACHINE.get());
        dropSelf(ModBlocks.COPPER_MACHINE.get());
        dropSelf(ModBlocks.BRASS_MACHINE.get());
        dropSelf(ModBlocks.REDSTONE_MACHINE.get());
        dropSelf(ModBlocks.CRAFTER.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
