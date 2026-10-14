> 2026-10-14 更新：本文保留为早期移植计划。当前玩家陆地重力已加入 Mixin 六方向物理；现状、验证和未完成范围请先阅读 [重力修复记录](gravity-fix-2026-10-14.md)。

# StarMiner 移植计划

从 Minecraft 1.7.10 / Forge 移植到 **1.21.1 / NeoForge 21.1.259**。
本仓库是**干净重写**：解包出的字节码只当行为规格，不逐行搬运。

- 工程：`D:\Deepseek\StarMiner`，包名 `com.qince.starminer`，modid `starminer`
- 参考树（只读）：`D:\Deepseek\tools\reference`（Vineflower 反编译 `Starminer1710-0.9.9.jar`）
- 构建：`gradlew build` / `runData` / `runClient` / `runServer`，需要 JDK 21（本机 `D:\Java\java21`）

---

## 一、切片 1 交付状态

| 功能 | 状态 |
|---|---|
| 重力核心方块 + 方块实体（28 格库存，NBT 键沿用 `stmn.*`） | 已实现 |
| 核心 GUI（贴图实测对齐；5 行读数 + 7 个控件） | 已实现 |
| 双向增减控件（左键 +1 / 右键 -1 / Shift ×10 / Ctrl ×100） | 已实现 |
| 建造速度三档（0.5 / 4 / 64 格每秒） | 已实现 |
| 重力场索引（按区块；只存坐标，参数实读） | 已实现 |
| 5 种场形态 in-range 判定 | 已实现 |
| 6 档引力方向（枚举 + 主导轴 + 迟滞缓冲） | 已实现，**G-1 已修** |
| 玩家重力效果 | 部分实现：仅垂直轴真正生效 |
| HUD 方向提示 | 已实现 |
| 星壳生成（层迭代游标，可续跑，无尺寸上限） | 已实现 |
| 战利品表 / 中英本地化（datagen） | 已实现 |

**验证**：`gradlew build` 通过；单元测试全绿（含锁定轴向约定的新断言）；`runServer` 正常启动；
`runClient` 资源加载零警告。

---

## 二、引力方向的轴向约定

**已用解包原版 `TileEntityGravityGenerator.getGravityDirection` 确证。** 设 `xRel/yRel/zRel`
为实体相对核心球心的偏移：

| 偏移 | 原版返回 | 物理含义 | 命名描述的是 |
|---|---|---|---|
| `xRel > 0`（实体在东侧） | `eastTOwest_XN` | 向西掉 | **被拉向哪** |
| `zRel > 0`（实体在南侧） | `southTOnorth_ZN` | 向北掉 | 被拉向哪 |
| `yRel > 0`（实体**在上方**） | `upTOdown_YN` | **向下**掉 | **身体在哪** |
| `yRel < 0`（实体**在下方**） | `downTOup_YP` | **向上**掉 | 身体在哪 |

三条推论：

1. **约定不统一**，且必须照搬：X/Z 的名字说"被拉向哪"，Y 的名字说"身体在哪"。
   按 X 的方式读 Y，会让站在星球上方的人**往上掉**，整个星球玩法失效。
2. `GravityDirection.vector()` 返回**拉力向量**（向下掉是 `(0,-1,0)`），与"名字描述什么"无关。
3. 只有 `yRel > 0 → upTOdown_YN` 能让"普通重力"成为默认值——这是原版这么设计的理由。

**G-1 已修复**：实现集中到 `GravityDirection.axisDirection` 一处，配 3 组断言
（映射表、6 档向量、主导轴）把它钉死。

---

## 三、重力系统缺陷清单

| 编号 | 缺陷 | 影响 | 状态 |
|---|---|---|---|
| G-1 | Y 轴映射反转 | **核心玩法失效** | **已修复** |
| G-2 | 侧向重力只是"抵消垂直重力" | ±X/±Z 场里玩家浮在原地 | 见 4.3 |
| G-3 | 相机与第三人称朝向不跟重力旋转 | 天花板/墙上视角与操作错乱 | 见 4.2 |
| G-4 | 掉落伤害按垂直轴结算 | 倒挂时下落伤害不合理 | 见 4.2 |
| G-5 | 判定方向用 `getEyeY()`，与 `GravityHelper.samplePosition` 的面心基准不一致 | 两处基准不同 | 见 4.4 |
| G-6 | 重力修饰符对所有生物生效 | 场内的牛/村民也受影响，与"只做玩家"的范围决定冲突 | 见 4.4 |
| G-7 | 每 tick 场查询未做预算 | 生物多时有 `getBlockEntity` 开销 | 见 4.4 |

---

## 四、下一阶段：完善重力系统

### 4.0 硬约束（已核实源码，非推测）

调研覆盖 `LivingEntity.travel`、`Entity.move`、`Entity.collide`、相机链路、
掉落伤害链路与全部 NeoForge 事件，结论如下：

