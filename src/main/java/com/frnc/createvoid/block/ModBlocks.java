package com.frnc.createvoid.block;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.custom.*;
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
import net.minecraft.world.level.material.PushReaction;
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

    /**
     * 虚空传送门方块。
     * <p>
     * 方块属性逐项对齐原版 {@code NETHER_PORTAL}：不可破坏、无碰撞、自发光 11、
     * 玻璃音效、活塞推不动、{@code randomTicks}（{@link VoidPortal#randomTick}
     * 需要它才会被调用）。
     * </p>
     * <p>
     * <b>不注册 BlockItem</b>（用 {@link #registerBlockOnly}）：它只能由框架围成、
     * 用精密构件点燃生成，不该出现在创造模式物品栏里，也不该能被手动放置——否则会
     * 留下悬空的门。与原版下界门一样，它没有物品形态。
     * </p>
     */
    public static final RegistryObject<Block> VOID_BLOCK =
            registerBlockOnly("void_block", () -> new VoidPortal(BlockBehaviour.Properties.of()
                    .noCollission()
                    .randomTicks()
                    .strength(-1.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()));

    //自动合成器
    public static final RegistryObject<Block> CRAFTER =
            registerBlock("crafter", () -> new CrafterBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .strength(0.5f, 3.0f)));

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

    /**
     * 只注册方块，不生成对应的 BlockItem。
     * <p>用于不该被玩家直接持有的方块（如虚空传送门）。</p>
     */
    private static <T extends Block> RegistryObject<T> registerBlockOnly(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
