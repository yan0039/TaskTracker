# 实习投递跟踪 App：Coding Agent 实现指导书

> 用途：把本文件交给可编辑本地文件、运行构建命令的 Coding Agent，用来**实际生成项目**。  
> 功能真源：[项目规格与技术方案](实习投递跟踪App_项目规格与技术方案.md)。  
> 本文件是执行说明；当前交付的是指导书，尚未声称 Android 项目已生成。

## 0. 任务契约

```yaml
task: build_android_resume_project
project_name: InternshipTracker
project_root: D:\interview\InternshipTracker
spec_file: D:\interview\实习投递跟踪App_项目规格与技术方案.md
reference_only: D:\interview\TaskTracker项目实战指南.md
required_scope: FR-01..FR-08
optional_scope: P1（仅在 P0 已通过验收且时间允许时选一项）
platform: Android-only
language: Kotlin
ui: Jetpack Compose + Material 3
architecture: UI -> ViewModel/StateFlow -> Repository -> Room
storage: Room 2.x 稳定版；首版不需要 DataStore 或网络
navigation: Navigation Compose 2.x 稳定版
min_sdk: 26
output: 源码、Gradle Wrapper、有效测试、README、实际构建产物及验证报告
```

**完成目标**：在 `project_root` 交付可构建、可安装、可按规格演示的本地 Android 工程。不要仅交付伪代码、页面截图或尚未运行的源码。当前工作区如已有该目录，先检查并保留已有工作，继续完善；不得覆盖或删除用户文件。

执行环境的上级指令和用户最新要求始终优先。功能冲突以项目规格为准；本指导书规定执行顺序和验证方式。[TaskTracker 项目实战指南](TaskTracker项目实战指南.md)只供理解技术概念，不是可原样复制的项目模板。

## 1. 开始前读取与检查

1. 阅读**完整**项目规格，列出 FR-01 至 FR-08 的实现点及各自证据。
2. 查看 `D:\interview` 是否存在 `AGENTS.md`、项目目录或未提交改动；遵守适用的仓库说明。
3. 检查 JDK、Android SDK、Android Studio/命令行工具、Gradle、模拟器或真机；记录版本与缺项。
4. 查看执行当天的 Android 官方稳定版发布和兼容说明，选择**能一起构建**的 AGP、Gradle、Kotlin、Compose BOM、KSP、Hilt、Room 2.x 与 Navigation Compose 2.x；写入 `gradle/libs.versions.toml`，并在 README 记下版本选择。避免 alpha/beta 依赖。
5. 确认项目没有被用户先前建立。若已有代码，只修复和补充，不重新生成覆盖。

