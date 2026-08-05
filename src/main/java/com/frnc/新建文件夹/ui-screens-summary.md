# C6C Mod — MC 主界面 UI 修改总结（供 AI 读取与工作）

> 本文档总结 C6C mod（`c6c-1.2.5.1`）中对 Minecraft **主菜单/标题屏幕** 与 **世界创建界面** 的 UI 修改。
> 内容基于对已编译 `.class` 文件的字节码反编译分析（`javap -c -p`），面向需要在此代码库上继续开发的 AI 或开发者。

---

## 0. 环境与加载方式

| 项目 | 值 |
|---|---|
| 加载器 | NeoForge（`modLoader="javafml"`, `loaderVersion="[4,)"`） |
| MC 版本 | `[1.21.1,1.22)` |
| Mod | `c6c` v`1.2.5.1` |
| Java | 21 |
| Mixin 配置 | `c6c.mixins.json`（`required: true`, 包 `org.huahua.pr.mixin`, 插件 `org.huahua.pr.MixinPlugin`） |
| 已编译产物 | 该目录为解包后的 class 文件（无 `.java` 源码） |

**mixin 注册位置**：所有 UI 相关 mixin 都注册在 `c6c.mixins.json` 的 `"client"` 区块中（见下），仅客户端生效。

---

## 1. 主菜单 / 标题屏幕修改

### 1.1 `TitleScreenMixin` — 完全替换标题屏幕

- **文件**：`org/huahua/pr/mixin/UI/TitleScreenMixin.class`
- **Mixin 目标**：`net.minecraft.client.gui.screens.TitleScreen`（通过继承 `Screen` + `@Mixin`）
- **注册**：`c6c.mixins.json` → `"client": ["UI.TitleScreenMixin"]`

#### 行为（字节码反编译还原）

**`@Inject(method = "init", at = @At("HEAD"), cancellable = true)`**
- 取消原版 `init()` 全部逻辑（**移除单人/多人/Realms/Mods 等所有原版按钮**）。
- 布局基准 `y0 = height / 4 + 32`。
- 仅新增两个按钮（尺寸 `98 x 20`，并排摆放）：

| 按钮 | 文案 key | 位置（x, y） | 点击行为 |
|---|---|---|---|
| 选项 | `menu.options` | `(width/2 - 100, y0 + 32 + 60)` | `minecraft.setScreen(new OptionsScreen(this, minecraft.options))` |
| 退出游戏 | `menu.quit` | `(width/2 + 2, y0 + 32 + 60)` | `minecraft.stop()` |

**`@Redirect(method = "createNormalMenuOptions")`** — 拦截原版所有正常菜单按钮的 `Button.builder(...)` 创建：
- 将所有被替换掉的按钮（原版逻辑里调用 `createNormalMenuOptions` 生成的）点击行为重定向为一个 lambda：**用系统浏览器打开赞助链接**。
- 链接按语言切换：
  - `zh_cn` → `https://www.xyebbs.com/resources/1116/prom`
  - 其他语言 → `https://www.bisecthosting.com/curseforge?curseforge_project_id=1469136`
- 通过 `Util.getPlatform().openUri(url)` 打开。

> **注意**：反编译中 `init` 内仅见 "选项"/"退出" 两个按钮，而 `redirectCreateNormalMenuOptions` 的作用是把原版 `createNormalMenuOptions` 生成按钮时的 `OnPress` 全部替换为赞助链接处理器——这意味着该 mod 的设计是：主菜单不再展示任何可正常进入游戏的按钮，原版"进入游戏"相关入口被重定向为打开赞助网页。

#### AI 修改指引
- 若需恢复"单人游戏"入口：在 `init` 注入体或 `redirectCreateNormalMenuOptions` 中改为打开 `LevelStorageScreen`/`WorldSelectionList` 相关逻辑，而非赞助 URL。
- 按钮坐标均基于 `width/2` 与 `height/4` 动态计算，修改排版时保持同样的基准。

---

### 1.2 `LogoRendererMixin` — 自定义 Logo 渲染

- **文件**：`org/huahua/pr/mixin/UI/LogoRendererMixin.class`
- **Mixin 目标**：`net.minecraft.client.gui.components.LogoRenderer`
- **注册**：`c6c.mixins.json` → `"client": ["UI.LogoRendererMixin"]`

#### 行为

- 新增私有字段 `boolean keepLogoThroughFade`。
- **`@Inject(method = "renderLogo", at = @At("HEAD"))`** 完全重写 Logo 绘制：
  - `GuiGraphics.setColor(1, 1, 1, alpha)`：当 `keepLogoThroughFade == true` 时 alpha 恒为 `1.0f`（Logo 在淡入淡出期间始终保持不透明）；否则使用方法传入的 `fade` 参数（淡入淡出渐变）。
  - 开启 `RenderSystem.enableBlend()`，分别 blit：
    - `MINECRAFT_LOGO`（尺寸 `256 x 44`），x 居中：`width/2 - 128`
    - `MINECRAFT_EDITION`（尺寸 `256 x 32`），y = 传入 logoY + 44 - 7
  - 绘制后恢复 `setColor(1,1,1,1)` 并 `disableBlend()`。

