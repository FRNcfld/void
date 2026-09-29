package com.frnc.createvoid.mixin;

import com.frnc.createvoid.portal.VoidPortal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让进门时的屏幕扭曲用<b>玩家实际所在那座门</b>的贴图。
 *
 * <h3>原版为什么永远发紫</h3>
 * <p>
 * 屏幕扭曲（{@code spinningEffectIntensity}）由 {@code LocalPlayer.handleNetherPortalClient}
 * 累计，真正绘制的是 {@code Gui#renderPortalOverlay}。而那个方法把方块写死了：
 * </p>
 * <pre>
 * TextureAtlasSprite sprite = this.minecraft.getBlockRenderer().getBlockModelShaper()
 *         .getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
 * </pre>
 * <p>
 * 取的是<b>原版下界门</b>模型的 {@code particle} 贴图，与玩家身处哪座门无关。
 * 原版世界里只有一种门，写死没问题；本 mod 加了第二种门，于是进虚空门也照样发紫。
 * </p>
 *
 * <h3>改法</h3>
 * <p>
 * 包住那次 {@code getParticleIcon} 调用，把参数换成玩家实际所在的门方块状态
 * （贴图由我们方块模型里的 {@code particle} 指定为 {@code void_portal}，即蓝色那张）。
 * 不在虚空门里时原样返回，地狱门的行为完全不变。
 * </p>
 * <p>
 * 注意这个 Mixin 打的是<b>原版类</b>，所以 {@code remap} 保持默认的 {@code true}，
 * 依赖 refmap 把方法名映射到 SRG；{@code Gui} 是客户端类，因此注册在 Mixin 配置的
 * {@code client} 段而不是 {@code mixins} 段。
 * </p>
 */
@Mixin(Gui.class)
public abstract class GuiPortalOverlayMixin {

    @WrapOperation(
            method = "renderPortalOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockModelShaper;"
                            + "getParticleIcon(Lnet/minecraft/world/level/block/state/BlockState;)"
                            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private TextureAtlasSprite createvoid$useActualPortalTexture(
            BlockModelShaper shaper, BlockState state, Operation<TextureAtlasSprite> original) {
        return original.call(shaper, createvoid$actualPortalState(state));
    }

    /**
     * 玩家正身处虚空门时返回那座门的方块状态，否则原样返回。
     *
     * <p>
     * 只认虚空门：没压在任何门方块上（例如真的在地狱门里）时返回 {@code fallback}，
     * 即原版那个写死的下界门状态。
     * </p>
     */
    private static BlockState createvoid$actualPortalState(BlockState fallback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return fallback;
        }
        BlockPos pos = VoidPortal.findPortalBlockAt(player.level(), player);
        return pos == null ? fallback : player.level().getBlockState(pos);
    }
}
