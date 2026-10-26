# 投递簿实现与验收报告

日期：2026-10-25 至 2026-10-26（Asia/Shanghai）

## 项目位置与范围

用户要求完善当前目录中的项目，因此在 `D:\TaskTracker` 继续开发。指导书模板中的 `D:\interview\InternshipTracker` 是新项目建议路径；本次没有另建目录，也没有覆盖既有 TaskTracker 源码。旧 README 已保存为 `docs/TaskTracker_旧版说明.md`。当前 Activity 入口显示投递簿，旧待办页面不在导航里。

P0 范围为 FR-01 至 FR-08。P1 的 JSON 导出、迁移演示、后台通知未纳入首版。项目无账号、网络请求与网络权限。

## 功能证据

| 需求 | 实现位置与行为 | 自动验证 |
| --- | --- | --- |
| FR-01 新增 | 表单填写公司、岗位、城市和可选字段；Repository 写入后 Room Flow 更新列表 | `ApplicationFlowTest`、`JobApplicationRepositoryTest` |
| FR-02 查看/编辑 | 列表卡片与复用表单按 ID 加载；编辑保留 `createdAtMillis` | 同上 |
| FR-03 阶段 | `ApplicationStatus` 统一六个稳定代码，编辑可双向调整，终态排除待跟进 | `ApplicationRulesTest`、`ApplicationFlowTest` |
| FR-04 组合筛选 | 城市和阶段共同过滤全部记录；待跟进从筛选结果计算，可清空、有空态 | `ApplicationRulesTest` |
| FR-05 待跟进 | `epochDay <= today`、非终态；按日期升序，今天/逾期分开显示，重回前台刷新今天 | `ApplicationRulesTest`、`LocalDateProviderTest` |
| FR-06 删除撤销 | DAO 事务删除返回完整快照，5 秒撤销插回原 ID；撤销按钮由待删除状态直接渲染 | `JobApplicationRepositoryTest`、`ApplicationFlowTest` |
| FR-07 Room 持久化 | 单独的 `InternshipDatabase`、DAO Flow 与提交的 v1 schema | `JobApplicationRepositoryTest`；手工重启见下 |
| FR-08 校验异常 | 必填 trim 后非空、http/https URL、日期选择器、写入失败保留表单，列表/筛选空态 | `ApplicationRulesTest`、手工检查见下 |

源码：`app/src/main/java/com/example/tasktracker/internship`；入口：`app/src/main/java/com/example/tasktracker/MainActivity.kt`；Room schema：`app/schemas/com.example.tasktracker.internship.data.InternshipDatabase/1.json`。

## 环境与自动验证

本机 JDK 17、Android SDK Platform 35 / Build Tools 35、Gradle Wrapper 8.9。2026-10-25 至 26 在 Windows PowerShell 的 `D:\TaskTracker` 执行：

