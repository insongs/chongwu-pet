# 🐑 咩咩宠物 - Android 桌面小羊

一只会用 Canvas 绘制的可爱桌面宠物小羊，完全零图片资源！最小 APK 仅约 **2-3MB**。

## ✨ 功能特色

| 交互方式 | 咩咩的反应 |
|---------|-----------|
| 👆 点击头部 | 开心跳跃 🐑♥ |
| 🌀 点击角 | 头晕目眩 ✦ |
| 🤗 点击身体 | 害羞享受抚摸 💕 |
| 🦵 点击腿 | 吓一跳！❗ |
| 🔄 点击尾巴 | 开心摇尾巴 ♪ |
| ↔️ 拖拽移动 | 跟着你的手指走 |
| 🧗 拖到屏幕边缘 | 沿边缘攀爬！ |
| 💤 不理它 | 会自己做可爱小动作 |

### 表情系统
😊 开心 · 😢 难过 · 😠 生气 · 😲 惊讶 · 😴 睡觉 · 😵 晕眩 · 😇 无辜 · ☺️ 满足 · 😆 大笑

### 动画效果
- 🏃 走路摆腿 · 🦘 跳跃 · 🧗 攀爬 · 🌀 转圈晕
- ♥ 爱心漂浮 · ✦ 星星转圈 · zZz 呼噜 · ♪ 音符飞舞
- 尾巴摇摆 · 耳朵抖动 · 身体呼吸起伏 · 眨眼

## 📦 项目结构

\\\
chongwu/
├── app/
│   ├── build.gradle.kts              # App 构建配置
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml        # 权限声明 (悬浮窗/前台服务)
│       ├── java/com/chongwu/pet/
│       │   ├── MainActivity.kt        # 主界面 (权限引导)
│       │   ├── PetOverlayService.kt   # 悬浮窗服务 (Overlay)
│       │   └── SheepView.kt           # 🎨 核心: Canvas 绘制 + 触摸交互
│       └── res/                       # 资源文件
├── build.gradle.kts                   # 根项目配置
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── gradlew.bat                        # Gradle 构建脚本
\\\

## 🔧 构建步骤

### 方法一：Android Studio（推荐）

1. **安装 Android Studio**
   - 从 https://developer.android.com/studio 下载

2. **打开本项目**
   - 启动 Android Studio → **Open an existing project**
   - 选择本项目的 \chongwu\ 文件夹
   - 等待 Gradle 同步完成（首次需要下载依赖）

3. **连接你的安卓手机**
   - 开启手机的 **开发者选项** 和 **USB 调试**
   - USB 连接电脑，手机上允许调试

4. **运行**
   - 点击工具栏的 ▶ **Run** 按钮
   - 选择你的手机 → 等待安装

### 方法二：命令行构建

1. **安装 JDK 17+** 和 **Android SDK**
   - 设置环境变量 \JAVA_HOME\ 和 \ANDROID_HOME\

2. **构建 APK**
   \\\ash
   .\gradlew.bat assembleDebug
   \\\
   APK 生成在 \pp/build/outputs/apk/debug/app-debug.apk\

## 📱 首次使用指引

1. 安装 APK 后打开「咩咩宠物」App
2. 点击 **「启动宠物」** 按钮
3. 允许 **悬浮窗权限**（不同手机路径不同）
4. 建议允许 **忽略电池优化**（防止后台被杀死）
5. 🎉 咩咩出现在屏幕上啦！

## 🧠 最低内存设计原理

\\\
方案                    APK 体积    运行时内存
原生 Kotlin + Canvas   ~2-3MB     ~15-25MB
React Native           ~20MB      ~50-80MB  
Flutter                ~15MB      ~40-60MB
\\\

- 所有图形用 **Canvas 2D** 绘制 → 零图片资源
- 使用 **View** 而非 SurfaceView → 无额外 GPU 纹理
- 动画用 **Handler + invalidate()** → 无动画框架开销
- ProGuard 混淆+压缩 → 极小 APK

## 🎨 扩展想法

- [ ] 添加喂食功能（咩咩会吃东西）
- [ ] 多种宠物形象切换（猫/狗/兔子）
- [ ] 天气感应（下雨时会躲起来）
- [ ] 触摸音效（咩咩叫声）
- [ ] 宠物成长系统
- [ ] 桌面小部件（Widget）

---

**祝你跟咩咩玩得开心！** 🐑✨
