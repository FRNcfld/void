package com.frnc.createvoid.gui;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.gui.menu.CrafterMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CreateVoid.MOD_ID);

    public static final RegistryObject<MenuType<CrafterMenu>> CRAFTER_3X3 =
            MENU_TYPES.register("crafter_3x3", () -> new MenuType<>(CrafterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