| 能力 | 是否可用 | 证据 |
|---|---|---|
| 相机朝向/滚转 | ✅ **官方事件可用** | `ViewportEvent.ComputeCameraAngles`（get/set yaw/pitch/roll），由 `Camera` 在计算视角时触发 |
| 掉落伤害数值 | ✅ **官方事件可用** | `LivingFallEvent`：`getDistance/setDistance`、`getDamageMultiplier/setDamageMultiplier`、可取消 |
| 客户端移动输入 | ✅ 官方事件可用 | `MovementInputUpdateEvent`：`getInput()` 可变引用 |
| 修改"重力方向/移动" | ❌ **没有任何事件** | 全量扫描无 `TravelEvent`/`GravityEvent`/`MovementEvent`/`JumpEvent` |
| 覆盖 `LivingEntity.travel()` | ⚠️ 方法非 final，但**只对自己 new 的实体子类有效**，无法用于原版 `Player` | `LivingEntity.java:2219` |
| 替换"是否站在地面上" | ❌ 私有 | `Entity.move` 内 `verticalCollisionBelow = verticalCollision && pos.y < 0.0`（`Entity.java:661`） |
| 台阶/支撑块/摩擦/击退/流体高度 | ❌ 私有或硬编码 -Y | `Entity.collide`、`collectCandidateStepUpHeights`、`checkSupportingBlock`、`getOnPos` 家族、`updateFluidHeightAndDoFluidPushing` |
| 掉落距离累积 | ❌ 私有 | `Entity.java:1207` `else if (y < 0.0) fallDistance -= (float)y` |

**因此：真正的侧向重力（人站在竖直墙面上、能沿墙面走）在不改核心类的前提下无法完成。**
只有"沿重力加速度移动"这一层可以非侵入地做到，而"站得住/算不算落地/台阶/摩擦"都必须注入。

### 4.1 G-1 方向与几何（已完成）

已修映射并把约定集中到单点 + 断言。

### 4.2 G-3 + G-4：表现层对齐（**全程无需 Mixin，建议先做**）

- **相机**：监听 `ViewportEvent.ComputeCameraAngles`，按当前 gravity 方向注入 yaw/pitch/roll，
  使"上"始终背离重力。等价于原版 `TransformClientHelper.orientCameraByGravity`。
  注意第三人称的翻转在事件**之后**应用（`Camera` 里 `yRot + 180`），roll/pitch 运算要一并考虑。
- **玩家模型朝向**：当前无官方事件，先只做相机；第三人称身体朝向列入 4.5 的 Mixin 评估。
- **掉落伤害**：监听 `LivingFallEvent`，把 `distance` 与 `damageMultiplier` 按
  `−Δposition · gravityVector` 重算；并在 `EntityTickEvent` 里按需要重置 `fallDistance`
  （因为原版的累积只认真实 Y 位移）。
- **验收**：倒挂/侧向场里下落的伤害与视觉方向一致；相机在天花板上"上"朝下。

### 4.3 G-2：侧向重力的加速度（非侵入版，**有明确上限**）

非侵入方案：玩家进入非垂直场时，把 `Attributes.GRAVITY` 置 0 抑制原版 Y 拉力，
再由模组每 tick 沿 gravity 向量给 `deltaMovement` 施加加速度。

**必须自己补的两件事**（原版靠 Y 分支做的）：
1. **终端速度阻尼**：原版地面分支对 Y 单独乘 `0.98`（`travel` 里 `d2 * 0.98F`），
   抑制后可继承原版做法，给出等效终端速度；否则会无限加速。
2. **落地判定**：`onGround` 由 `pos.y < 0` 推导，侧向时永不成立 → 没有摩擦。
   只能启发式检测"沿重力轴的碰撞"，会有误差。

**上限与取舍**：此方案能让玩家在 ±X/±Z 场里被推着走、相机也对，但**站不稳、无摩擦、
台阶与蹲行边缘保护失效**。是否可接受需要你定。若要真正"站在墙上走路"，必须走 4.5。

### 4.4 G-5 / G-6 / G-7 收敛（低风险，可与 4.2 并行）

- 统一采样点基准：只保留 `GravityHelper.samplePosition`，`GravityService` 不再用 `getEyeY()`
- 效果范围收窄到玩家，或加配置开关（`gravity.affectMobs`）
- 场查询加预算/缓存，避免每 tick 重复 `getBlockEntity`

### 4.5 需要决策：是否引入 Mixin

**Mixin 基础设施已就绪**：`src/main/resources/starminer.mixins.json` 已存在、
`neoforge.mods.toml` 已声明 `[[mixins]]`、runClient 日志已确认 mixin 配置被加载。
目前 `mixins` 数组为空，加入不需要额外搭架子。

若选 Mixin，需注入的点（对应 4.0 的私有项）：

| 注入目标 | 目的 |
|---|---|
| `Entity.move` / `Entity.collide` | 让"地面"= 重力反方向的碰撞 |
| `Entity.checkFallDamage` + 其调用点 | 按重力向量累积落差 |
| `Entity.checkSupportingBlock` / `getOnPos` 家族 | 支撑块、摩擦、跳跃因子按重力方向取 |
| `LivingEntity.jumpFromGround` / `aiStep` 的跳跃触发 | 跳跃沿重力反向 |
| `Entity.updateFluidHeightAndDoFluidPushing` | 流体高度以重力方向为底 |
| `Player.maybeBackOffFromEdge` | 蹲行边缘保护按重力方向 |

**代价**：与其他改移动的模组冲突风险显著上升；每次游戏/NeoForge 更新都要重测。
**建议**：先把 4.2 做完（无风险、能明显改善观感），再用 4.3 试探手感；
真觉得"站不住"是硬伤，再立第四个切片专门做 Mixin。

---

## 五、验收标准

每个工作项必须给出：`gradlew build` 通过、单元测试通过、`runClient` 手测步骤与预期。

**凡涉及渲染/交互/网络的改动，禁止只验证"能启动"。** 本项目已经因为只验证启动而发生 3 次
实际缺陷（菜单包越界崩溃、按钮数组越界崩溃、面板文字溢出）。渲染与几何类常量一律进
`GuiLayout` 并配单元测试断言。
