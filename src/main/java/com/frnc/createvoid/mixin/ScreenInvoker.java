package com.frnc.createvoid.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 访问器 mixin：暴露 Screen 的受保护成员。
 * <p>
 * 直接以声明这些成员的 Screen 为目标，避免在 TitleScreen 上 @Shadow 继承自父类的
 * 成员（字段/泛型方法）时解析失败。
 * </p>
 */
@Mixin(Screen.class)
public interface ScreenInvoker {

    // addRenderableWidget 是 protected 泛型方法，必须用 @Invoker 访问。
    // 注意：Mixin AP 无法为泛型 @Invoker 解析混淆映射，生成的 refmap 会缺该条目，
    // 需要在构建时由 patchMixinRefmap 任务补上（见 build.gradle）。
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T createVoid$addRenderableWidget(T widget);

    @Accessor("width")
    int createVoid$getWidth();

    @Accessor("height")
    int createVoid$getHeight();
}
