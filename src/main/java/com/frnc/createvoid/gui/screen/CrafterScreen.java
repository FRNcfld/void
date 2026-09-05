package com.frnc.createvoid.gui.screen;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.gui.menu.CrafterMenu;
import com.frnc.createvoid.gui.menu.CrafterSlot;
import com.frnc.createvoid.network.CrafterToggleSlotMessage;
import com.frnc.createvoid.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 自动合成器界面（布局对齐原版 crafter_port）：
 * <ul>
 *   <li>顶部中间显示容器标题（合成器）；</li>
 *   <li>左侧 3×3 网格 + 右侧结果预览槽 + 两者间为素材自带的箭头；</li>
 *   <li>禁用槽：被禁用时用 {@code disabled_slot} 红叉盖住且不渲染其中物品；</li>
 *   <li>中部 (97,35) 处根据是否被红石触发显示 powered/unpowered 小灯；</li>
 *   <li>shift 点击空网格槽切换禁用/启用。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {

    private static final ResourceLocation CONTAINER_LOCATION =
            new ResourceLocation(CreateVoid.MOD_ID, "textures/gui/crafter/crafter.png");
    private static final ResourceLocation DISABLED_SLOT_LOCATION =
            new ResourceLocation(CreateVoid.MOD_ID, "textures/gui/crafter/disabled_slot.png");
    private static final ResourceLocation POWERED_REDSTONE_LOCATION =
            new ResourceLocation(CreateVoid.MOD_ID, "textures/gui/crafter/powered_redstone.png");
    private static final ResourceLocation UNPOWERED_REDSTONE_LOCATION =
            new ResourceLocation(CreateVoid.MOD_ID, "textures/gui/crafter/unpowered_redstone.png");

    private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");

    public CrafterScreen(CrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // 标题居中于容器顶部
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(CONTAINER_LOCATION, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    /**
     * 禁用槽：绘制红叉并跳过物品渲染（需 META-INF/accesstransformer.cfg 把 renderSlot 放为 public）。
     * 此方法在 (leftPos, topPos) 平移坐标系下调用，slot.x/y 即容器内坐标。
     */
    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (slot instanceof CrafterSlot && this.menu.isSlotDisabled(slot.getSlotIndex())) {
            guiGraphics.blit(DISABLED_SLOT_LOCATION, slot.x - 1, slot.y - 1,
                    0, 0, 18, 18, 18, 18);
            return;
        }
        super.renderSlot(guiGraphics, slot);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 红石灯（绝对屏幕坐标；等价容器 (97,35)）
        ResourceLocation lamp = this.menu.isPowered() ? POWERED_REDSTONE_LOCATION : UNPOWERED_REDSTONE_LOCATION;
        guiGraphics.blit(lamp, this.leftPos + 97, this.topPos + 35, 0, 0, 16, 16, 16, 16);

        // 悬浮在“启用的空格”上时提示可禁用
        Slot hovered = this.hoveredSlot;
        if (hovered instanceof CrafterSlot && !this.menu.isSlotDisabled(hovered.getSlotIndex())
                && hovered.getItem().isEmpty() && this.menu.getCarried().isEmpty()
                && (this.minecraft.player == null || !this.minecraft.player.isSpectator())) {
            guiGraphics.renderTooltip(this.font, DISABLED_SLOT_TOOLTIP, mouseX, mouseY);
        }
    }

    /**
     * 左/右键单击空的合成网格槽 → 切换禁用状态（再点一下恢复）。
     * 仅当光标为空时拦截；否则放行正常放取。
     */
    @Override
    protected void slotClicked(Slot slot, int slotIndex, int button, ClickType clickType) {
        boolean clickEmptyCursor = clickType == ClickType.PICKUP && this.menu.getCarried().isEmpty();
        if (slot instanceof CrafterSlot && !slot.hasItem()
                && (this.minecraft.player == null || !this.minecraft.player.isSpectator())
                && clickEmptyCursor) {
            updateSlotState(slotIndex, !this.menu.isSlotDisabled(slotIndex));
            return;
        }
        super.slotClicked(slot, slotIndex, button, clickType);
    }

    private void updateSlotState(int slotIndex, boolean disabled) {
        this.menu.setSlotState(slotIndex, disabled);
        ModNetwork.sendToServer(new CrafterToggleSlotMessage(this.menu.containerId, slotIndex, disabled));
    }
}
