# 侧向站立跳动与摩擦修复

## 原因和修复

客户端 `LocalPlayer.aiStep` 每 tick 对脚边的四个世界 X/Z 检测点调用 `moveTowardsClosestSpace`。原版 `suffocatesAt` 把检测点所在的整个方块列当成玩家所在空间；侧向重力旋转碰撞箱后，这些列会伸进支撑墙，虽然玩家实际碰撞箱只是在墙面上站立。

误判会给玩家每 tick 施加 0.1 格的脱离方块速度。它沿侧向重力的反方向把玩家弹开，使 `onGround` 频繁变为 false，移动进入空气阻尼分支。因此站立抖动和滑动失控是同一个客户端问题的两个表现。此前服务端 GameTest 没有执行客户端专有的这段推离逻辑。

`LocalPlayerGravityMixin` 现在只检测“检测列与实际旋转碰撞箱的交集”，并去掉 1e-7 的边界接触。接触支撑面不会触发推离；真正进入方块的身体区域仍参与卡墙检测。默认重力仍使用原版实现。

没有人为增大摩擦系数或冻结玩家。稳定落地后，已有本地坐标系移动自然使用支撑方块的原版摩擦：石头每 tick 保留约 `0.6 × 0.91 = 0.546` 的切向速度。

## 验证

新增 `GravityClientMotionChecks`，创建真实 `LocalPlayer` 并执行完整 `aiStep`，覆盖客户端卡墙推离逻辑：

- 六方向分别连续站立 120 tick，逐 tick 检查站立高度和 `onGround`。
- 无输入、初始切向速度 0.2 时，检查石头摩擦后的速度约 0.1092。
- 每个方向再执行 20 tick，确认持续有支撑、速度降至停止阈值。
- 确认修复没有压制跳跃。

修复前实际复现：`EAST_TO_WEST_X_N unstable client standing at tick 0: height=-0.09999999999999432, grounded=false`。

修复后日志：`GRAVITY CLIENT MOTION CHECKS PASSED: 840 standing/stopping ticks across six directions`。原有 450 组相机运行时检查也再次通过。

日志在 `build/gravity-standing-before.log` 和 `build/gravity-standing-after.log`。客户端检查仍通过 `runClient -PgravityClientCheck --offline` 启动，使用独立 `run-gravity-client/saves/GravityQA` 存档；不运行在用户原有存档。

构建：`build/libs/starminer-1.0.0.jar`。完整构建与服务端回归结果见 `build/gravity-standing-verification.log`。