#### AI 修改指引
- 若想替换成自定义 Logo 贴图：将 `MINECRAFT_LOGO`/`MINECRAFT_EDITION` 换成自定义 `ResourceLocation`，并同步改 blit 尺寸（当前按 256 宽资源裁剪：`u/v=0,0`，`srcW/H` 256x44 / 256x32）。
- `keepLogoThroughFade` 可通过其他 mixin 注入访问器控制（当前仅存在于本类私有字段，未反编译到 getter/setter，如需外部控制需先加 `@Accessor`）。

---

### 1.3 `BrandingControlMixin` — 移除版本/模组水印

- **文件**：`org/huahua/pr/mixin/BrandingControlMixin.class`
- **Mixin 目标**：`net.minecraft.client.Minecraft`（的 branding 计算方法）
- **注册**：`c6c.mixins.json` → `"mixins": ["BrandingControlMixin"]`（客户端/通用区块）

#### 行为

- 新增静态字段 `List<String> brandings`。
- **`@Inject(method = "computeBranding", at = @At("HEAD"), cancellable = true)`**：
  - 将 `brandings` 重置为新的空 `ArrayList`，并 `cancel()`。
  - **效果：标题屏幕左下角的品牌水印（MC 版本、模组数量等）被清空，不再显示。**

#### AI 修改指引
- 若需保留部分品牌信息，可在取消前手动 `brandings.add(...)` 加入自定义字符串。

---

## 2. 世界创建界面修改（SuperFlat）

### 2.1 `CreateWorldScreenMixin` — 平坦世界移除数据包维度

- **文件**：`org/huahua/pr/mixin/SuperFlat/CreateWorldScreenMixin.class`
- **Mixin 目标**：`net.minecraft.client.gui.screens.worldselection.CreateWorldScreen`
- **注册**：`c6c.mixins.json` → `"client": ["SuperFlat.CreateWorldScreenMixin"]`

#### 行为（`@Inject` 于创建世界逻辑，`@Shadow` 字段 `uiState`）

`c6c$createNewWorld(CallbackInfo)` 逻辑链：
1. `uiState.getSettings().selectedDimensions().overworld()` **instanceof `FlatLevelSource`**（即玩家选了"超平坦"预设）且
2. `uiState.getSettings().datapackDimensions()` **instanceof `MappedRegistry`** 时：
3. 基于原 registry 的 `key()` 与 `registryLifecycle()` 构造一个**新的空 `MappedRegistry`**（不复制任何条目）。
4. 用新 registry 替换 `datapackDimensions`，重建一个 `WorldCreationContext`：
   - 参数顺序（构造签名）：`(WorldOptions options, Registry<DimensionType> datapackDimensions, WorldDimensions selectedDimensions, LayeredRegistryAccess<?> worldgenRegistries, ReloadableServerResources dataPackResources, WorldDataConfiguration dataConfiguration)`。
   - 其余参数原样取自 `uiState.getSettings()`。
5. 将 `uiState` cast 为 `WorldCreationUiStateMixin`，调用 `setSettings(新WorldCreationContext)` 应用替换。

> **效果**：创建超平坦世界时，剔除数据包维度注册表中的全部自定义维度，避免平坦预设被附加的维度数据干扰。

### 2.2 `WorldCreationUiStateMixin` — 接口注入（访问器）

- **文件**：`org/huahua/pr/mixin/SuperFlat/WorldCreationUiStateMixin.class`
- **Mixin 目标**：`net.minecraft.client.gui.screens.worldselection.WorldCreationUiState`
- **性质**：**接口型 mixin**（`interface` + 抽象方法，字节码可见 `public abstract void setSettings(WorldCreationContext)`）。

#### 行为
- 通过接口注入为 `WorldCreationUiState` 增加 `setSettings(WorldCreationContext)` 能力，供 `CreateWorldScreenMixin` 在运行时替换 settings。
- 由于是接口注入，通常配合 `@Invoker`/`@Accessor` 风格让目标类实现该方法；实际写回逻辑取决于目标类的 `settings` 字段。

#### AI 修改指引
- 若需要改"超平坦之外"的预设行为，条件判断位于 `CreateWorldScreenMixin.c6c$createNewWorld` 的 `instanceof FlatLevelSource` 分支。
- 重建 `WorldCreationContext` 的构造参数顺序以 1.21.x 为准（上列顺序已从字节码确认）。

---

## 3. 相关 GUI 基础设施（辅助性，非主界面）

