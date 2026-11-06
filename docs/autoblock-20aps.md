# Grim AC 检测机制分析与 20 APS Full AutoBlock 方案

> 依据：`D:\Windows\lib\Grim-2.0 (1)` 源码（Grim 2.0）。所有结论都标注了对应的类/方法，便于复核。

---

## 1. Grim 的判定骨架：tick 窗口模型

| 事实 | 源码位置 | 含义 |
|---|---|---|
| 窗口边界 = 任意 `ServerboundMovePlayerPacket`；1.21.2+ 无移动包时用 `CLIENT_TICK_END` | `checks/GrimProcessor#isTickPacket` | 一个客户端 tick ≈ 一个窗口（正常情况下） |
| 收到窗口边界时，`attacking/using/placing/interacting/releasing/digging/picking` 全部清零 | `checks/impl/packetorder/PacketOrderProcessor#onPacketReceive` | 检查只比较"同一窗口内"的包 |
| `isRightClicking() = placing \|\| using \|\| interacting` | 同上 | 只有 `USE_ITEM / PLACE / INTERACT_ENTITY` 会置位 |

### 关键检查判据

| 检查 | 触发条件 | experimental |
|---|---|---|
| `PacketOrderI` | ① 窗口内 `USE/PLACE` 后出现 `ATTACK`；② `RELEASE_USE_ITEM` 与 `attack/place/use/pick/dig` 同窗口 | 是 |
| `PacketOrderJ` | `USE_ITEM/PLACE` 到达时 `isAttacking() && !isInteracting()` | 是 |
| `PacketOrderM` | `USE_ITEM` 后同窗口出现实体 `INTERACT` | 是 |
| `MultiActionsA` | `isSlowedByUsingItem()` 且 `lastSlotSelected == usingSlot` 时收到 `ATTACK` | 是 |
| `Simulation`(OffsetHandler) | 预测位移 offset ≥ 0.001（immediate 0.1），decay 0.02 | 否 |
| `NoSlow` | `isSlowedByUsingItem()` 且连续 2 拍 offset > 0.001 | 否 |

> `experimental` 检查只在 `player.isExperimentalChecks()` 为真时记账（`checks/Check#recordFlag`），配置项 `experimental-checks` 默认 **false**。

### 减速预测（Block Slow 的根）

| 事实 | 源码位置 |
|---|---|
| 收到 `USE_ITEM` 且 `ItemBehaviour.canUse()` 为真，才置 `slowedByUsingItem = true` | `events/packets/PacketPlayerDigging#handleUseItem` |
| 剑在"客户端 ≥1.21.4 且服务端 <1.9"时判定为可用（ViaVersion 场景） | `utils/item/LegacyItem#canUse` |
| 预测时 `input = input.scale(0.98).scale(0.2)`（组件缺失时 0.2） | `predictionengine/predictions/input/impl/ModernInputTransformer#modifyInput` |

**结论：只要服务端认为你在用剑格挡，Grim 就按 0.2 预测你的位移。客户端不减速 ⇒ 每拍 offset ≈ 0.8×速度 ⇒ `Simulation` 与 `NoSlow` 同时爆。**

---

## 2. 由机制推出的四条不变量

1. **一窗一包**：`attack / use / place / release / dig` 在同一个移动包区间内最多出现一个。
2. **攻击前必须已经释放**（仅当 `MultiActionsA` 生效，即服务端认剑为可用物品）：否则每拍 flag。
3. **减速必须与 `slowedByUsingItem` 同拍**：`use` 发包与本地 0.2 翻转必须在同一 tick 头部完成。
4. **所有动作都在 tick 头部**（原版 `Minecraft.tick → handleKeybinds` 的位置，移动包之前），否则窗口归属会与本地状态错拍。

---

## 3. 能力矩阵

