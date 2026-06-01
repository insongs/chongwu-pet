# 🐑 咩咩宠物 · 3D 花影羚羊桌面宠物

花影羚羊（灵感来自《洛克王国》花影羚羊）—— 一只全息 3D Q 版桌面宠物，使用 **OpenGL ES 2.0** 硬件渲染。
支持悬浮窗模式，零图片资源，纯程序化建模与渲染。

## ✨ 功能特色

- **3D 卡通渲染** — Cel Shading（三阶光照）+ 法线贴图 + 边缘光晕
- **AI 大脑状态机** — 15 种行为状态（散步、吃草、睡觉、蹦跳、探索、好奇、顶角、惊吓…）
- **生态模拟** — 天气系统（晴天/多云/下雨/下雪/刮风/雷暴/起雾）、昼夜交替、草地生长、蝴蝶飞舞
- **触摸交互** — 头部/角/身体/腿/尾巴 不同部位不同反馈特效（爱心/星星/音符/红晕）
- **程序化音效** — 不用音频文件，数学合成咩咩叫声、环境音、吃草声
- **花瓣装饰** — 蓝色半透明角、身体花影纹样、季节色彩变化

### 交互反馈

| 部位 | 反馈 |
|------|------|
| 👆 头部 | 爱心漂浮 + 开心叫声 |
| 🌀 角 | 星星旋转 |
| 🤗 身体 | 脸红害羞 |
| 🦵 腿 | 感叹号受惊 |
| 🔄 尾巴 | 音符飞舞 + 开心摇尾 |
| ↔️ 拖拽 | 全屏自由拖动 |

### 表情系统
😊 开心 · 😢 难过 · 😠 生气 · 😲 惊讶 · 😴 睡觉 · 😇 无辜 · ☺️ 满足

### 生态特效
- 🌤️ 天气变化（7 种）
- 🌙 昼夜交替（影响行为）
- 🌿 草地生长 / 吃草
- 🦋 蝴蝶飞舞
- 🌸 花朵随风摇曳
- 💧 下雨 / 🌨️ 下雪 / 🌫️ 起雾

## 📦 项目结构

\\\
chongwu/
├── app/
│   ├── build.gradle.kts               # App 构建 (OpenGL ES 2.0)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml         # 权限声明 (悬浮窗/前台服务)
│       ├── java/com/chongwu/pet/
│       │   ├── MainActivity.kt         # 主界面 (权限引导)
│       │   ├── PetOverlayService.kt    # 悬浮窗服务 (全屏 GLSurfaceView)
│       │   ├── GLSheepView.kt          # GLSurfaceView 渲染视图
│       │   ├── ai/
│       │   │   └── SheepState.kt       # AI 大脑状态机 + 需求系统
│       │   ├── audio/
│       │   │   └── AudioEngine.kt      # 程序化音效引擎
│       │   ├── ecology/
│       │   │   └── Environment.kt      # 生态模拟 (天气/昼夜/草地/蝴蝶)
│       │   ├── interaction/
│       │   │   └── TouchEngine.kt      # 触摸交互引擎
│       │   ├── model/
│       │   │   ├── Model3D.kt          # 3D网格模型 (VBO/IBO)
│       │   │   ├── Camera.kt           # 透视摄像机
│       │   │   ├── PrimitiveBuilder.kt # 几何体构建器 (球/柱/锥/环/平面)
│       │   │   └── SheepModel3D.kt     # 花影羚羊模型组装
│       │   └── render/gl/
│       │       ├── PetRenderer.kt      # 主渲染器 (场景+粒子+特效)
│       │       ├── ShaderProgram.kt    # 卡通着色器 (Cel Shading)
│       │       └── ShaderHelper.kt     # 着色器编译工具
│       └── res/                        # 资源文件
├── build.gradle.kts                    # 根项目配置
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew.bat
├── sheep.html                          # 2D Canvas 预览版 (备用)
└── server.js                           # 简单 HTTP 服务器 (用于预览 sheep.html)
\\\

## 🔧 构建步骤

### 方法一：Android Studio（推荐）

1. **安装 Android Studio**
   - 从 https://developer.android.com/studio 下载

2. **打开本项目**
   - 启动 Android Studio → **Open an existing project**
   - 选择本项目文件夹
   - 等待 Gradle 同步完成

3. **连接安卓手机**
   - 开启 **开发者选项** + **USB 调试**
   - USB 连接电脑，允许调试

4. **运行**
   - 点击 ▶ **Run** / `Shift+F10`

### 方法二：命令行

\\\ash
.\gradlew.bat assembleDebug
\\\

APK 输出路径：\`app/build/outputs/apk/debug/app-debug.apk\`

## 📱 首次使用

1. 安装 APK 并打开「咩咩宠物」
2. 点击 **「启动宠物」**
3. 允许 **悬浮窗权限**
4. 建议 **关闭电池优化**（防后台杀死）
5. 🎉 花影羚羊出现在屏幕啦！

## 🧠 内存与性能

| 指标 | 数据 |
|------|------|
| APK 体积 | ~3MB（零图片零音频） |
| 运行时内存 | ~20-35MB |
| 渲染方式 | OpenGL ES 2.0 硬件加速 |
| 音效 | 程序化合成（PCM） |
| 模型 | 纯数学几何体构建 |

## 🎨 技术栈

- **Kotlin** + **Android SDK** (minSdk 26)
- **OpenGL ES 2.0** — 自定义 GLSL 着色器
- **Cel Shading** — 三段式卡通光照
- **View/WindowManager** — 悬浮窗叠加层
- **AudioTrack** — PCM 音频合成
- **状态机** — 15 状态 + 4 维需求系统

---

**祝你跟花影羚羊玩得开心！** 🐑✨
