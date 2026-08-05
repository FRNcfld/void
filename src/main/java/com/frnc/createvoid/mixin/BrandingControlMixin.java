package com.frnc.createvoid.mixin;

import net.minecraftforge.internal.BrandingControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

/**
 * 移除标题屏的品牌水印（精简页面）。
 * Forge 1.20.1 的版本/模组水印由 BrandingControl.forEachLine(...) 绘制（左下角：
 * Forge 版本 / Minecraft 版本 / MCP / 模组数量），右下角的 Forge 状态行由
 * forEachAboveCopyrightLine(...) 绘制。两个都置为 no-op。
 */
@Mixin(value = BrandingControl.class, remap = false)
public abstract class BrandingControlMixin {

    @Inject(method = "forEachLine", at = @At("HEAD"), cancellable = true, remap = false)
    private static void createVoid$noLeftBranding(boolean includeMC, boolean reverse,
                                                  BiConsumer<Integer, String> lineConsumer, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "forEachAboveCopyrightLine", at = @At("HEAD"), cancellable = true, remap = false)
    private static void createVoid$noRightBranding(BiConsumer<Integer, String> lineConsumer, CallbackInfo ci) {
        ci.cancel();
    }
}