| 服务端认剑可格挡 | `experimental-checks` | 可达 APS | Block Slow | 说明 |
|---|---|---|---|---|
| 是（1.8 + Via） | 开 | **6.67** | 必开 | `MultiActionsA` 挡死，一次命中必须占 3 个窗口 |
| 是（1.8 + Via） | 关 | **20** | 必开 | 20 APS Full AB 的目标形态 |
| 否（现代服务端） | 任意 | 20 | **必须关** | 剑不会置 `slowedByUsingItem`，开了必爆 Simulation；格挡无实际减伤 |

20 APS 与"真实减伤 + 实验检查全开"不可兼得，这是 Grim 判据本身决定的，不是实现问题。

---

## 4. 20 APS Full AB 方案

### 4.1 核心参数

| 参数 | 取值 | 作用 |
|---|---|---|
| `Server Mode` | `Legacy18` / `Modern` | 决定 Block Slow 与是否强制 release |
| `Target APS` | 20（=1 包/tick） | 攻击窗口预算 |
| `Ignore Cooldown` | 开 | 绕过 `getAttackStrengthScale`（否则剑被 1.6 APS 锁死） |
| `Block Slow` | 开，系数 **0.2** | 举盾期输入乘 0.2 |
| `Strict Order` | 开 | 一窗一包 |
| `Soft Refresh` | 机会式（空窗补 `use`） | 不掉 APS 的刷新 |
| `Hard Refresh` | 事件驱动（受伤 / 换目标 / 切物品 / 传送） | `release → use`，占 2 个窗口 |
| `Block Range` | 4.0 | 进入/退出格挡的判定距离 |

### 4.2 逐拍时序（tick 头部，每拍只执行一个分支）

| 优先级 | 条件 | 发出的包 | 本拍 APS |
|---|---|---|---|
| 1 | `DROP_PENDING` 或 `!wantsBlock()` | `RELEASE_USE_ITEM` | 0 |
| 2 | `HARD_REFRESH` 步骤 A | `RELEASE_USE_ITEM` | 0 |
| 3 | `HARD_REFRESH` 步骤 B 或 `!serverUsing` | `USE_ITEM` | 0 |
| 4 | `attackReady()` | `ATTACK` + `SWING` | **1** |
| 5 | 其它（攻击没发出） | `USE_ITEM`（机会式软刷新） | 0 |

- 分支 4 命中即 20 APS；分支 5 只在被打断（超出射程、冷却、窗口被占）时补盾，不额外消耗攻击窗口。
- 移动包仍由 `MixinClientPlayerEntity` 在 `aiStep` 末尾发出，天然把相邻两拍隔成两个窗口。

### 4.3 触发条件

| 事件 | 动作 |
|---|---|
| 目标进入 `blockRange` 且主手为剑 | `ARM`：立即 `USE_ITEM`，`serverUsing = true` |
| `mc.player.hurtTime > 0`（挨打） | `HARD_REFRESH`：下一拍 `release`，再下一拍 `use`（服务端很可能刚清掉 using） |
| 目标切换 / 距离越界 / 换非剑物品 | `DROP`：单独一拍 `release` |
| `attackReady()` | `ATTACK`（窗口空闲时） |
| 攻击窗口被其它模块占用 | 顺延一拍，不改状态（禁止退回 ARM） |

### 4.4 状态机

```
IDLE ──目标有效──▶ ARMING ──use 成功──▶ BLOCKED
  ▲                   │                   │
  │                   │              attackReady
  │                   │                   ▼
  │                   │                HIT ──(下一拍)──▶ BLOCKED
  │                   │
  │                   └──hurt/换目标──▶ HARD_REL ──▶ HARD_USE ──▶ BLOCKED
  └──目标丢失/切物品── DROP(release) ──▶ IDLE
```

状态只保存 `serverUsing / pendingReuse / postArmed / refreshTicks` 五个量，全部在 tick 头部读写。

### 4.5 Block Slow 规格（本方案强制启用）

