package com.frnc.createvoid.item;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateVoid.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CREATE_VOID_TAB =
            CREATIVE_MODE_TABS.register("create_void_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.IRON_MECHANISM.get()))
                    .title(Component.translatable("itemGroup.create_void_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.IRON_MECHANISM.get());
                        output.accept(ModItems.INCOMPLETE_BRASS_MACHINE.get());
                        output.accept(ModBlocks.ANDESITE_MACHINE.get());
                        output.accept(ModBlocks.COPPER_MACHINE.get());
                        output.accept(ModBlocks.BRASS_MACHINE.get());
                        output.accept(ModBlocks.REDSTONE_MACHINE.get());
                        output.accept(ModBlocks.CRAFTER.get());
                        output.accept(ModBlocks.VOID_BLOCK.get());
                        output.accept(ModItems.KELP_GEL_BUCKET.get());
                        output.accept(ModItems.LA_VAGUELETTE.get());
                        output.accept(ModItems.CRY_FOR_ME.get());
                    }).build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
