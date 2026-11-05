# deadline · PopupPlanner

一个极简的「计划本式弹窗提醒」Android App：指定某一天的某个时刻，写下一件要做的事；时间一到，deadline 会用全屏弹窗提醒你。

- Kotlin + Jetpack Compose（Material 3）
- Room 持久化，AlarmManager 精确闹钟
- minSdk 26，compileSdk / targetSdk 35
- 不依赖 Hilt / Dagger，不使用 WorkManager 触发提醒

## 1. 把代码推到 GitHub

在 `PopupPlanner` 目录下执行（把 `YOUR_NAME` 换成你的 GitHub 用户名）：

```bash
git init
git add .
git commit -m "Initial commit: PopupPlanner"
git branch -M main
git remote add origin https://github.com/YOUR_NAME/PopupPlanner.git
git push -u origin main
```

推送到 `main` 后，`.github/workflows/build-apk.yml` 会自动开始构建。仓库里故意不包含 `gradlew` 和 `gradle/wrapper/`；工作流会先由 `gradle/actions/setup-gradle@v4` 提供 Gradle 8.9，再在 CI 中生成 wrapper 并执行 `./gradlew assembleDebug`。

## 2. 手动触发构建并下载 APK

1. 打开 GitHub 仓库页面，进入 **Actions**。
2. 在左侧选择 **Build debug APK**。
3. 点击右上角的 **Run workflow**，选择 `main` 分支并确认。
4. 构建完成后进入这次运行记录，在页面底部的 **Artifacts** 区域下载 **PopupPlanner-debug-apk**（保留 14 天）。
5. 解压下载的文件，得到 `app-debug.apk`，传到手机上安装。Debug APK 已自动使用调试签名，无需配置 keystore 或 Secrets。

## 3. 首次使用必须授予“闹钟和提醒”权限

安装并打开 deadline 后，请到系统设置中允许本应用的 **闹钟和提醒**（即 `SCHEDULE_EXACT_ALARM`）权限：

- 常见路径：**设置 → 应用 → deadline → 闹钟和提醒 → 允许**
- 部分系统路径：**设置 → 应用 → 特殊应用权限 → 闹钟和提醒 → deadline → 允许**

如果未授权，App 不会静默降级成不精确提醒；保存计划时会弹窗引导你前往系统设置，列表页也会持续显示提醒。授权后返回 App，已保存的未来计划会自动重新注册闹钟。

另外建议开启以下权限；首页的“提醒权限需要开启”卡片会逐项提示并跳转系统设置：

- **通知**（Android 13+）：没有通知权限时，OEM 限制后台启动 Activity 后就没有兜底提醒。
- **全屏通知**（Android 14+）：路径通常为 **设置 → 应用 → deadline → 全屏通知**。未开启时，锁屏提醒可能只显示横幅。
- **勿扰模式例外**（可选，用于绕过勿扰）：路径通常为 **设置 → 通知 → 勿扰模式 → 例外**，或 **设置 → 应用 → 特殊应用权限 → 勿扰模式访问权限 → deadline**。

## 4. 关闭电池优化限制

为保证设备休眠、后台或厂商省电策略下仍能准时弹窗，建议把 deadline 加入“不受限制”的白名单：

- **设置 → 电池 → 电池优化 → 所有应用 → deadline → 不受限制**
- 国产 ROM 还需在系统管家中允许 **自启动 / 后台活动 / 锁屏显示**，并在最近任务中锁定本 App（如果系统支持）。

不同厂商的设置名称可能略有差异。没有这些设置时，系统可能延迟甚至拦截闹钟广播。

## 使用方式

1. 点击右下角 **新建计划**。
2. 选择日期和时间，填写标题（必填）与备注（选填）。
3. 点击 **保存计划**，计划会出现在列表中，并按时间升序排列。
4. 左滑计划卡片或点击 **删除** 可以删除计划，同时取消已注册的闹钟。
5. 到点后，即使 App 在后台或被杀死，也会尝试全屏弹出提醒；锁屏时弹窗会显示在锁屏之上并点亮屏幕。点击 **知道了** 关闭。

## 主要目录

```text
PopupPlanner/
├── .github/workflows/build-apk.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/popupplanner/
│           ├── MainActivity.kt
│           ├── data/        # Plan / PlanDao / AppDatabase
│           ├── alarm/       # AlarmScheduler / AlarmReceiver / BootReceiver
│           ├── ui/          # 列表、新建页、全屏提醒
│           └── theme/       # Material 3 主题
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```