| 项 | 规定 |
|---|---|
| 注入点 | `MoveInputEvent`（`KeyboardInput.tick` TAIL，原始向量），`forward *= 0.2; strafe *= 0.2` |
| 生效条件 | `serverUsing == true`（服务端认为在 using） |
| 翻转时机 | 与 `use/release` 发包**同一次 tick 头部调用**内完成 |
| 释放当拍 | `release` 发出的同拍立刻取消 0.2，因为 `onMoveInput` 在本拍 `aiStep` 内、发包之后 |
| 数值校准 | 若实测 offset 恒为 ±2%，说明 0.98 缩放位置差；把系数调成 0.196 |
| 禁止 | `Server Mode = Modern` 时必须关闭，否则每拍 0.8×速度的 offset |

### 4.6 窗口预算公式

设软刷新周期 `R` 拍（1 个窗口），硬刷新每次 2 个窗口、每秒 `k` 次：

```
APS = 20 − 20/R − 2k
```

| R | k | APS | 格挡维持方式 |
|---|---|---|---|
| ∞(0) | 以受伤为主，约 0.5 | 19–20 | 服务端常驻 + 机会式补盾 |
| 20 | 0.5 | 17.5 | 每 1s 一次软刷新 |
| 10 | 0.5 | 15 | 更稳，适合 1.8 服务端 |

---

## 5. 前置条件

1. **服务端形态必须匹配**：`Legacy18` 要求服务端 <1.9 且客户端 ≥1.21.4（`LegacyItem#canUse` 的唯一 true 分支），否则 `slowedByUsingItem` 恒 false，Block Slow 必爆 Simulation。
2. **20 APS + 真实减伤**要求服务端 `experimental-checks: false`；否则 `MultiActionsA` 每拍 flag，必须退到 6.67 APS 形态。
3. **绕过本地攻击冷却**，否则剑的 `attackSpeed=1.6` 把速度锁在 1.6 APS。
4. **全部 AB 包走 `ActionWindow`**，与 `NoSlow / Scaffold / Velocity` 共享窗口占用权。
5. **`Keep Sprint Mode` 在 20 APS 下关闭**：每拍 `STOP_SPRINTING` 会额外增加 `PacketOrderF/H` 的判定面。
6. 客户端 26.2 无法伪造本地 `isUsingItem`（剑 `useDuration=0`，`startUsingItem` 下一拍即失效），格挡状态只能存在于服务端，本地减速必须手动实现。

---

## 6. 验证方法（可复现断言）

| # | 断言 | 方法 | 期望 |
|---|---|---|---|
| 1 | 窗口零违规 | `PacketEvent(Send)` 里按"距上一个 `MovePlayerPacket`"分组，统计每组 `attack/use/release` 数量 | 恒 ≤1 |
| 2 | APS 达标 | 统计 1s 内 `ATTACK` 包数 | 20±1（或按公式） |
| 3 | 无 Simulation | `/grim verbose` 观察 offset | 恒 `< 0.001` |
| 4 | 无 NoSlow | 同上 | 不出现 `NoSlow` |
| 5 | Block Slow 真的生效 | F3 看水平速度（举盾中） | ≈ 平时 19.6%，关掉立即 100% |
| 6 | 服务端认可格挡 | 1.8 服务端 + ViaVersion + 假人互砍 | 格挡期伤害减半 |
| 7 | 减伤窗口 | 记录每次 `release` 到 `use` 的拍数 | ≤1 拍（硬刷新）/ 0 拍（软刷新） |

---

## 7. 预期可复现表现

- **20 APS 形态**：每秒恒定 20 个攻击包，每拍一个，窗口内永不出现两个冲突动作；移动无 offset；服务端 `using` 状态常驻（仅在受伤硬刷新的 2 拍内中断，约 100ms）。
- **6.67 APS 形态**（实验检查全开时）：每 3 拍一次 `release → attack → use`，格挡 HUD 常亮，`MultiActionsA / PacketOrderI / J / Simulation / NoSlow` 全部为 0，命中瞬间处于非格挡态。
- **误配表现**（用于反查）：`Server Mode` 设错时，`Simulation` 会以**固定 ~0.8×速度**的 offset 每拍上报，`NoSlow` 两拍后跟进——这是判断 Block Slow 是否该开的唯一可靠信号。
