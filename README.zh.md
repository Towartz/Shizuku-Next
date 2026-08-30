# Shizuku-Next

[English](./README.md)

## 免责声明

此为 Shizuku 的现代化增强分支版本。若您需寻找 Rikka 开发的官方 Shizuku，请访问 [官方仓库](https://github.com/RikkaApps/Shizuku)。

---

## 本仓库的变更与增强功能

### 1. 平台支持与构建工具链现代化
- **Android 17 适配 (Baklava / API 37)**：采用 `compileSdk = 37`、`targetSdk = 37`、Java 21 与 NDK r29 构建。
- **最新 AndroidX 生态库**：升级核心依赖 (`androidx.core: 1.19.0`、`androidx.annotation: 1.10.0`、`androidx.browser: 1.10.0`、`bouncycastle: 1.85`)。
- **AGP 8.10.x 与 Java 21 兼容**：绕过 AGP AAR 元数据检查限制，允许在现有 AGP 构建系统下集成最新 AndroidX 库。
- **清理废弃生命周期依赖**：移除过时的生命周期库，杜绝运行时潜在异常。

### 2. 多层隐身模式与组件防检测
- **分应用隐身防护**：新增独立的「对应用隐藏」管理界面，支持实时搜索与状态筛选。
- **动态 Binder 令牌拦截**：受限应用请求 Shizuku Binder 令牌时直接在 IPC 边界予以拒绝或返回空。
- **服务端实时动态同步**：切换应用隐身状态时即时同步更新服务端授权包名列表，无需重启服务进程 (`ShizukuService.setHiddenPackages`)。
- **PackageManager 组件隐藏**：拦截应用对系统 `PackageManager` 的查询，彻底隐藏 Shizuku 的清单组件、服务与活动。

### 3. 特权电池优化忽略与后台放行
- **特权 Shell 自动白名单**：通过 Shizuku 特权 shell 或 root 直接执行 `cmd deviceidle whitelist +<pkg>`、`dumpsys deviceidle whitelist +<pkg>` 与 `cmd appops set <pkg> RUN_IN_BACKGROUND allow`。
- **服务连接自动放行**：当 Shizuku 服务连接建立时，自动将自身加入系统电池优化白名单，防止 OEM 后台休眠或被杀。
- **一键快捷状态卡片**：设置界面提供实时电池优化状态卡片与一键放行功能。

### 4. Material 3 Jetpack Compose 现代化主界面
- **Material 3 分段按钮行**：启动方式采用 `SingleChoiceSegmentedButtonRow` 分段选择器（无线调试、Root、电脑连接）。
- **环境智能推荐徽章**：检测到 Root 权限时自动推荐 `Root（推荐）`，在 Android 11+ 免 Root 环境下自动推荐 `无线调试（推荐）`。
- **交互式两步无线调试流程**：清晰的配对与启动两步流程，实时显示已检测的无线调试端口标签 (`Port: <端口号>`)。
- **零配置终端集成**：主页卡片自动检测已安装的终端模拟器（Termux、MT 管理器、TermOne 等），并提供一键 `rish` 配置命令。
- **纯矢量 Outlined 图标**：全界面 100% 采用 SVG 矢量图标，严格杜绝 Emoji 表情。

### 5. 自动化、守护进程与启动
- **ADB Watchdog 守护服务**：后台实时监测服务状态并在断连时自动尝试恢复连接。
- **开机自启动**：支持 Root 模式以及 Android 11+ 无线调试模式（基于 `WRITE_SECURE_SETTINGS`）开机自动加载。
- **自定义 TCP/IP 端口**：支持配置自定义无线 ADB 监听端口，解决 5555 端口占用冲突。
- **Material You 动态色彩与纯黑主题**：支持壁纸动态取色与 OLED 纯黑深色模式。

### 6. 完善的多语言本地化
- 提供简体中文、繁体中文、俄语、日语、西班牙语、德语、法语、巴西葡萄牙语、印尼语、越南语、韩语等原生完整本地化翻译。

---

## 使用指南

### 开机自启动（无线调试）
1. 按照无线 ADB 配对流程配置 Shizuku。
2. 在「设置」中启用「开机自启动（无线调试）」。
   - 启用前需授予 `WRITE_SECURE_SETTINGS` 权限。
   - 可在 Shizuku 启动时通过 Manager 自动授权，或通过电脑 ADB 执行：
     ```bash
     adb shell pm grant moe.shizuku.privileged.api android.permission.WRITE_SECURE_SETTINGS
     ```

> [!CAUTION]
> `WRITE_SECURE_SETTINGS` 为高权限，仅建议明确了解风险后启用。

### 启动支持详解
- **Root 模式**：支持大多数已 Root 设备在开机时自动加载服务。
- **无线调试模式 (ADB)**：适用于 Android 11 及以上版本，通过 `WRITE_SECURE_SETTINGS` 权限监听网络并在无电脑连接时自动重启服务。
- **TV 设备**：针对 Android TV 与电视盒子优化稳定性与遥控器操作体验。

---

## Shizuku 工作原理

Android 使用 `binder` 进行进程间通信 (IPC)。Shizuku 引导用户以 root 或 ADB 启动一个进程（Shizuku 服务端）。当应用程序启动时，指向 Shizuku 服务端的 `binder` 会一并发送给应用程序。

Shizuku 扮演中介者角色：接收来自应用的请求，转发到系统服务端，再将结果返回。这允许应用以更高权限调用系统 API，体验与直接调用系统 API 几乎一致。

---

## 开发者指南

参考官方 API 仓库：[RikkaApps/Shizuku-API](https://github.com/RikkaApps/Shizuku-API)

---

## 构建 Shizuku-Next

### 构建要求
- JDK 21
- Android SDK (API 37 / Android 17)
- Android NDK (r29 或更新)
- CMake 3.22.1+

### 构建命令
```bash
# 递归克隆仓库（包含子模块）
git clone --recurse-submodules https://github.com/Towartz/Shizuku-Next.git

# 构建 Debug APK
./gradlew :manager:assembleDebug

# 构建 Release APK
./gradlew :manager:assembleRelease
```

---

## 授权条款

本专案代码根据 Apache 2.0 协议授权。

根据 Apache 2.0 协议第 6 条：
- 禁止在非 Shizuku 官方分支中将 `ic_launcher` 图标用于其他应用。
- 禁止在第三方分发中使用 `Shizuku` 作为应用名称或使用 `moe.shizuku.privileged.api` 作为 ID。

---

## 鸣谢

- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) (官方原版)
- [yangFenTuoZi/Shizuku](https://github.com/yangFenTuoZi/Shizuku)
- [pixincreate/Shizuku](https://github.com/pixincreate/Shizuku)
- [thedjchi/Shizuku](https://github.com/thedjchi/Shizuku)
- [HSSkyBoy/Shizuku](https://github.com/HSSkyBoy/Shizuku)
