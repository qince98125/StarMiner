# 第一人称遮挡、偶发弹跳与开发测试退出修复（1.0.1）

## 本轮修复

- 第一人称被支撑块纹理覆盖：`ScreenEffectRenderer.getOverlayBlock` 原来使用世界 X/Z 和普通 eyeY 取样，侧向站立会误采到支撑块。现在将八个头部遮挡采样点转换到重力坐标系，围绕实际眼睛位置检测。保留真正头部进入方块时的遮挡，不是直接禁用纹理覆盖。
- 静止镜头晃动：原版 `Player.aiStep` 使用世界 X/Z 速度计算镜头 bob，把侧向重力留下的法向速度误算成走路。改为本地地面平面的切向速度。
- 接触误差导致失去支撑：额外测试复现了碰撞箱进入支撑面 1e-6 格后失去落地状态。现在只校正不超过 1e-5 格的支撑面穿入，客户端推离检测之前及共同移动入口均处理。向上跳跃不做该校正，不产生额外弹力，也不改变摩擦系数。

## 21:11 的异常退出

`run/crash-reports/crash-2026-10-14_21.11.03-client.txt` 显示退出原因是开发断言：

```
AssertionError: DOWN_TO_UP_Y_P real head obstruction no longer draws overlay
```

先前测试复用了普通 `client` 启动任务，它生成的 `clientRunVmArgs.txt` 中保留了 `-Dstarminer.verifyCamera=true`。普通开发启动沿用了这个测试参数，导致测试进入实际游玩存档。

现在：

1. 普通 `runClient` 固定 `starminer.verifyCamera=false`，重新生成普通启动参数文件。
2. 测试改成独立的 `runGravityClientCheck` 任务，使用独立参数文件和 `run-gravity-client-checks/` 目录。
3. 运行时还检查规范化后的游戏目录；即使旧参数意外开启，普通 `run/` 中也不会执行测试。
4. 相机测试使用独立玩家对象，不修改实际玩家的重力、飞行状态或姿态。
5. 测试断言失败只记录日志及 `gravity-check-result.txt`，不再传播到游戏事件循环引发崩溃。

## 验证

```powershell
$env:JAVA_HOME = 'D:\Java\java21'
$env:GRADLE_USER_HOME = 'C:\Users\qince\.gradle'
.\gradlew.bat runGravityClientCheck -PgravityCheckExit --offline
.\gradlew.bat build runGameTestServer prepareClientRun --offline
```

测试需要 `run-gravity-client-checks/saves/GravityQA`；本轮已从已有的隔离 QA 存档复制。`gravityCheckExit` 仅让独立测试客户端在记录结果后正常关闭，不影响普通游玩。

已实际通过：

- 450 组 Camera 朝向/上方向组合。
- 六方向完整 `LocalPlayer.tick`，含姿态更新、推离检测、落地、摩擦、静止 bob 和跳跃。
- 1240 个站立/停止检查 tick；非默认重力额外覆盖接触偏差 `-1e-6 / 0 / 1e-8 / 1e-6`。
- 实际 `ScreenEffectRenderer.getOverlayBlock`：站立时无虚假支撑纹理，真实头部进入方块时仍有遮挡。
- 跳跃仍能离开地面，接触误差校正不会把玩家吸住。

客户端最终结果：`run-gravity-client-checks/gravity-check-result.txt`。
日志：`build/gravity-contact-before.log`（复现）、`build/gravity-contact-after.log`（通过）。

新构建产物：`build/libs/starminer-1.0.1.jar`。使用它替换旧版并重启；正在运行的客户端不会自动加载新编译的代码。完整构建和服务端回归记录在 `build/gravity-first-person-verification.log`。

之前文档中的 `runClient -PgravityClientCheck` 已停用。今后自动检查请使用单独的 `runGravityClientCheck` 任务，正常游玩使用 `runClient`。
