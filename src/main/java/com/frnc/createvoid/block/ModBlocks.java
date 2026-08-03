package com.frnc.createvoid.block;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.custom.AndesiteMachineBlock;
import com.frnc.createvoid.block.custom.BrassMachineBlock;
import com.frnc.createvoid.block.custom.CopperMachineBlock;
import com.frnc.createvoid.block.custom.RedstoneMachineBlock;
import com.frnc.createvoid.fluid.ModFluids;
import com.frnc.createvoid.item.ModItems;
import com.frnc.createvoid.portal.VoidPortal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CreateVoid.MOD_ID);

    public static final RegistryObject<Block> ANDESITE_MACHINE =
            registerBlock("andesite_machine", () -> new AndesiteMachineBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .strength(0.5f, 3.0f)));

    public static final RegistryObject<Block> COPPER_MACHINE =
            registerBlock("copper_machine", () -> new CopperMachineBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .strength(0.5f, 3.0f)));

    public static final RegistryObject<Block> BRASS_MACHINE =
            registerBlock("brass_machine", () -> new BrassMachineBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .strength(0.5f, 3.0f)));

    public static final RegistryObject<Block> REDSTONE_MACHINE =
            registerBlock("redstone_machine", () -> new RedstoneMachineBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .strength(0.5f, 3.0f)));

    public static final RegistryObject<Block> VOID_BLOCK =
            registerBlock("void_block", () -> new VoidPortal(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .strength(1.0f, 3.0f)));

    // 流体方块（不需要 BlockItem，通过水桶放置）
    public static final RegistryObject<LiquidBlock> KELP_GEL_BLOCK =
            BLOCKS.register("kelp_gel", () -> new LiquidBlock(
                    () -> (FlowingFluid) ModFluids.KELP_GEL.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .noLootTable()
                            .strength(100.0F, 3.0F)
                            .replaceable()
                            .sound(SoundType.EMPTY)
            ));

    private static <T extends Block> void registerBlockItems(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> blocks = BLOCKS.register(name, block);
        registerBlockItems(name, blocks);
        return blocks;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
