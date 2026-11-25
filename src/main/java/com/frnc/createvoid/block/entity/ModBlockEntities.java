package com.frnc.createvoid.block.entity;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CreateVoid.MOD_ID);

    public static final RegistryObject<BlockEntityType<CrafterBlockEntity>> CRAFTER =
            BLOCK_ENTITY_TYPES.register("crafter", () ->
                    BlockEntityType.Builder.of(CrafterBlockEntity::new, ModBlocks.CRAFTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<EntityMyriadBlockEntity>> ENTITY_MYRIAD =
            BLOCK_ENTITY_TYPES.register("entity_myriad", () ->
                    BlockEntityType.Builder.of(EntityMyriadBlockEntity::new,
                            ModBlocks.ENTITY_MYRIAD.get(),
                            ModBlocks.ENTITY_MYRIAD_SORTING.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
