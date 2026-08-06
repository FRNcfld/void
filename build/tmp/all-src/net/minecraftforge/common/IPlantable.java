/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common;

import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public interface IPlantable
{
    default PlantType getPlantType(BlockGetter level, BlockPos pos) {
        if (this instanceof CropBlock || this == Blocks.f_276665_) return PlantType.CROP;
        if (this instanceof SaplingBlock) return PlantType.PLAINS;
        if (this instanceof FlowerBlock) return PlantType.PLAINS;
        if (this == Blocks.f_50036_)      return PlantType.DESERT;
        if (this == Blocks.f_50196_)       return PlantType.WATER;
        if (this == Blocks.f_50073_)   return PlantType.CAVE;
        if (this == Blocks.f_50072_) return PlantType.CAVE;
        if (this == Blocks.f_50200_)    return PlantType.NETHER;
        if (this == Blocks.f_50359_)     return PlantType.PLAINS;
        return net.minecraftforge.common.PlantType.PLAINS;
    }

    BlockState getPlant(BlockGetter level, BlockPos pos);
}
