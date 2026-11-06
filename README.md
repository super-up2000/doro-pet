# Doro 桌宠（安卓端）

《胜利女神：NIKKE》桃乐丝 Q 版桌面宠物，基于 PC 版 doro-pet 移植。

## 架构
- 原生 Android 悬浮窗（SYSTEM_ALERT_WINDOW）+ WebView
- WebView 里跑 PixiJS 7 + pixi-live2d-display 0.4 渲染 Live2D 模型
- Live2D Cubism Core 4.2 Web 版
- 素材：完整 Doro model3.json + moc3 + 纹理 + 动作 + 表情

## 安装
1. 克隆本仓库到 Android Studio 能访问的路径
2. 打开 `app/` 用 Gradle 构建（AGP 8.5 + SDK 34）
3. 安装 APK 后，首次启动会引导授予"显示在其他应用上层"权限

## 交互
- **轻点 doro**：摸头 + 台词
- **拖动 doro**：移动位置（可甩到屏幕任意角落）
- **甩抛**：拖拽到边缘快速放开，触发挣扎动画
- **周期性随机**：每 6s 自动换动作、每 12s 有 40% 概率冒台词

## 素材来源
- Live2D 模型：`shengzengyang/DesktopPetMonitor`（开源 Live2D 桌宠框架）
- PC 版 doro-pet：Tauri 打包，素材内嵌
- 前端 JS：npm `pixi.js@7`、`pixi-live2d-display@0.4`、`live2dcubismcore@1.0.2`

## License
个人学习/非商用。素材版权归原作者所有。
