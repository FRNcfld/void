package com.frnc.createvoid.wateringrecipes;

import com.frnc.createvoid.CreateVoid;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * 浇水配方类型的注册中心。
 * 使用枚举 + 静态内部类持有 DeferredRegister，确保在 enum 常量构造前完成初始化。
 */
public enum RecipeTypes implements IRecipeTypeInfo {
    WATERING(WateringRecipe::new);

    private final ResourceLocation id;
    private final Supplier<RecipeSerializer<?>> serializerSupplier;
    private final Supplier<RecipeType<?>> typeSupplier;

    // 静态内部类：保证 DeferredRegister 在 enum 常量构造前被初始化
    private static class Registers {
        static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER =
                DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CreateVoid.MOD_ID);
        static final DeferredRegister<RecipeType<?>> TYPE_REGISTER =
                DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, CreateVoid.MOD_ID);
    }

    RecipeTypes(ProcessingRecipeBuilder.ProcessingRecipeFactory<WateringRecipe> factory) {
        String name = name().toLowerCase();
        this.id = new ResourceLocation(CreateVoid.MOD_ID, name);

        RegistryObject<RecipeSerializer<?>> serializerObj = Registers.SERIALIZER_REGISTER.register(
                name, () -> new WateringRecipeSerializer(factory));
        RegistryObject<RecipeType<?>> typeObj = Registers.TYPE_REGISTER.register(
                name, () -> RecipeType.simple(id));

        this.serializerSupplier = serializerObj;
        this.typeSupplier = typeObj;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RecipeSerializer<?>> T getSerializer() {
        return (T) serializerSupplier.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RecipeType<?>> T getType() {
        return (T) typeSupplier.get();
    }

    public Supplier<RecipeType<?>> getTypeSupplier() {
        return typeSupplier;
    }

    public Supplier<RecipeSerializer<?>> getSerializerSupplier() {
        return serializerSupplier;
    }

    /**
     * 在模组初始化时调用，将序列化器和类型注册到 Forge 事件总线。
     */
    public static void register(IEventBus bus) {
        Registers.SERIALIZER_REGISTER.register(bus);
        Registers.TYPE_REGISTER.register(bus);
    }
}
