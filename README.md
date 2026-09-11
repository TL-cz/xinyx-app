# xinyx射击 · Android App 壳

这是 xinyx-tps 游戏的 Android WebView 壳，**不会把游戏变成原生**，但能：
- 有 App 图标 + 启动页
- 真正强制横屏锁（不用再弹竖屏遮罩）
- 隐藏状态栏，沉浸式全屏
- 从桌面直接点开，体验接近原生

## 🚀 打包 APK (两条路, 任选其一)

---

### 路径 A · GitHub Actions 在线打包 (推荐, 零本地环境)

**前提**: 一个 GitHub 账号

1. **把整个 `apps/xinyx-app` 目录推到 GitHub 仓库**
   ```bash
   git add apps/xinyx-app
   git commit -m "add xinyx android app shell"
   git push
   ```

2. **生成自签名密钥** (只需一次):
   在任意装了 JDK 的机器上跑, 或者 GitHub 自己的 Actions 里临时跑:
   ```bash
   keytool -genkey -v -keystore release.keystore \
     -alias xinyx -keyalg RSA -keysize 2048 -validity 10000
   ```
   会让你设一个密码（记好）, 最终生成一个 `release.keystore` 文件。

3. **把密钥转 Base64 存 GitHub Secret**:
   ```bash
   base64 release.keystore | tr -d '\n'  # Mac/Linux
   certutil -encode release.keystore out.b64; type out.b64    # Windows
   ```
   去 GitHub 仓库 → **Settings → Secrets and variables → Actions → New repository secret**:
   - 新建 `KEYSTORE_BASE64` (粘贴上面的 base64 内容)
   - 新建 `KEYSTORE_PASSWORD` (你设的那个密码)

4. **跑 Actions**:
   - push 到 main 分支会自动触发
   - 或 Actions 标签页 → Build xinyx APK → Run workflow 手动触发

5. **下载 APK**:
   - Actions 完成 → Summary 页 → Artifacts → 下载 `xinyx-release`
   - 解压拿到 `app-release.apk`, 发给朋友用

---

### 路径 B · 本地装 Android Studio 打包 (想离线打或调试)

1. **装 Android Studio** (≈1GB, 含 JDK 17 + Android SDK + Gradle):
   https://developer.android.com/studio

2. **初始化壳工程**:
   ```bash
   cd apps/xinyx-app
   npm install
   npx cap add android    # 生成 android/ 目录 (首次慢)
   ```

3. **装 JDK 密钥**:
   ```bash
   keytool -genkey -v -keystore release.keystore \
     -alias xinyx -keyalg RSA -keysize 2048 -validity 10000
   ```
   把 `release.keystore` 放到 `apps/xinyx-app/android/app/` 下, 然后在 `android/app/build.gradle` 里配 signingConfig (或用 Android Studio 的 Build → Generate Signed APK 向导)。

4. **打包**:
   ```bash
   # 命令行
   cd apps/xinyx-app/android
   ./gradlew assembleRelease
   # 产物: app/build/outputs/apk/release/app-release.apk
   
   # 或 Android Studio: Open... 选 android/ 目录 → Build → Generate Signed Bundle/APK
   ```

5. **发给朋友安装**:
   - APK 通过微信/QQ/网盘传过去
   - 手机打开允许"安装未知来源应用"即可

---

## 📋 配置项

所有配置在 `capacitor.config.json`, 常用字段:

| 字段 | 说明 |
|---|---|
| `appId` | 包名, 改了就跟之前装的冲突 |
| `appName` | 桌面图标下显示的名字 |
| `server.url` | WebView 加载的地址, 换服务器改这里 |
| `plugins.SplashScreen.launchShowDuration` | 启动页显示毫秒 |

## 🖼️ 图标

把 `icon-192.png` 和 `icon-512.png` 放到 `src/` 下 (任意 PNG)。
`cap sync` 时 Capacitor 会自动生成 Android 各尺寸。

## ⚠️ 注意

- **WebView 版本**: 需要 Android System WebView ≥ 87 (约 2021 年后的手机)。启动页有 WebView 版本检测, 太旧会提示升级。
- **服务器可达**: WebView 需要能访问 `http://8.134.192.185:4000`。如果服务器挂了会显示"网络连接失败"。
- **横屏锁死**: Capacitor 的 `landscape` 锁比 H5 靠谱, 竖屏遮罩还是留着当兜底。
- **签名密钥**: 自签名足够给朋友装。以后想换包名或上应用商店, 需要改 `appId` 并重签。
