# 轻控 · VolumeControl

<img src="app/src/main/res/drawable-nodpi/ic_launcher_artwork.png" width="88" alt="轻控应用图标">

**声音与光，轻松掌控。**

轻控是一个原生 Android 音量控制与手电筒应用，适用于实体音量键损坏，或希望通过屏幕操作调节音量的场景。

## 功能

- **媒体音量控制**：加减按钮每次调整一档，滑条直接选择音量，显示当前百分比和系统档位。
- **系统音量同步**：页面在前台时读取真实媒体音量，反映系统面板等外部来源的变化。
- **下拉快捷磁贴**：提供「媒体音量」入口，点击后收起快捷面板，打开弹窗式滑条进行左右拖动调节。
- **手电筒**：使用系统闪光灯接口，显示真实开关状态，并处理无闪光灯、相机占用和权限不足等情况。
- **浅色与深色主题**：跟随系统外观，主页面可滚动，提供大号音量按钮和中文操作说明。

音量控制仅针对媒体流 `STREAM_MUSIC`，用于音乐、视频和游戏；不会主动调整来电铃声、通知、闹钟或勿扰模式。

## 使用方法

### 应用内操作

打开「轻控」，点击音量加减按钮或拖动滑条即可调整媒体音量。手电筒开关位于音量卡片下方，首次使用时按提示授予相机权限。

### 添加下拉音量磁贴

**Android 10–12：**

1. 安装后先打开一次轻控。
2. 从屏幕顶部下拉两次，展开快捷面板。
3. 点击编辑按钮，将「媒体音量」拖入常用区域。
4. 点击磁贴，在弹出的音量窗口中左右拖动滑条。

**Android 13 及以上：** 可在应用内点击「添加媒体音量磁贴」，再在系统弹窗中确认；也可以手动编辑快捷面板。

长按磁贴可返回应用。应用只注册一个「媒体音量」磁贴，快捷面板里不会出现多个同名入口；旧版「音量＋」「音量－」磁贴已随代码移除，升级后若面板上残留空白位置，在编辑页手动删掉即可。

当前实现使用弹窗式 Activity 承载滑条，磁贴本身是点击入口。磁贴到弹窗的完整真机点击链路仍有待完成最终验收，详见下方验证记录。

## 开发环境

| 项目 | 当前配置 |
| --- | --- |
| 应用名称 / 包名 | 轻控 / `com.ljrh.volumecontrol` |
| 当前版本 | `versionName = 1.0`，`versionCode = 1` |
| 最低系统版本 | Android 10（API 29） |
| compileSdk / targetSdk | 37 / 37 |
| 界面技术 | Kotlin、Jetpack Compose、Material 3 |
| 构建工具 | Gradle Wrapper 9.5.0、Android Gradle Plugin 9.3.3 |
| Gradle JVM | JDK 25；Java 编译目标为 11 |

使用 Android Studio 打开项目并完成 Gradle 同步。通过 SDK Manager 安装对应 Android SDK，SDK 路径保存在不提交的 `local.properties` 中。依赖版本统一维护在 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)。

以下命令均在项目根目录执行。macOS 可使用 Android Studio 自带 JBR：

```bash
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
```

其他环境请将 `JAVA_HOME` 指向本机 JDK 25。

## 构建与安装

### Debug

```bash
./gradlew :app:assembleDebug
adb devices -l
adb install -r 'app/build/outputs/apk/debug/轻控_v1.0_debug.apk'
adb shell am start -n com.ljrh.volumecontrol/.MainActivity
```

### Release

先按下一节准备本机签名配置，再执行：

```bash
./gradlew :app:assembleRelease
adb install -r 'app/build/outputs/apk/release/轻控_v1.0_release.apk'
```

APK 文件名由 [`app/build.gradle.kts`](app/build.gradle.kts) 根据版本号和构建变体自动生成：

| 类型 | 当前输出路径 |
| --- | --- |
| Debug | `app/build/outputs/apk/debug/轻控_v1.0_debug.apk` |
| Release | `app/build/outputs/apk/release/轻控_v1.0_release.apk` |

命名格式为 `轻控_v<versionName>_<variant>.apk`；修改版本号后，请同步调整安装命令中的文件名。

