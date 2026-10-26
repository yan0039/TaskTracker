# 实习投递跟踪 App：面试技术与设计问答

> 适用场景：Android 开发实习面试。讲述时优先演示代码和运行结果；涉及测试、截图、APK、设备验证的数字，以 README 中最终记录为准。本文中的“后续可以”均为设计讨论，不代表已经实现。

## 30 秒介绍

这是一个离线优先的 Android 实习投递记录工具。用户可以记录公司、岗位、城市、阶段与跟进日期，在列表中组合筛选，并看到今天和已逾期的待跟进事项。我用 Kotlin、Jetpack Compose、ViewModel、StateFlow、Room 和 Hilt 做了单 Activity、单模块的分层实现：Room 保存记录，Repository 封装数据访问，ViewModel 把数据库结果与筛选条件整理成 UI 状态，Compose 只负责展示和上报操作。设计重点是日历日期的边界、终态记录的跟进规则，以及删除后的完整撤销。

### 代码定位

| 问题 | 主要代码 |
| --- | --- |
| 阶段代码与终态 | [`ApplicationStatus.kt`](../app/src/main/java/com/example/tasktracker/internship/model/ApplicationStatus.kt) |
| 校验、组合筛选、到期计算 | [`ApplicationRules.kt`](../app/src/main/java/com/example/tasktracker/internship/util/ApplicationRules.kt) |
| Room 模型映射与写入 | [`JobApplicationEntity.kt`](../app/src/main/java/com/example/tasktracker/internship/data/JobApplicationEntity.kt)、[`JobApplicationRepository.kt`](../app/src/main/java/com/example/tasktracker/internship/data/JobApplicationRepository.kt) |
| 删除快照事务与冲突策略 | [`JobApplicationDao.kt`](../app/src/main/java/com/example/tasktracker/internship/data/JobApplicationDao.kt) |
| 可替换的本地日期 | [`LocalDateProvider.kt`](../app/src/main/java/com/example/tasktracker/internship/util/LocalDateProvider.kt) |
| 列表状态、筛选、撤销 | [`ApplicationListViewModel.kt`](../app/src/main/java/com/example/tasktracker/internship/ui/list/ApplicationListViewModel.kt)、[`ApplicationListScreen.kt`](../app/src/main/java/com/example/tasktracker/internship/ui/list/ApplicationListScreen.kt) |
| 表单状态、校验与保存 | [`ApplicationFormViewModel.kt`](../app/src/main/java/com/example/tasktracker/internship/ui/form/ApplicationFormViewModel.kt)、[`ApplicationFormScreen.kt`](../app/src/main/java/com/example/tasktracker/internship/ui/form/ApplicationFormScreen.kt) |
| 两页路由与 ID 参数 | [`InternshipNavHost.kt`](../app/src/main/java/com/example/tasktracker/internship/ui/navigation/InternshipNavHost.kt) |
| Activity 入口与权限声明 | [`MainActivity.kt`](../app/src/main/java/com/example/tasktracker/MainActivity.kt)、[`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) |
| 日期、业务规则、Room 与 UI 测试 | [`LocalDateProviderTest.kt`](../app/src/test/java/com/example/tasktracker/internship/util/LocalDateProviderTest.kt)、[`ApplicationRulesTest.kt`](../app/src/test/java/com/example/tasktracker/internship/util/ApplicationRulesTest.kt)、[`JobApplicationRepositoryTest.kt`](../app/src/androidTest/java/com/example/tasktracker/internship/data/JobApplicationRepositoryTest.kt)、[`ApplicationFlowTest.kt`](../app/src/androidTest/java/com/example/tasktracker/internship/ui/ApplicationFlowTest.kt) |

## 一、项目与架构

### Q1：为什么做投递跟踪，而不是通用待办？

**答**：投递记录有明确的业务对象和可解释的规则：阶段可以前进或回退，Offer 和未通过是终态；跟进日期到了才进入待跟进区；城市和阶段能够一起筛选。这些规则让项目能展示建模、状态转换、数据持久化与测试，而不只是一个 CRUD 页面。首版聚焦个人本地使用，交互和数据范围也能控制在可完整验证的规模。

### Q2：项目的数据流是什么？新增后列表为何会自动更新？

**答**：界面把“保存”动作交给表单 ViewModel，ViewModel 校验输入后通过 Repository 调用 Room DAO 的挂起写入方法。`ApplicationListViewModel` 观察 `repository.observeAll()`，Room 表变化会让可观察查询重新发射；它用 `combine` 合并记录、筛选条件、今天的日期和待撤销快照，再通过 `stateIn` 提供 `ApplicationListUiState`。Compose 用 `collectAsStateWithLifecycle()` 收集后重组列表。因此 Room 是记录的唯一真实来源，不需要在列表 ViewModel 中维护第二份长期可变的数据。保存成功后才返回列表，避免写入失败却显示成功。

### Q3：为什么使用 Repository？ViewModel 直接调 DAO 是否更简单？

**答**：直接调用在很小的演示程序里可行，但会把 Room 实体、映射和存储细节带进页面状态。Repository 让 ViewModel 面向 `JobApplication` 业务模型而不是 `JobApplicationEntity`，统一封装观察、按 ID 查询、创建、更新、删除和恢复。当前 Repository 是具体类，现有测试通过可替换的 `Clock` 和内存 Room 验证它；若要轻松注入 Fake Repository 做 ViewModel 单测，还需先抽出接口或其他替换边界。投递业务只有一个核心实体，所以不再叠加空的 domain 模块和每个操作一个 UseCase。

### Q4：为什么是单 Activity、单 app 模块？以后如何拆分？

**答**：当前投递功能只有列表与新增/编辑两页、一个投递业务实体。单模块让构建和导航关系直接可见，分包就足以隔离 `ui`、`data`、`model`、`di`。仓库中保留的旧 TaskTracker 源码仍参与编译，但不在当前导航里。拆模块是解决团队协作、复用或构建耗时的手段；若以后出现多个独立功能，再考虑 `core:database`、`core:model` 和功能模块，并让依赖方向从功能层指向稳定的核心层。

### Q5：ViewModel、Compose 和 Room 各自持有什么状态？

**答**：Room 持有已经保存的投递记录；ViewModel 持有页面所需的筛选条件、加载或提交状态、表单输入和错误信息，并将其转换成可观察的 `UiState`；Compose 处理短暂的展示状态，例如放弃修改对话框。旋转屏幕时 ViewModel 能保留页面状态；进程被终止后的已保存记录从 Room 读取。当前 `SavedStateHandle` 只读取 `recordId`，未保存的表单草稿没有跨进程恢复机制，不能把 Room 的持久化能力等同于草稿自动保存。

### Q6：为什么 UI 用 `collectAsStateWithLifecycle()`？

**答**：它把 `StateFlow` 转成 Compose 可读状态，并按生命周期收集。页面离开前台时不持续进行无意义的 UI 收集，回来时再接收最新状态。ViewModel 的 `StateFlow` 缓存最新值，Room 则持续保存已提交数据；两者分别解决页面观察与长期持久化。

### Q7：为什么导航只传 `recordId`，不传完整记录？

**答**：完整对象可能很大，而且传入时的快照可能在其他操作后过期。`InternshipNavHost` 只传 `recordId`；新增路由用 `-1` 表示无 ID，表单 ViewModel 将其转为 `null`，编辑路由按正数 ID 向 Repository 查询当前记录。两种模式复用表单。按 ID 查不到记录时显示“记录已不存在”并提供返回列表入口，不会把它当成新记录覆盖保存。

### Q8：Hilt 在这里解决了什么问题？

**答**：它统一装配数据库、DAO、Repository、`Clock`/日期提供者和 ViewModel，避免页面手工创建依赖。数据库由应用级 Context 创建并保持单实例；ViewModel 通过构造参数接收依赖，不持有 Activity。依赖缺失通常在编译期暴露。当前日期测试直接注入 `Clock.fixed(...)`，Room 测试直接建立内存数据库；项目没有编写 Hilt 替换模块或 Fake Repository 测试。

## 二、数据建模与业务规则

### Q9：为什么业务模型与 Room Entity 分开？

**答**：Entity 关注表名、主键、列和 Room 注解；业务模型表达页面与规则需要的概念。把二者分开，UI 不会直接依赖数据库字段的存储格式，比如 `nextFollowUpEpochDay`。未来改变数据库实现或引入导入导出时，Repository 的映射是清晰的边界。对于这个规模，映射代码有少量成本，但比让持久化注解穿透每个页面更可控。

### Q10：阶段为什么存稳定字符串，而不存枚举序号？

**答**：序号受声明顺序影响。以后在“笔试”前插入一个新状态，旧数据的序号可能被解释为另一种阶段。`ApplicationStatus` 定义了 `PENDING`、`APPLIED`、`ASSESSMENT`、`INTERVIEW`、`OFFER`、`REJECTED` 的稳定代码与显示文案；Entity 保存代码，映射时通过 `fromCode()` 还原。未知代码会明确抛错，避免静默显示成另一个阶段。

### Q11：为什么允许阶段前进和回退，不自动设置投递日期？

**答**：用户可能误选阶段，也可能补录旧记录；阶段是当前状态，不是可信的历史事件日志。把“已投递”自动写成今天，会制造一个可能不真实的日期。因此允许状态修正，投递日期由用户选择或留空。若以后要做阶段耗时统计，应增加真实的阶段变更历史，而不能从一个当前阶段字段反推。

### Q12：如何判断一条记录需要跟进？

**答**：条件是 `nextFollowUpDate != null && nextFollowUpDate <= today && status !in {OFFER, REJECTED}`。小于今天的记录在同一个待跟进区标为“已逾期”，等于今天的标为“今天跟进”，晚于今天的不显示。待跟进项按日期从早到晚排序，优先让逾期最久的事项可见。终态仍保留跟进日期供历史查看；如果用户把阶段改回非终态，会按当前日期重新计算。

### Q13：为什么日期存 `epochDay`，而不是毫秒时间戳？

**答**：投递日和跟进日的业务含义是“用户本地日历上的某一天”，没有具体时刻。`LocalDate.toEpochDay()` 存一个整数，读回时用 `LocalDate.ofEpochDay()`，避免把午夜时间戳跨时区解释成前一天。创建与更新时间则是实际时间点，适合存毫秒。两类时间概念不能混为一谈。

### Q14：应用跨过午夜后，待跟进列表如何更新？

**答**：Room 记录没变化时，其 `Flow` 未必会重新发射，但“今天”已经变化。列表页在 `ON_RESUME` 调用 `refreshToday()`，从注入了 `Clock` 的 `LocalDateProvider` 取得本地日期，触发到期规则重算。设备用的 `DeviceLocalClock` 每次读取当前系统时区，能在重新进入前台时适应时区变更。纯规则测试把不同日期传给 `dueFollowUps()`，日期提供者测试用固定时钟验证时区边界；当前没有自动化测试直接覆盖跨午夜的前台生命周期事件。若应用一直停在前台不离开，清单不会恰在午夜自动更新。

### Q15：城市与阶段组合筛选怎样实现？

**答**：`filterApplications()` 让城市与阶段同时作用于同一份记录集。城市比较前去除首尾空格并忽略大小写，阶段用 `ApplicationStatus` 比较；条件为空表示不过滤。列表 ViewModel 把筛选后的 `visibleRecords` 交给 `dueFollowUps()`，所以待跟进区与“全部记录”区的范围一致。`clearFilters()` 恢复全量；数据库为空与筛选无匹配项要显示不同提示，用户才能知道是没有数据还是条件太窄。

### Q16：同一公司同一岗位重复出现，是不是数据错误？

**答**：不一定。不同城市、岗位批次、投递渠道或重新投递都可能形成多条记录，因此首版不设公司加岗位的唯一约束，也不自动合并。记录的身份由自增 `id` 决定。如果以后做去重提醒，也应只是提示，不能把用户不同时间的真实投递强行覆盖。

### Q17：新增和编辑如何处理 `createdAt`、`updatedAt`？

**答**：新增时生成 ID 及两个时间字段；编辑时保留原始 ID 和 `createdAt`，只更新 `updatedAt`。这样按 ID 查找与撤销有稳定身份，创建时间仍表示第一次建立记录的时刻。更新时间是元数据，不应靠页面重新创建对象时顺手重置所有字段。

### Q18：删除后 5 秒撤销有什么难点？

**答**：DAO 的 `deleteByIdReturningSnapshot()` 在同一个事务中读取并删除，返回的快照包含原 ID、日期、阶段、备注和时间字段。撤销时 Repository 把这份快照插回 Room；插入冲突采用 `ABORT`，避免误覆盖新数据。`ApplicationListViewModel` 只保留一个带 token 的待撤销快照，在 5 秒倒计时中拒绝第二次删除；UI 直接根据该状态渲染撤销按钮，避免普通 Snackbar 消息排队遮住短暂的操作窗口。仅把卡片在 UI 中画回来，进程重启后仍会丢失。

### Q19：为什么不把投递记录放在 DataStore？

**答**：记录是结构化、多行、可按 ID 修改并需要筛选的实体，Room 有表、查询、事务和可观察 `Flow`，适合做主存储。DataStore 更适合少量键值或配置偏好，例如主题、排序方式。这个投递跟踪功能没有使用 DataStore 保存记录或筛选条件，也不把它当成数据库的替代品。

### Q20：Room 版本变化时为什么不能使用破坏性迁移兜底？

**答**：`fallbackToDestructiveMigration()` 找不到迁移路径时会重建表，用户投递记录可能被清空。当前数据库为 v1，配置了 `exportSchema = true` 并提交 schema；没有配置破坏性迁移。将来字段变化时应提升版本，编写 Migration 并用旧 schema 和迁移测试证明记录保留。首版没有发生表结构升级，所以没有凭空制造迁移。

## 三、交互、异常与安全

### Q21：表单输入如何校验，为什么不能只禁用保存按钮？

**答**：公司、岗位、城市用 `isBlank()` 检查，写入前去除公司和岗位的首尾空格；城市保留原输入供编辑，显示与筛选时规范化。非空岗位链接用 `URI` 检查 `http/https`、有效主机且不带用户信息；日期由日期选择器产生，减少格式解析错误。当前保存按钮在提交期间禁用，普通输入错误则在点击后显示字段提示；Repository 写入前也调用纯函数 `validateJobApplicationInput()`，避免未来其他入口绕过 UI。失败时保留输入，不导航离开。

### Q22：数据库写入失败时如何避免“假成功”？

**答**：`ApplicationFormViewModel.save()` 先校验并把 `isSaving` 设为真，等待 Repository 的挂起写入真正返回。创建成功或编辑确实更新了 1 行，才把 `saveSucceeded` 设为真，页面观察这个状态后返回列表；记录已经被删除时，`update()` 返回 `false`，表单显示失败。异常时恢复可提交状态、保留草稿并展示错误。`isSaving` 也挡住双击保存，避免同一次输入创建两条。不能把 `viewModelScope.launch { insert(...) }` 启动成功误当成数据库写入成功。

### Q23：用户改了表单后按返回键，如何防止误丢输入？

**答**：`ApplicationFormUiState` 同时保存 `values` 和 `initialValues`，`hasUnsavedChanges` 比较两者。无修改则直接返回；有修改则弹“放弃修改”确认。确认放弃才离开，取消则继续编辑。保存成功后 `saveSucceeded` 为真，不再触发这个确认。比较的是用户可编辑字段，自动变化的 `updatedAt` 不会误判为脏状态。

### Q24：为什么首版没有通知和网络权限？

**答**：首版核心是用户打开应用时看到可靠的待跟进清单。通知涉及权限、后台调度与系统限制，不能承诺准确时刻提醒；网络则引入账号、同步冲突和隐私处理。Manifest 没有申请网络权限，`allowBackup` 设为 `false`；投递记录保存在设备的 Room 数据库中。设备间数据传输行为还受系统版本和厂商实现影响，不能仅凭此属性承诺绝对不会迁移数据。后续若增加通知，可用 WorkManager 做允许延迟的后台检查，并向用户说明调度不保证精确时间。

### Q25：岗位链接为什么只接受 `http/https`？点击链接又要注意什么？

**答**：表单需要排除空白以外的非法或不适合打开的 URI scheme，避免 `javascript:` 等内容被当成网页链接。列表通过 Compose 的 `LocalUriHandler.openUri()` 交给系统处理，并捕获无法打开的异常，显示提示。链接是用户输入的数据，不意味着应用获得该网页内容，也不应把它说成职位同步功能。

## 四、测试、排错与扩展设计

### Q26：哪些测试最有价值？

**答**：最容易出错的是日期和数据身份。`LocalDateProviderTest` 用同一时刻在上海和 UTC 时区验证本地“今天”不同，也检查设备时区变化；`ApplicationRulesTest` 覆盖昨天、今天、明天、无日期、终态与重新计算，以及城市和阶段的组合筛选；`JobApplicationRepositoryTest` 用内存 Room 验证新增、按 ID 查询、编辑、删除与恢复原 ID；`ApplicationFlowTest` 从真实 Activity 走新增、列表、编辑阶段、删除与撤销的 UI 路径。最终验证为 8 项 JVM 测试（其中旧项目 2 项）、3 项 Android 10 模拟器仪器化测试，全部通过。它们没有覆盖所有手工场景，也没有验证连接的 Android 16 真机。

### Q27：如何手动验证“持久化”和“旋转不丢数据”？

**答**：先保存记录，再切页面、旋转设备、强制结束并重开应用，核对字段与跟进结果。旋转主要检验页面状态和生命周期处理；强制结束重开才检验真实数据库文件在进程结束后仍可读取。本次在模拟器检查了空库、必填缺失及只输入空格、非法 URL、筛选无结果、删除撤销、长备注、重复公司岗位、日期为空、旋转、强制结束重开和断网，具体结果见实现报告。已通过的仪器化测试不能替代手工步骤，演示截图和录屏也只能来自实际运行。

### Q28：如果面试官问“你修复过什么真实问题”，如何回答？

**答**：升级 Room 到 2.8.5 后，旧 KSP1 路径在导出 schema 时触发 `AbstractMethodError`；切到 KSP2 后，Hilt 2.53.1 的处理器在当前 Windows 构建环境继续失败。排查后让 Room 使用 KSP2，Hilt 编译器改走 kapt 并升级到 2.54。重新运行 Debug 构建、8 项 JVM 测试、Lint 和 3 项模拟器仪器化测试后均通过。这是当前项目的实际构建排错；没有把旧 TaskTracker 的问题冒充新项目经历。

### Q29：数据达到几万条时，当前设计会遇到什么问题？

**答**：全量读取与内存筛选会让查询、对象分配和列表状态转换成本增加。可以把城市与阶段筛选下推到 Room 查询，给常用条件建索引；列表引入 Paging 3，待跟进则用明确的日期和阶段条件查询。优化前应先测实际数据量、查询耗时和 UI 卡顿，不凭空声称当前实现有性能问题。`LazyColumn` 解决可见项渲染，不等于数据库分页。

### Q30：如果做 JSON 导出与恢复，主要设计点是什么？

**答**：使用系统文件选择器由用户决定导出位置和导入文件；导出要有格式版本，日期与阶段使用稳定表示。导入时先完整解析和校验必填字段、URL、阶段代码与日期，再确定重复处理策略，最后在事务中写入并给出成功、跳过、失败数量。恢复不能因为一条坏记录就悄悄覆盖全部现有数据。这是首版之后的 P1 方案；只有代码与测试真正落地后才写进简历的已实现功能。

### Q31：这个项目最重要的设计取舍是什么？

**答**：一是把“跟进日期”定义成日历日，而不是时间点，从模型上避开时区误差；二是让 Room 成为记录的唯一真实来源，用 Flow 驱动 UI，减少手动同步状态；三是先交付可验证的离线闭环，不加入登录、云同步和通知来扩大未验证的范围。限制也相应明确：目前适合单设备个人使用，没有跨设备同步，也不保证系统级提醒。

### Q32：`StateFlow` 和 `SharedFlow` 在这个项目里分别做什么？

**答**：列表的筛选条件、今天日期和 `UiState` 是持续存在的状态，新的观察者需要拿到当前值，因此使用 `StateFlow`。删除失败等提示是一次性事件，列表 ViewModel 用 `SharedFlow` 发消息，避免旋屏后因为重播旧 `UiState` 再弹一次提示。是否删除成功仍由 Room 数据和待撤销快照表示，不能只依赖一次性消息维持业务状态。

### Q33：`stateIn(SharingStarted.WhileSubscribed(5_000))` 的 5 秒是什么意思？

**答**：列表 ViewModel 把多个 Flow 合成 `UiState` 时用 `stateIn` 转成热流并缓存最新值。最后一个 UI 订阅者消失后，上游再持续约 5 秒才停止；短暂切后台或旋转时不必立即重新启动全部观察。这个 5 秒是 Flow 订阅保活时间，与删除撤销的 5 秒窗口是两种独立机制，不能混淆。

### Q34：为什么查询返回 `Flow`，增删改使用 `suspend`？

**答**：列表需要持续看到数据库变化，所以 DAO 的 `observeAll()` 返回 `Flow`；按 ID 查找和插入、更新、删除都是一次性操作，用挂起函数等待结果。Room 会把数据库工作安排到合适的执行器，ViewModel 从 `viewModelScope` 调用即可；不能因为函数是 `suspend` 就认为写入必定成功，仍要处理异常和受影响行数。

### Q35：Material 3 日期选择器的毫秒值如何转成 `LocalDate`？

**答**：日期选择器给出表示所选日的 UTC 毫秒值；它只是控件的接口格式，业务层不把它当成“当地午夜的时间戳”。表单用 `Math.floorDiv(selectedDateMillis, 86_400_000L)` 得到 epoch day，再构造 `LocalDate`；设置初始选择时反向转换。这样点击某个日历格就得到同一天的 `LocalDate`，不会因为设备时区偏移到前一天。负 epoch day 也用 `floorDiv`，比直接整数除法更严谨。

## 面试前对照清单

1. 能从点击“新增”追踪到 Repository、DAO、Room `Flow` 和列表重组，并指给面试官看对应文件。
2. 能用昨天、今天、明天及终态各举一个待跟进边界例子。
3. 能解释同公司同岗位为什么允许重复，以及撤销为何必须恢复原 ID。
4. 能演示输入校验、组合筛选、编辑、删除撤销、重启后持久化。
5. 简历只写实际已构建、测试、演示过的内容；未运行的设备测试或 P1 功能不写成已完成。
