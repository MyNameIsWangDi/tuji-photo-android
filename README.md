# 途记 · photo（Android）

照片编辑 App 安卓端，依据「暗房修图台」设计稿（Ardot 文件 745924180878045 / 745928099421772）实现。

## 技术栈

- Kotlin + Jetpack Compose（Material3）
- MVVM + Repository
- Room（编辑项目持久化）
- Hilt（依赖注入）
- Coil（图片加载）

## 功能

- **图库首页**：最近照片 3 列网格、继续编辑卡片、系统相册选图（Photo Picker）
- **编辑器**：照片画布 + 底部常驻横向工具栏（调色/滤镜/水印/边框/裁剪/文字），点击工具后功能面板从工具栏上方滑出，面板首行为该功能的二级功能 chips
  - 调色：亮度/对比度/饱和度/色温，实时 ColorMatrix 预览
  - 滤镜：10 预设（暖阳/胶片/黑白/青橙/复古/清冷/浓郁/青柠/蜜桃）+ 强度
  - 水印：文字/图片、3×3 位置九宫格、不透明度
  - 边框：无边框/白边/黑边/圆角/拍立得 + 宽度 + 颜色
  - 裁剪：比例 chips + 旋转
  - 文字：字体/颜色/大小
- **模板页**：分类 chips + 模板卡网格 + 主题外观横向选择列表
- **主题系统**：暗房黑 / 晨光白 / 胶片暖 / 青野 / 墨玉灰，5 套色板共用同一结构 token，SharedPreferences 持久化

## 构建

```
./gradlew :app:assembleDebug
```

### 构建环境（已验证通过的组合）

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | AGP 8.5 要求 17+；JDK 24 不被 Gradle 8.10 支持 |
| Gradle | 8.10.2 | wrapper 已包含 |
| Android SDK | platform 34 + build-tools 34.0.0 | `local.properties` 里配 `sdk.dir` |
| AGP / Kotlin / KSP | 8.5.2 / 2.0.20 / 2.0.20-1.0.25 | |

构建产物：`app/build/outputs/apk/debug/app-debug.apk`

国内网络建议给 Gradle 加镜像（`~/.gradle/init.d/mirrors.gradle`，阿里云 google/public 源），并把 `GRADLE_USER_HOME` 指到非 C 盘。
