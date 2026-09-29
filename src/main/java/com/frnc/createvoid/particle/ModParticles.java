package com.frnc.createvoid.particle;

import com.frnc.createvoid.CreateVoid;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, CreateVoid.MOD_ID);

    public static final RegistryObject<SimpleParticleType> WHITE_SMOKE =
            PARTICLE_TYPES.register("white_smoke", () -> new SimpleParticleType(false));

    /**
     * 虚空传送门的粒子（蓝色，配色对齐天境）。
     * <p>精灵表见 {@code assets/create_void/particles/void_portal.json}，
     * 客户端实现见 {@link VoidPortalParticle}。</p>
     */
    public static final RegistryObject<SimpleParticleType> VOID_PORTAL =
            PARTICLE_TYPES.register("void_portal", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
