package com.frnc.createvoid.datagen;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelsProvider extends ItemModelProvider {
    public ModItemModelsProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CreateVoid.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.IRON_MECHANISM.get());
        basicItem(ModItems.INCOMPLETE_BRASS_MACHINE.get());
        basicItem(ModItems.KELP_GEL_BUCKET.get());
        basicItem(ModItems.LA_VAGUELETTE.get());
        basicItem(ModItems.CRY_FOR_ME.get());
    }
}
