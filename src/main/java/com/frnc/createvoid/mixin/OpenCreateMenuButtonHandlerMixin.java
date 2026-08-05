package com.frnc.createvoid.mixin;

import com.simibubi.create.infrastructure.gui.OpenCreateMenuButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 取消 Create 在标题屏自动添加 {@link OpenCreateMenuButton}。
 * <p>
 * Create 通过 {@code ScreenEvent.Init} 的 OpenConfigButtonHandler.onGuiInit 在标题屏和暂停菜单
 * 自动加"Create 主菜单"按钮。标题屏由本 mod 自己添加（与"选项"同行），
 * 这里只取消标题屏分支，暂停菜单不受影响。
 * </p>
 */
@Mixin(value = OpenCreateMenuButton.OpenConfigButtonHandler.class, remap = false)
public abstract class OpenCreateMenuButtonHandlerMixin {

    @Inject(method = "onGuiInit", at = @At("HEAD"), cancellable = true, remap = false)
    private static void createVoid$suppressTitleScreenAutoAdd(ScreenEvent.Init event, CallbackInfo ci) {
        if (event.getScreen() instanceof TitleScreen) {
            ci.cancel();
        }
    }
}
