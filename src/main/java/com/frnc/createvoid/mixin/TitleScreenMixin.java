package com.frnc.createvoid.mixin;

import com.simibubi.create.infrastructure.gui.OpenCreateMenuButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.TitleScreenModUpdateIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 精简原版主菜单，4 行 1 列布局：
 * <pre>
 *     单人游戏
 *     多人游戏
 *  [Create] 选项
 *     退出游戏
 * </pre>
 * 取消原版 init() 的全部逻辑（移除 Realms / Mods / 语言 / 辅助功能 / 版权等）。
 * Create 的按钮由本 mixin 手动添加在"选项"行，并通过
 * {@link OpenCreateMenuButtonHandlerMixin} 取消 Create 自身的自动添加。
 * <p>
 * 不在此 mixin 内 @Shadow 任何继承自 Screen 的成员（Mixin 对继承成员解析有限），
 * 统一通过 {@link ScreenInvoker}（@Invoker/@Accessor）访问，或使用 Minecraft.getInstance()。
 * </p>
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Shadow(remap = false)
    private TitleScreenModUpdateIndicator modUpdateNotification;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void createVoid$customInit(CallbackInfo ci) {
        ci.cancel();

        Minecraft minecraft = Minecraft.getInstance();
        ScreenInvoker screen = (ScreenInvoker) (Object) this;
        int width = screen.createVoid$getWidth();
        int height = screen.createVoid$getHeight();

        // 布局基准，与原版 height / 4 + 32 一致；行间距 24；4 个主按钮水平居中
        int y0 = height / 4 + 32;
        int x = width / 2 - 49;

        // 第 1 行：单人游戏
        screen.createVoid$addRenderableWidget(Button.builder(Component.translatable("menu.singleplayer"), button ->
                        minecraft.setScreen(new SelectWorldScreen((TitleScreen) (Object) this)))
                .bounds(x, y0, 98, 20)
                .build());

        // 第 2 行：多人游戏（沿用原版跳过警告逻辑）
        screen.createVoid$addRenderableWidget(Button.builder(Component.translatable("menu.multiplayer"), button -> {
                    Screen target = minecraft.options.skipMultiplayerWarning
                            ? new JoinMultiplayerScreen((TitleScreen) (Object) this)
                            : new SafetyScreen((TitleScreen) (Object) this);
                    minecraft.setScreen(target);
                })
                .bounds(x, y0 + 24, 98, 20)
                .build());

        // 第 3 行：Create 主菜单按钮 + 选项（Create 在选项左侧）
        screen.createVoid$addRenderableWidget(new OpenCreateMenuButton(x - 22, y0 + 48));
        screen.createVoid$addRenderableWidget(Button.builder(Component.translatable("menu.options"), button ->
                        minecraft.setScreen(new OptionsScreen((TitleScreen) (Object) this, minecraft.options)))
                .bounds(x, y0 + 48, 98, 20)
                .build());

        // 第 4 行：退出游戏
        screen.createVoid$addRenderableWidget(Button.builder(Component.translatable("menu.quit"), button ->
                        minecraft.stop())
                .bounds(x, y0 + 72, 98, 20)
                .build());

        // Forge 补丁的 render() 会无条件调用 modUpdateNotification.render(...)，而该字段只在原版
        // init() 中赋值。取消 init 后必须补上，否则淡入完成后 render 直接 NPE。
        // 传入 null modButton 时 showNotification 保持 null，其 render() 会提前返回，无副作用。
        this.modUpdateNotification = TitleScreenModUpdateIndicator.init((TitleScreen) (Object) this, null);
    }
}