截至 2026-10-25，Room 2.8.5 与 Navigation Compose 2.10.2 是可参考的稳定线；执行时需重新核对官方版本及工具链兼容。Compose BOM **不**管理 Kotlin/Compose compiler 插件、Room、Hilt 或 Navigation 的版本。参考：[AndroidX 发布说明](https://developer.android.com/jetpack/androidx/versions)、[Compose BOM](https://developer.android.com/develop/ui/compose/bom)、[Kotlin 与 Compose 编译器配置](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)、[Room 发布说明](https://developer.android.com/jetpack/androidx/releases/room)。

**旧指南已知问题**：其中固定依赖版本属于旧组合；`exportSchema = false` 和破坏性迁移适合演示，不适合作品最终方案；不要照抄 Manifest 中可能导致解析错误的注释位置；不要在异步数据库写入尚未成功时立刻提示“保存成功”。复制代码后即使能编译，也要逐条对照 FR 行为。

## 2. 不可偏离的实现边界

- 先完成 P0。无登录、云同步、爬虫、假招聘接口、AI 功能或网络权限；不要用这些功能代替核心的投递、筛选、跟进和撤销流程。
- 单 `app` 模块、单 Activity。UI 与数据层分包即可；没有真实复用需求时不创建额外 Gradle 模块和空的 UseCase 层。
- 采用 `Compose Screen -> 屏幕级 ViewModel -> Repository -> Room DAO`。Composable 不直接持有 DAO；ViewModel 不持有 Activity/Context，不复制一份长期可变的数据库列表。
- Room 是投递数据唯一真实来源。DAO 用 `Flow` 观察，用 `suspend` 修改。数据库 `exportSchema = true`、提交 schema；不要配置 `fallbackToDestructiveMigration()` 来掩盖版本变化。
- 列表 UI 使用 `collectAsStateWithLifecycle()`；导航只传 `recordId`；导航动作由 UI 回调决定。表单未保存修改返回时有确认。
- 日期用统一的本地日历日表示，注入或封装今天的来源，以便测试到期边界。阶段值集中定义并用稳定字符串保存，不能存 enum 序号。
- 只有数据库操作真正成功，才能清空表单、返回列表或显示成功反馈。失败时保留输入并显示可理解错误。
- “删除撤销”恢复原 ID 与全部字段；不能仅在界面重新画一条但数据库中已丢失。
- 不引入任何明文密钥、用户真实投递数据、虚构用户数、未经验证的性能数字。截图、APK、录屏和测试结果都必须来自实际运行。

## 3. 分阶段执行与闸门

### 阶段 A：工程骨架

建立 Kotlin DSL 工程、Gradle Wrapper、版本目录、Manifest、应用入口、主题和两条页面路由。使用 `com.portfolio.internshiptracker` 作为默认包名；如果现有工程已有包名，保留其可工作的设置。先让空应用构建通过，不要一次生成所有文件后才编译。

**退出闸门**：`assembleDebug` 成功；有设备时能启动并看到基础页面。

### 阶段 B：数据层与纯业务规则

按规格建立 `JobApplicationEntity`、`JobApplication`、稳定阶段类型、DAO、Repository 与 Hilt 提供关系。实现新增、按 ID 查询、更新、删除和列表 `Flow`。把输入校验、组合筛选、到期计算做成可独立测试的逻辑。先写最小必要的单测，再继续 UI。

**退出闸门**：数据层相关测试通过；Room 生成 schema 文件；增删改能从观察流反映出来。

### 阶段 C：完整用户流程

完成列表页和复用的新增/编辑表单；列表含待跟进区、城市/阶段筛选、空态、编辑与删除。操作后依赖 Room 的新数据刷新 UI，不靠手动拼接临时列表。完成删除撤销和保存失败反馈。跨日后重新进入应用能刷新到期状态。

**退出闸门**：FR-01 至 FR-08 每项都有可操作入口；空表单、非法链接和筛选无结果都有清楚反馈。

### 阶段 D：测试、排错与演示

补足规格第 7 节测试：到期边界、组合筛选、Room 增改删与撤销，至少一条 Compose UI 流程。有设备时按规格第 8 节完成手动演示，实际检查旋转、强制关闭重开和离线状态。修复发现的问题，记录一次真实的“现象→定位→修复→复验”。

**退出闸门**：构建和可运行的自动化测试通过；手动验收有实际结果。若设备不可用，明确标记未验证，不伪造录屏或安装结论。

### 阶段 E：作品包装

写 README：项目目的、功能、真实截图、技术架构与数据流、构建步骤、测试命令及结果、设计取舍、已知限制、真实排错记录。构建 Debug APK，列出确切路径。若能录屏，做 60–90 秒演示；若不能，保留可直接照做的演示脚本。

**退出闸门**：他人可按 README 从干净检出构建；交付清单只列真实存在的文件。P1 增强只能在此闸门之后选择一项实施，不得拖延 P0 交付。

## 4. 验证命令与结果记录

在 Windows PowerShell 中从项目根目录执行；每次修复后重跑受影响的验证：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

有可用模拟器或真机时，再执行 `:app:connectedDebugAndroidTest`、安装应用并手动验收。若当前 Android 工具链的任务名有差异，先列出 Gradle task，再使用实际存在的等价任务。把命令、退出码、测试数量/结果、设备型号与 Android 版本、验证日期写到 `README.md` 或 `IMPLEMENTATION_REPORT.md`。

手动验收最短路径：清空数据 → 新增 3 条样例记录 → 出现今天/逾期待跟进 → 城市与阶段组合筛选 → 编辑阶段和备注 → 删除并撤销 → 强制关闭重开仍存在 → 旋转后已保存状态不丢失。样例数据标为演示数据，不写真实求职信息。

## 5. 失败与范围管理

- **依赖/构建失败**：检查仓库访问、JDK/AGP/Gradle/Kotlin/KSP/Compose 兼容性；按错误修复，不能随机升降版本或删除核心功能来“通过”。
- **无 SDK、网络或设备**：继续完成可静态检查的代码和可运行的单元测试；尽力解决本机配置。仍无法完成时列出精确阻碍、已尝试步骤和后续补验命令，不声称已运行成功。
- **用时超出**：先保住 FR-01 至 FR-08、构建、测试和演示，再削减动画、主题、统计与 P1。不要留下能点击却没实现的入口。
- **规格含糊**：选择最简单且能验证的行为，在 README 写明取舍；仅当会改变产品目标或无法继续实现时再请用户决定。
- **旧代码冲突**：检查用户当前修改与 Git 状态，保留原有工作；不要用递归删除或重建工程掩盖冲突。

## 6. 最终交付报告格式

执行 Agent 最终回复应有以下 6 项，均使用项目绝对路径：

1. 项目目录、入口文件、README 和主要实现文件。
2. FR-01 至 FR-08 完成情况，未实现项如实标注。
3. 实际运行的构建、单测、UI 测试与手动验收结果；未运行的原因。
4. 实际存在的 APK、截图和录屏路径；不存在的不要列。
5. 已知问题、复现步骤与下一步。
6. 仅基于已实现并可展示功能的一段简历描述。

## 7. 可直接交给 Agent 的起始指令

```text
请读取 D:\interview\实习投递跟踪App_项目规格与技术方案.md 和 D:\interview\实习投递跟踪App_Agent实现指导书.md，按照指导书在 D:\interview\InternshipTracker 生成并验证完整的 P0 Android 项目。TaskTracker项目实战指南.md 仅作参考。请持续完成到构建、关键测试与可用设备上的手动验收；按指导书报告真实结果、生成的文件及剩余限制。
```
