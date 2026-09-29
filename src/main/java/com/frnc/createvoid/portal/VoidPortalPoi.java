package com.frnc.createvoid.portal;

import com.frnc.createvoid.CreateVoid;
import com.frnc.createvoid.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

/**
 * 把虚空传送门方块登记为一种兴趣点（POI）。
 *
 * <h3>为什么必须用 POI 来找门</h3>
 * <p>
 * 原版 {@code PortalForcer#findPortalAround} 靠 {@code PoiManager.getInSquare} 找门，
 * 而那个方法的过滤条件<b>只有 X 和 Z</b>：
 * </p>
 * <pre>
 * return Math.abs(blockpos.getX() - pPos.getX()) &lt;= pDistance
 *     &amp;&amp; Math.abs(blockpos.getZ() - pPos.getZ()) &lt;= pDistance;
 * </pre>
 * <p>
 * <b>Y 完全不参与筛选</b>，Y 只在排序时作为 tie-breaker。这一点是必须的：
 * 目标维度的 Y 与源维度毫无关系（主世界的门建在 y=64，虚空侧自动生成的门在地表
 * y≈1），任何"以实体 Y 为中心上下扫 N 格"的搜索都看不见对面的门，于是每次往返都会
 * 新建一座，门越堆越多。
 * </p>
 * <p>
 * POI 除了天然无视 Y，还有两个好处：数据随区块持久化，且可以在区块只加载到
 * {@code ChunkStatus.EMPTY} 时读取——不必为了找门而做完整的地形生成。
 * </p>
 *
 * <h3>注册顺序是安全的</h3>
 * <p>
 * 下面的 supplier 会取 {@code ModBlocks.VOID_BLOCK}，因此 BLOCKS 必须<b>先于</b>
 * POI_TYPES 完成注册。这不是碰运气：Forge 在
 * {@code GameData#postRegisterEvents} 里按 {@code MappedRegistry.getKnownRegistries()}
 * 的顺序派发 {@code RegisterEvent}，而那个集合是按 {@code BuiltInRegistries} 的
 * <b>声明顺序</b>填充的 {@code LinkedHashSet}——{@code BLOCK} 在第 142 行、
 * {@code POINT_OF_INTEREST_TYPE} 在第 268 行，因此 BLOCKS 必然先派发。
 * </p>
 *
 * <h3>POI 记录如何维护</h3>
 * <p>
 * 不必手动增删：{@code ServerLevel#onBlockStateChange} 会在方块状态变化时比对
 * {@code PoiTypes.forState}，为门方块自动登记记录、门消失时自动移除。存档里
 * <b>此前已存在</b>的门则由 {@code PoiManager#checkConsistencyWithBlocks} 在区块载入时
 * 从方块状态补建记录。
 * </p>
 */
public final class VoidPortalPoi {

    /** 本 POI 类型的键：{@code create_void:void_portal}。 */
    public static final ResourceKey<PoiType> VOID_PORTAL_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, CreateVoid.id("void_portal"));

    private static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, CreateVoid.MOD_ID);

    /**
     * 登记 {@code create_void:void_block} 的全部方块状态（{@code axis=x} 与 {@code axis=z}）。
     * <p>
     * {@code maxTickets = 0}、{@code validRange = 1} 与原版 {@code NETHER_PORTAL} 的
     * 取值一致——这两个参数只影响村民 AI，对传送门没有作用。
     * </p>
     */
    public static final RegistryObject<PoiType> VOID_PORTAL = POI_TYPES.register("void_portal", () -> {
        Set<BlockState> states = Set.copyOf(
                ModBlocks.VOID_BLOCK.get().getStateDefinition().getPossibleStates());
        return new PoiType(states, 0, 1);
    });

    private VoidPortalPoi() {
    }

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
