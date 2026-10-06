package com.frnc.createvoid.item;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.fluid.ModFluids;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreateVoid.MOD_ID);

    public static final RegistryObject<Item> IRON_MECHANISM =
            ITEMS.register("iron_mechanism", ()->new Item(new Item.Properties()));

    public static final RegistryObject<Item> INCOMPLETE_BRASS_MACHINE =
            ITEMS.register("incomplete_brass_machine", ()->new Item(new Item.Properties()));

    public static final RegistryObject<Item> KELP_GEL_BUCKET =
            ITEMS.register("kelp_gel_bucket", () -> new BucketItem(
                    ModFluids.KELP_GEL,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