### 安装时提示签名不一致

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` 表示手机中同包名应用的签名与待安装 APK 不同。Debug 与正式 Release 使用不同签名，`adb install -r` 无法绕过这一限制。

首次从 Debug 切换到 Release，需要先卸载旧版。**卸载会清除该应用的数据和权限，音量磁贴也需要重新添加。确认可以清除后再执行：**

```bash
adb uninstall com.ljrh.volumecontrol
adb install 'app/build/outputs/apk/release/轻控_v1.0_release.apk'
```

以后继续使用同一份正式签名构建，即可通过 `adb install -r` 覆盖更新。

## 正式签名配置

Gradle 从本机 `signing/sign_key.properties` 读取签名配置，使用 UTF-8 编码。已有发布签名时，请恢复原密钥库与配置，不要为更新重新生成私钥。

配置格式如下，示例内容需替换为本机实际值：

```properties
# storeFile 相对项目根目录
storeFile=signing/你的发布签名.p12
storeType=PKCS12
storePassword=填写密钥库密码
keyAlias=填写密钥别名
keyPassword=填写密钥密码
```

当前发布密钥库采用 PKCS12（`.p12`）格式。整个 `signing/` 目录已加入 Git 忽略，密码不写入构建脚本。请妥善备份密钥库和配置文件，后续更新必须沿用同一签名。缺少签名材料时仍可构建 Debug；Release 构建会被签名校验拦截。

查看签名证书的 MD5、SHA-1 和 SHA-256：

```bash
./gradlew :app:signingReport
```

正式包接入第三方平台时，查看输出中的 `Variant: release`，不要误用 Debug 指纹。

## 权限与设备限制

| 权限 | 用途 |
| --- | --- |
| `MODIFY_AUDIO_SETTINGS` | 调整系统媒体音量 |
| `CAMERA` | 控制闪光灯；使用手电筒时申请运行时权限 |

手电筒功能不拍照、不录音。当前清单未申请网络、悬浮窗或无障碍服务权限。相机及闪光灯被声明为可选硬件，不支持的设备仍可安装并使用音量功能。固定音量设备或被系统限制的场景会显示不可调整状态。

当前没有常驻前台服务，系统结束应用进程时可能关闭手电筒。不同厂商系统的快捷面板和权限行为需要分别验证。

## 测试与检查

```bash
# Android Lint 静态检查
./gradlew :app:lintDebug

# 连接真机或模拟器后运行仪器化测试
./gradlew :app:connectedDebugAndroidTest
```

测试位于 `app/src/androidTest/`，使用 JUnit 4、AndroidJUnitRunner、Compose UI 测试及 Espresso，覆盖真实媒体音量加减、滑条边界、外部音量同步、弹窗滑条和手电筒开关。测试会在 `finally` 中恢复音量或关闭测试开启的闪光灯。

当前 Gradle 测试运行器会在测试结束后卸载被测应用和测试包；运行后需重新安装，并按需重新添加磁贴。手电筒测试依赖闪光灯硬件和相机权限，部分系统会拒绝自动授予权限，应将环境准备失败与功能断言失败分别记录。

### 已有验证记录

以下为 2026-10-22 的已有结果：

- Debug、Release 构建及正式 APK 签名校验已通过，APK 证书与发布密钥库证书一致。
- 一加 6 / Android 11 的首版测试为 4 项通过，其中磁贴验证对应旧版加减操作。
- 一加 7 Pro / Android 12 的测试为 5 项、4 项通过；手电筒项在自动授予相机权限时失败，未进入功能断言。弹窗滑条自身操作真实媒体音量的测试通过。
- 2026-10-22 移除旧磁贴后，在一加 6 / Android 11 上复验：「媒体音量」磁贴点击后收起面板并弹出滑条，拖动把真实媒体音量从第 14 档改到第 26 档，磁贴副标题随系统音量回到「当前 47%」。Android 13+ 的添加磁贴分支仍未做真机验收。

详细过程、历史截图及边界见[使用与验收记录](docs/20261022211600_轻控使用与验收.md)。历史文档中的本机签名文件名可能已变更，签名路径以当前 `app/build.gradle.kts` 和 `signing/sign_key.properties` 为准。

## 项目结构

```text
app/
  src/main/
    java/com/ljrh/volumecontrol/
      ui/                          # View 层
        MainActivity.kt            # 主页面、权限和添加磁贴
        VolumeControlScreen.kt     # Compose 主界面
        VolumeSliderActivity.kt    # 弹窗式音量滑条
        theme/                     # 主题与字体
      viewmodel/                   # ViewModel 层
        VolumeViewModel.kt         # 音量状态与轮询
        TorchViewModel.kt          # 手电筒状态
      data/                        # Model 层
        MediaVolumeController.kt   # 媒体音量读写
        TorchController.kt         # 闪光灯回调与状态
      service/                     # 系统组件
        MediaVolumeTileService.kt  # 唯一的媒体音量磁贴
    res/                         # 文案、图标和主题资源
    AndroidManifest.xml          # 组件与权限声明
  src/androidTest/                # 设备仪器化测试
  build.gradle.kts                # 构建、签名与 APK 命名
gradle/                          # Wrapper、JVM 与依赖版本配置
signing/                         # 本机签名材料，Git 忽略
artwork/                         # 应用图标原稿
docs/                            # 使用说明与验收证据
AGENTS.md                        # 中文贡献指南
```

参与开发前请阅读[仓库贡献指南](AGENTS.md)。

## 开源协议

本项目基于 [MIT 协议](LICENSE) 开源，可自由使用、修改与分发，请保留原始版权声明。

