# 虚空科技 (CreateVoid)

Minecraft **1.20.1 / Forge** 模组，[Create 机械动力](https://github.com/Creators-of-Create/Create) 的体验优化附属。

- **modId**: `create_void`
- **前置**: Create `6.0.8` ~ `6.1.0`（必装）、Forge `47.x`、Minecraft `1.20.1`
- **许可**: MIT

## 内容

### 1. 浇水系统
让 Create 的 **Spout（注液器）** 把方块"浇"成另一种方块，复用 Create 官方
`BlockSpoutingBehaviour` 注册表，与内置的「泥巴→泥土」走同一条代码路径。

| 输入方块 | 流体 | 产物 | 方式 |
|---|---|---|---|
| 铜块 | 水 250mB | 斑驳的铜块 | 原地替换 |
| 斑驳的铜块 | 水 250mB | 风化的铜块 | 原地替换 |
| 风化的铜块 | 水 250mB | 氧化的铜块 | 原地替换 |
| Create 铜机壳 | 海带胶 250mB | 铜机械 | 原地替换 |
| 远古残骸 | 岩浆 250mB | 下界残骸碎片 ×4 | 消耗方块 + 掉落 |

同一位置只允许转换一次，破坏或重新放置后方可再次浇灌。

### 2. 自动合成器
红石**上升沿**触发一次的 3×3 自动合成台。

- 空手右键打开界面，对齐原版 crafter 布局
- 左/右键点击**空格**可禁用该槽；禁用槽在配方匹配时按空格处理
- **Shift + 点击**可锁定槽位物品类型（槽内清空后仍只接受同类物品）
- 产物优先送入正面邻居的物品容器（Create 管道 / 漏斗），无处安放则向正面抛出
- 比较器输出 = 被禁用槽的数量

### 3. 虚空维度与传送门
虚空维度 `create_void:void` 与主世界 **1 : 1** 坐标（`coordinate_scale = 1.0`），平坦地形。

传送门**按原版下界门逐方法实现**：形状检测、生命周期、渲染都对齐原版
`PortalShape` / `NetherPortalBlock` 的同一套做法。只有两点不同：

- **框架固定为 Create 的列车机壳**（`create:railway_casing`）
- **右手手持 Create 的精密构件**（`create:precision_mechanism`）**右键框架**点燃，
  每次消耗 1 个（创造模式不消耗）

尺寸同原版：内部 `2×3` ~ `21×21`。门方块**没有物品形态**，不会出现在创造模式物品栏；
破坏框架会连带关闭传送门。走进去约 4 秒（生存模式）后传送，中途走出则取消——
与原版下界门一致。

**外观**（三者统一为蓝色，配色取自天境）：

- **门幕纹理**沿用原版下界门那张菱形编织纹，只把配色换成蓝色
  （B 通道恒定 244，R/G 随亮度走），因此花纹与下界门逐像素一致
- **粒子**是本 mod 自己的 `create_void:void_portal`：外形继承原版 `PortalParticle`，
  只覆写取色系数（蓝通道满值、红绿压到 `f * 0.2`，原版下界门是 `f * 0.9 / f * 0.3 / f`）
- **进门时的屏幕扭曲**用 `GuiPortalOverlayMixin` 换成了本门自己的贴图。
  原版 `Gui#renderPortalOverlay` 把方块写死成 `Blocks.NETHER_PORTAL`，
  原版世界里只有一种门所以没问题，本 mod 加了第二种就会永远发紫

**目标维度没有门时会自动新建一座**（内部 `2×3`，与原版最小尺寸一致）。

**Create 的列车可以穿过传送门往返**。经 Create 官方扩展点 `PortalTrackProvider.REGISTRY`
注册门方块，坐标换算与落点复用与玩家传送完全相同的 `VoidPortalTeleporter`，
因此不会出现"人过去了、车却落在别处"。

> 只注册**门方块**，不注册框架（列车机壳）：`getOtherSide` 取的是轨道撞击面所连接的
> 方块，而 `fromProbe` 随后要求目标位置是**同一种方块**——源方块是机壳时这一校验永远
> 不可能通过，注册了也连不上。
>
> 列车走的是探测实体而非玩家，因此传送器对它**只找门、不建门**，与原版一致：
> `Entity#getExitPortal` 只调 `findPortalAround`，只有 `ServerPlayer#getExitPortal`
> 找不到时才 `createPortal`。半径内没有已有的门，列车就是不连通，不会凭空造一座。
> 搜索半径也照搬原版的 `pIsNether ? 16 : 128`（去主世界放宽到 128）。

> 与原版的两点实现差异，都源于"虚空维度不在原版的维度配对里"：
> 原版 `Entity.findDimensionEntryPoint` 只认主世界/地狱/末地，遇到自定义维度直接
> 返回 `null`，所以传送器必须自己实现 `ITeleporter#getPortalInfo`；
> 原版 `Entity.handleNetherPortal` 又把目标维度写死为地狱（照搬会把玩家送进地狱），
> 所以驻留计时与目标维度判定也由本 mod 自己完成。
>
> 目标是二元判定，与原版同构：原版是「地狱 → 主世界，其余 → 地狱」，
> 这里是「虚空 → 主世界，其余 → 虚空」，因此不做来源维度记忆。
>
> 注意：地狱与主世界仍是原版 `8 : 1`，本 mod 不改变该行为。

### 4. 海带胶
粘稠的自定义流体（`density 3000` / `viscosity 6000`），深绿不透明，
不会被其他流体替换。配方：4× 干海带 + 1000mB 水 → 500mB 海带胶（加热混合）。

## 可选依赖：Create Compressed

加载 [Create Compressed](https://www.curseforge.com/minecraft/mc-mods/create-compressed)
后，**安山机械**的配方会自动切换：

| 条件 | 配方 |
|---|---|
| **未加载** Create Compressed | 机械手装配：安山机壳 + 铁构件 → 安山机械 |
| **已加载** Create Compressed | 机械手装配：小齿轮箱（`create_compressed:cogwheel_block`）+ 铁构件 → 安山机械 |

两条配方由 `forge:mod_loaded` 条件互斥启用（见
`data/create/recipes/deploying/andesite_machine*.json`），因此永远只有一条生效。

## 切石配方

四台机械都可以在**切石机**上切回构成它的 Create 部件 —— 是一条回收路径：

- `create_void:andesite_machine` → `create:cogwheel`、`create:gearbox` …（38 条）
- `create_void:brass_machine` → `create:deployer`、`create:mechanical_arm` …（15 条）
- `create_void:copper_machine` → `create:fluid_pipe`、`create:spout` …（18 条）
- `create_void:redstone_machine` → `minecraft:piston`、`minecraft:observer` …（14 条）

共 85 条，按**产物的命名空间**分子目录存放：

| 子目录 | 条数 |
|---|---|
| `create/` | 62 |
| `fluidlogistics/` | 13 |
| `minecraft/` | 9 |
| `create_connected/` | 1 |

涉及 `fluidlogistics:*` 与 `create_connected:*` 的那 14 条带 `forge:mod_loaded` 条件，
对应附属未安装时干净地不加载，不会产生"缺失物品"的警告。

## 构建

需要 **JDK 17**。

```bash
./gradlew build          # 产物在 build/libs/create_void-<version>.jar
./gradlew runClient      # 启动开发客户端
```

> **注意：仓库当前缺少 `gradle/wrapper/gradle-wrapper.jar`。**
> 此前的 `.gitignore` 用 `*.jar` 全局排除，把 wrapper jar 一起排除了，导致干净克隆无法
> 执行 `./gradlew`。该规则已移除，但 jar 需要重新生成并提交：
>
> ```bash
> gradle wrapper --gradle-version 8.8
> git add -f gradle/wrapper/gradle-wrapper.jar
> ```
>
> 在此之前，请使用本机安装的 Gradle 8.8 直接构建。

## 项目结构

```
src/main/java/com/frnc/createvoid/
├── CreateVoid.java          入口：注册表 / 事件 / 网络通道
├── Events/                  客户端事件订阅（界面注册、粒子提供方）
├── block/                   机械方块、自动合成器、VoxelShapes 碰撞箱
├── block/entity/            CrafterBlockEntity（合成、禁用/锁定槽、物品能力）
├── gui/                     自动合成器菜单与界面
├── item/                    物品与创造模式物品栏
├── network/                 C2S 报文：槽位禁用 / 槽位锁定
├── fluid/                   海带胶流体
├── sound/                   音效事件
├── particle/                粒子类型与客户端实现（白烟、虚空门粒子）
├── wateringrecipes/         浇水配方、Spout 行为、转换追踪
├── portal/                  虚空传送门（形状检测、点燃、跨维度传送、POI、Create 列车联动）
├── world/dimension/         虚空维度 ResourceKey
├── jei/                     浇水配方 JEI 分类 + Spout 3D 动画
└── mixin/                   Create 流体行为优化 + 原版传送门屏幕扭曲配色

注：数据生成器（datagen）已移除——它与 Create 6.0.8 冲突，且其输出从未入库。
src/main/resources 下的 blockstate / model / loot table / recipe / lang 全部为手写 JSON。
```

## 鸣谢

[Mantle](https://github.com/Creators-of-Create/Create) / Create 团队提供的 API 与
[Ponder](https://github.com/Creators-of-Create/Ponder)、Flywheel、Registrate。
