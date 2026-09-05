package com.frnc.createvoid.datagen;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.ModBlocks;
import com.frnc.createvoid.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModEnUsLangProvider extends LanguageProvider {
    public ModEnUsLangProvider(PackOutput output) {
        super(output, CreateVoid.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // 物品(Items)
        add(ModItems.IRON_MECHANISM.get(), "Iron Mechanism");
        add(ModItems.INCOMPLETE_BRASS_MACHINE.get(), "Incomplete Brass Machine");
        add(ModItems.KELP_GEL_BUCKET.get(), "Kelp Gel Bucket");
        add(ModItems.LA_VAGUELETTE.get(), "La Vaguelette");
        add(ModItems.CRY_FOR_ME.get(), "Cry For Me");
        add("item.create_void.la_vaguette.desc", "Disc — La Vaguelette");
        add("item.create_void.cry_for_me.desc", "Disc — Cry For Me");

        //方块(Blocks)
        add(ModBlocks.VOID_BLOCK.get(), "Void Portal");
        add(ModBlocks.ANDESITE_MACHINE.get(), "Andesite Machine");
        add(ModBlocks.COPPER_MACHINE.get(), "Copper Machine");
        add(ModBlocks.BRASS_MACHINE.get(), "Brass Machine");
        add(ModBlocks.REDSTONE_MACHINE.get(), "Redstone Machine");
        add(ModBlocks.KELP_GEL_BLOCK.get(), "Kelp Gel");
        add(ModBlocks.CRAFTER.get(), "Crafter");
        add("container.create_void.crafter", "Crafter");
        add("gui.togglable_slot", "Click to disable slot");
        add("subtitles.create_void.block.crafter.craft", "Crafter crafts");
        add("subtitles.create_void.block.crafter.fail", "Crafter fails crafting");

        add("itemGroup.create_void_tab", "Void Technology");
        add("message.create_void.portal_cooldown", "Teleport cooling down, %.1fs remaining");
        add("jei.create_void.watering", "Watering");
    }
}
