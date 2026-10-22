# Repository Guidelines

## 项目结构与模块划分

VolumeControl（轻控）是使用 Kotlin、Jetpack Compose 和 Material 3 开发的单模块 Android 应用，最低支持 API 29，编译与目标版本为 API 37。

- `app/src/main/java/com/ljrh/volumecontrol/`：Activity、Compose 页面、媒体音量与手电筒控制器，以及快捷设置磁贴服务；`ui/theme/` 存放主题样式。
- `app/src/main/res/`：中文文案、图标和浅色／深色资源；组件与权限在 `AndroidManifest.xml` 中声明。
- `app/src/androidTest/`：设备仪器化测试；`docs/` 存放使用说明与验收记录；`artwork/` 存放应用图标原稿。
- `gradle/libs.versions.toml`：统一管理依赖版本；`app/build/` 为生成产物目录。

## 构建、测试与开发命令

在仓库根目录使用 Gradle Wrapper 执行命令。Gradle 守护进程要求 JDK 25；macOS 可将 `JAVA_HOME` 设置为 `/Applications/Android Studio.app/Contents/jbr/Contents/Home`。

- `./gradlew :app:assembleDebug`：构建调试 APK。
- `./gradlew :app:assembleRelease`：使用本机签名配置构建发布 APK。
- `./gradlew :app:lintDebug`：运行 Android Lint 静态检查。
- `./gradlew :app:connectedDebugAndroidTest`：在连接的真机或模拟器上运行仪器化测试。
- `./gradlew :app:signingReport`：查看签名证书与指纹。

APK 按 `轻控_v<versionName>_<variant>.apk` 命名，输出到 `app/build/outputs/apk/<variant>/`。本机安装示例：`adb install -r 'app/build/outputs/apk/debug/轻控_v1.0_debug.apk'`。

## 代码风格与命名规范

使用四空格缩进和 Android Studio 的 Kotlin 格式化工具。类、文件及 Composable 函数使用 PascalCase，普通函数与属性使用 camelCase，资源使用 snake_case。面向用户的中文文案放入 `res/values/strings.xml`，颜色复用主题配置。系统音量操作集中在 `MediaVolumeController`，仅控制 `STREAM_MUSIC`，避免改动铃声、闹钟和通知设置。生命周期结束或组件停止时，清理对应的回调与轮询任务。

## 测试规范

测试使用 JUnit 4、AndroidJUnitRunner、Compose UI 测试与 Espresso。测试方法使用能描述行为的 camelCase 名称，断言真实系统状态，并在 `finally` 中恢复音量、关闭测试开启的手电筒。当前未配置覆盖率百分比要求。磁贴启动、滑条拖动、权限和深色模式需结合手动验收，记录设备型号与 API 版本，区分环境准备失败和功能断言失败。当前连接设备测试运行器会在结束后卸载被测包，之后需重新安装并按需添加磁贴。

## 提交与拉取请求规范

现有提交使用 `feat:` 前缀与中文说明，后续采用 `<type>: <中文摘要>` 格式，每次提交聚焦一个主题。拉取请求需说明行为变化、验证命令与结果，并关联适用的问题记录；涉及界面变更时附截图。不要混入无关工作区改动、IDE 文件或生成的 APK。

## 安全与本机配置

Release 签名读取已加入 Git 忽略的 `signing/sign_key.properties`。禁止提交密码和密钥库，后续发布更新必须保留并沿用现有正式签名。Debug 与 Release 签名不同，切换安装需先卸载旧包，卸载会清除应用数据。卸载应用或重启用户设备前，须获得用户授权。