| 类 | 作用 |
|---|---|
| `org/huahua/pr/mixin/Tooltips/AbstractContainerScreenMixin.class` | `@Inject(method="renderSlotContents")` 调用 `org.huahua.pr.Content.Client.ItemBorders.renderBorder(PoseStack, Slot)`，为容器物品格渲染稀有度边框。 |
| `org/huahua/pr/Content/Client/ItemBorders.class` | 物品稀有度边框渲染实现。 |
| `org/huahua/pr/GUI/DrawableSprite.class` | 可绘制精灵工具类。 |
| `org/huahua/pr/GUI/SpriteUploader.class` | 自定义精灵纹理上传器。 |
| `org/huahua/pr/GUI/Textures.class` | GUI 纹理资源加载管理。 |
| `org/huahua/pr/GUI/Quest/QuestButton.class`（+内部 `UserInputHandler`） | 任务书 GUI 按钮（含用户输入处理）。 |

### 相关资源文件
| 资源 | 说明 |
|---|---|
| `assets/c6c/atlases/gui.json` | 自定义 GUI 图集注册：`type: directory, source: atlas/gui, prefix: ""` |
| `assets/c6c/textures/atlas/gui/quest_book.png` | 任务书 GUI 纹理（需以 `atlas/gui` 图集打包上传） |
| `assets/c6c/lang/zh_cn.json` / `en_us.json` | 双语本地化（含 `menu.options`、`menu.quit` 等文案的覆盖） |

## 3.5 UI 纹理存放位置速查

| 命名空间 | 路径 | 说明 |
|---|---|---|
| `minecraft`（覆盖原版） | `assets/minecraft/textures/gui/title/minecraft.png`（1024×256） | 主标题 Logo，`LogoRendererMixin` blit 为 `MINECRAFT_LOGO`，实际裁剪 256×44 |
| `minecraft`（覆盖原版） | `assets/minecraft/textures/gui/title/edition.png`（512×64） | 副标题 "Edition"，对应 `MINECRAFT_EDITION`，裁剪 256×32 |
| `minecraft`（覆盖原版） | `assets/minecraft/textures/gui/title/background/panorama_overlay.png`（720×420） | 标题屏全景背景覆盖层 |
| `c6c`（自定义） | `assets/c6c/textures/atlas/gui/quest_book.png`（16×16） | 任务书 GUI 纹理，经 `atlases/gui.json` 注册为 `c6c:gui` 图集 |
| `c6c`（自定义） | `assets/c6c/textures/beam/beam.png`（16×16） | 战利品光束渲染 |
| `curios`（第三方） | `assets/curios/textures/slot/spell_slot.png`（16×16） | Curios 饰品槽位图标（注入 Curios GUI） |

> **规则**：命名空间 `minecraft` → 覆盖原版 UI（`assets/minecraft/textures/gui/`）；命名空间 `c6c` → 自定义 UI（`assets/c6c/textures/atlas/gui/` 走图集，或 `assets/c6c/textures/<子目录>/`）；`curios` → 第三方 mod 槽位纹理。
> 主界面全景背景本体（panorama）未被覆盖，仅覆盖了 `panorama_overlay.png` 覆盖层。

---

## 4. AI 在本代码库上工作的通用要点

1. **无 Java 源码**：目录内只有 `.class`。改代码需重新编译，或先通过 `javap -c -p`/反编译工具（如 CFR/Vineflower）还原 `.java` 再改。
2. **mixin 注册表**：新增/删除 mixin 必须同步编辑 `c6c.mixins.json`（client 区块）。`defaultRequire: 1` 表示所有注入点必须命中，否则崩溃——修改目标方法签名前先核对。
3. **受影响的原版类**（修改时注意与上游版本兼容）：
   - `net.minecraft.client.gui.screens.TitleScreen`
   - `net.minecraft.client.gui.components.LogoRenderer`
   - `net.minecraft.client.Minecraft`（branding）
   - `net.minecraft.client.gui.screens.worldselection.CreateWorldScreen`
   - `net.minecraft.client.gui.screens.worldselection.WorldCreationUiState`
   - `net.minecraft.client.gui.screens.inventory.AbstractContainerScreen`
4. **验证路径**：改动后进入主菜单（看按钮/Logo/水印），进"创建世界"选超平坦预设（看维度剔除），进任意容器界面（看物品边框）。

---

## 5. 附：字节码关键常量备忘

- `TitleScreenMixin`：按钮 `98x20`；选项 `(w/2-100, y0+92)`；退出 `(w/2+2, y0+92)`；`y0 = h/4+32`。
- 赞助 URL：`zh_cn`→`https://www.xyebbs.com/resources/1116/prom`；其他→`https://www.bisecthosting.com/curseforge?curseforge_project_id=1469136`。
- `CreateWorldScreenMixin` 判定：`overworld() instanceof FlatLevelSource` 且 `datapackDimensions() instanceof MappedRegistry`。