| 命令 | 结果 |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin --console=plain` | 退出码 0，`BUILD SUCCESSFUL` |
| `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain`，`ANDROID_SERIAL=emulator-5554` | 退出码 0，`BUILD SUCCESSFUL` |

JVM 测试 8 项：旧版 `ThemeModeTest` 2 项、新业务 `ApplicationRulesTest` 4 项、`LocalDateProviderTest` 2 项；均无失败。模拟器 Pixel 6 Pro、Android 10 / API 29 上最终运行 3 项仪器化测试：`JobApplicationRepositoryTest` 2 项、`ApplicationFlowTest` 1 项；均无失败。仪器化测试覆盖 Room 新增→编辑→删除→撤销的真实数据库路径，以及 Compose 新增→列表→编辑阶段→删除→撤销→再次删除的 UI 路径。

测试原始结果：`app/build/test-results/testDebugUnitTest`、`app/build/outputs/androidTest-results/connected/debug`。Lint 报告：`app/build/reports/lint-results-debug.html`。Debug APK：`app/build/outputs/apk/debug/app-debug.apk`（18,320,325 字节）。

曾尝试安装至连接的 2211133C Android 16 手机，ADB 返回 `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`，因此没有声称真机通过；改在 Pixel 6 Pro Android 10 模拟器上安装、启动并通过仪器化测试。

## 手工验收

手工操作在同一 Pixel 6 Pro Android 10 模拟器上进行。演示公司、岗位和备注均为虚构数据。

| 项目 | 实际结果 |
| --- | --- |
| 空数据库 | 首次进入显示“还没有投递记录”和“暂无到期跟进”，不是空白屏。 |
| 新增与日期 | 新增三条不同城市/阶段的演示记录；列表截图可见今天及逾期两条待跟进，另有无跟进日期的记录。 |
| 城市+阶段筛选 | 选择 Shanghai + 已投递时，待跟进和全部记录均只显示同一条匹配记录；选择无匹配组合时，两区分别显示明确空态；清空后恢复。 |
| 编辑 | 将 DemoLabs 的阶段改为 Offer、备注改为 `FollowupComplete` 并保存；列表显示 Offer，再打开表单仍读取到备注。 |
| 输入校验 | 空表单及只输入空格的必填字段均显示字段错误；`ftp://example.com` 被拒绝，输入留在表单；未保存修改返回时出现确认。 |
| 长备注与重复岗位 | 319 字符备注保存后重新进入编辑页仍完整；相同公司及岗位可新增第二条，列表显示两条。 |
| 删除撤销 | 删除后页面立即显示撤销按钮；超过约 5 秒未撤销，记录保持删除；重新新增后在窗口内撤销，记录返回列表。 |
| 强制结束、旋转、断网 | 保存一条 DemoLabs 后执行 `am force-stop` 并重开，仍显示该记录；旋转到横屏仍显示；关闭模拟器 Wi-Fi 和移动数据后仍显示。测试结束恢复竖屏及网络。 |

真实运行截图：`docs/screenshots/internship-list.png`、`internship-filter.png`、`internship-form.png`、`internship-edit.png`。未制作录屏；README 提供 60–90 秒演示脚本。

## 实际排错记录

1. **现象**：Room 升至 2.8.5 后，使用 KSP1 生成 schema 报 `AbstractMethodError: FieldBundle$$serializer.typeParametersSerializers()`。**定位**：KSP1 与 Room 使用的序列化运行时不匹配。**修复**：启用 `ksp.useKSP2=true`。**复验**：Room schema 成功生成。
2. **现象**：切 KSP2 后，Hilt 2.53.1 的处理器在本环境报 `Expected @AndroidEntryPoint to have a value`；改为 kapt 后，Windows 路径产生 `Invalid relative name META-INF\proguard...`。**定位**：Hilt 处理路径/版本组合。**修复**：Room 维持 KSP2，Hilt 编译器改用 kapt 并升级到 Hilt 2.54。**复验**：Kotlin 编译、Debug APK、8 项 JVM 测试、Lint、3 项仪器化测试均通过。
3. **现象**：首次仪器化测试编译因 Compose 测试文件的导入与节点断言 API 用法失败。**修复**：调整测试导入与节点查询。**复验**：`:app:compileDebugAndroidTestKotlin` 和 `:app:connectedDebugAndroidTest` 均通过。
4. **现象**：复核发现普通消息和删除撤销原先共用 Snackbar 队列，普通消息可能挡住只有约 5 秒的撤销入口。**修复**：撤销提示改为直接由 `pendingDeletion` 状态渲染。扩展 UI 测试第一次因在异步状态出现前直接断言而失败；改为等待撤销按钮出现，再点击并核对恢复。**复验**：最终 3 项仪器化测试全部通过，手工删除撤销也通过。

## 限制与后续

- 首版只有应用内待跟进清单，没有系统通知、JSON 导入导出或真实 Room 升级迁移。当前数据库版本为 1，schema 已导出，不使用破坏性迁移。
- 今天的日期在应用重回前台时刷新；持续停在前台跨过午夜不会立刻更新，需要重新进入前台。
- 新业务位于当前项目的 `internship` 包。保留的旧 TaskTracker 源码仍参与编译、旧数据逻辑与投递记录隔离；后续可单独清理，但本次不删除用户现有工作。
- 已在 Android 10 模拟器验证；连接的 Android 16 手机因用户设备安装策略拒绝 Debug APK，本次未验证该机型。
- 未制作录屏；README 提供可执行的 60–90 秒演示脚本。

