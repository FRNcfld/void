package com.frnc.createvoid.fluid;

import com.frnc.createvoid.CreateVoid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, CreateVoid.MOD_ID);

    // 注册流体类型
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, CreateVoid.MOD_ID);

    // 定义流体类型
    public static final RegistryObject<FluidType> KELP_GEL_TYPE =
            FLUID_TYPES.register("kelp_gel", () -> new KelpGelFluidType(
                    FluidType.Properties.create()
                            .density(3000)
                            .viscosity(6000)
                            .temperature(300)
                            .lightLevel(0)
                            .canExtinguish(true) // 像水一样能熄灭火焰（Forge 会把它接入实体灭火逻辑）
                            .descriptionId("block.create_void.kelp_gel")
            ));

    // 静态流体
    public static final RegistryObject<Fluid> KELP_GEL =
            FLUIDS.register("kelp_gel", () -> new KelpGelFluid.Source());

    // 流动流体
    public static final RegistryObject<Fluid> FLOWING_KELP_GEL =
            FLUIDS.register("flowing_kelp_gel", () -> new KelpGelFluid.Flowing());
}
