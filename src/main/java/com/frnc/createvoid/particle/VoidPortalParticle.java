package com.frnc.createvoid.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 虚空传送门的粒子，配色对齐天境（The Aether）。
 *
 * <h3>只有颜色不同</h3>
 * <p>
 * 形状、生命周期、运动轨迹全部继承原版 {@link PortalParticle}，本类只覆写颜色。
 * 对比两者的取色公式（{@code f} 均为 {@code random.nextFloat() * 0.6F + 0.4F}，
 * 即 [0.4, 1.0]）：
 * </p>
 * <table border="1">
 *   <caption>通道系数</caption>
 *   <tr><th></th><th>红</th><th>绿</th><th>蓝</th><th>观感</th></tr>
 *   <tr><td>原版下界门</td><td>{@code f * 0.9}</td><td>{@code f * 0.3}</td><td>{@code f}</td><td>紫</td></tr>
 *   <tr><td>天境</td><td>{@code f * 0.2}</td><td>{@code f * 0.2}</td><td>{@code f}</td><td>蓝</td></tr>
 * </table>
 * <p>
 * 天境把红绿两通道一起压到 0.2、只留蓝通道满值，于是粒子呈纯蓝。这里照搬同一组系数，
 * 与门幕纹理（同为蓝色）配套。
 * </p>
 *
 * <h3>为什么不复用 {@code PortalParticle.Provider}</h3>
 * <p>
 * 原版那个 Provider 构造的是 {@code PortalParticle} 本身，取色公式写死在里面，
 * 所以必须有自己的 Provider 来构造本类。精灵表由粒子 JSON
 * （{@code assets/create_void/particles/void_portal.json}）指定为
 * {@code minecraft:generic_0..7}——与原版下界门用的 {@code minecraft:portal} 不同，
 * 这一点也跟天境保持一致。<b>注意别漏了那个 JSON</b>：缺了它，
 * {@code registerSpriteSet} 会因为找不到纹理集而报错。
 * </p>
 */
@OnlyIn(Dist.CLIENT)
public class VoidPortalParticle extends PortalParticle {

    protected VoidPortalParticle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        // 天境同款配色：蓝通道保留全值，红/绿压到 0.2
        float f = this.random.nextFloat() * 0.6F + 0.4F;
        this.bCol = f;
        this.gCol = f * 0.2F;
        this.rCol = f * 0.2F;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @NotNull Particle createParticle(SimpleParticleType type, ClientLevel level,
                                                double x, double y, double z,
                                                double xSpeed, double ySpeed, double zSpeed) {
            VoidPortalParticle particle =
                    new VoidPortalParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            // pickSprite 是 public，可以直接在这里调用
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
