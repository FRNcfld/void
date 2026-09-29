package com.frnc.createvoid.portal;

import com.frnc.createvoid.block.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 虚空传送门的框架方块：<b>Create 的列车机壳</b>（{@code create:railway_casing}）。
 *
 * <h3>与原版的差异</h3>
 * <p>
 * 原版 {@code PortalShape} 把框架写死成黑曜石——{@code FRAME} 是 private static 的
 * {@code StatePredicate}，外部无法替换。而这个框架是 <b>Create 的方块</b>，
 * 我们既改不了它的类，也不想为此往 Forge 的 {@code IForgeBlock#isPortalFrame}
 * 默认方法里塞 Mixin。因此 {@link VoidPortalShape} 把框架谓词做成了构造参数，
 * 由本类提供。
 * </p>
 * <p>
 * 收敛成<b>单一材料</b>是有意的（与原版固定黑曜石、天境固定荧石同理）：
 * 框架材料越自由，"门平面朝向 + 框架完整性"的判定越容易在边缘情况下出错。
 * </p>
 */
public final class VoidPortalFrame {

    /** 固定框架方块：Create 的列车机壳。 */
    public static final ResourceLocation RAILWAY_CASING_ID =
            new ResourceLocation("create", "railway_casing");

    private VoidPortalFrame() {
    }

    /**
     * 按 id 惰性取方块。
     * <p>不缓存成静态字段：写死成静态字段会在 Create 缺失时直接抛空指针，
     * 按 id 查询天然安全，也符合可选依赖的语义。</p>
     */
    private static Block railwayCasing() {
        return ForgeRegistries.BLOCKS.getValue(RAILWAY_CASING_ID);
    }

    /** 该方块状态能否作为传送门框架。 */
    public static boolean isFrame(BlockState state) {
        Block casing = railwayCasing();
        return casing != null && state.is(casing);
    }

    /** 供 {@link VoidPortalShape} 使用的谓词。 */
    public static Predicate<BlockState> predicate() {
        return VoidPortalFrame::isFrame;
    }

    /**
     * 自动生成传送门时使用的框架方块状态。
     * <p>必须与 {@link #isFrame} 判定的一致，否则生成出来的门会立刻自我删除。</p>
     *
     * @return 框架的默认状态；Create 缺失（取不到方块）时返回 {@code null}
     */
    @Nullable
    public static BlockState railwayCasingState() {
        Block casing = railwayCasing();
        return casing == null ? null : casing.defaultBlockState();
    }

    /**
     * 传送门方块本身。
     * <p>放在这里是因为 {@link VoidPortalShape} 判定"内部格"时需要它，
     * 而形状检测不应依赖方块注册的顺序。</p>
     */
    public static Block portalBlock() {
        return ModBlocks.VOID_BLOCK.get();
    }
}
