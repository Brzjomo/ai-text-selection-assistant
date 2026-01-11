# AI文本选择助手 (AI Text Selection Assistant)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-orange.svg)](https://developer.android.com/jetpack/compose)

基于Android系统级文本选择API的AI助手，可在任意应用中选中文本并进行AI处理，结果以流式方式在悬浮窗口中展示。

## 使用方法：

### 方式一：

1.选择文本

2.点击“更多”图标（3个点）

3.点击“AI 划词助手”

### 方式二：

1.选择文本

2.点击“分享”（文本格式）

3.点击“AI 划词助手”

## ✨ 核心特性

### 🌐 跨应用文本处理
- **系统级集成**：基于Android `ACTION_PROCESS_TEXT`系统意图
- **全应用兼容**：支持浏览器、社交软件、文档应用等所有文本应用
- **非侵入式**：以Dialog悬浮窗形式呈现，不影响原应用使用

### 🤖 多LLM提供商支持
- **OpenAI GPT系列**：GPT-4o, GPT-4o-mini等
- **DeepSeek系列**：DeepSeek-Chat, DeepSeek-V3等
- **本地部署模型**：Ollama, LocalAI等本地服务
- **自定义API**：兼容任何OpenAI API格式的服务

### 🎨 智能Prompt模板系统
- **预设模板**：翻译、总结、润色、单词解析、语法分析等
- **自定义模板**：支持创建、编辑、删除个性化模板
- **变量支持**：`{{text}}`占位符自动替换为选中文本

### ⚡ 流式响应体验
- **实时打字机效果**：Server-Sent Events (SSE)流式传输
- **响应可控**：处理过程中可随时中断
- **网络优化**：异常自动重试，超时自动处理

### 🎯 现代化用户界面
- **Material Design 3**：Jetpack Compose构建的现代化UI
- **Markdown渲染**：支持代码高亮、表格、列表等格式
- **主题系统**：深色/浅色主题，支持跟随系统、手动切换
- **窗口控制**：支持悬浮窗拖动，灵活调整位置

### 🔧 本地数据管理
- **安全存储**：API密钥等敏感数据安全存储
- **模板管理**：Room数据库持久化存储
- **偏好设置**：用户配置持久化保存

## 🛠️ 技术架构

### 技术栈
| 组件 | 技术选型 |
|------|----------|
| 开发语言 | Kotlin 2.0.21 |
| UI框架 | Jetpack Compose BOM 2024.09.00 |
| 架构模式 | MVVM (ViewModel + StateFlow) |
| 网络库 | Retrofit 2.11.0 + OkHttp 4.12.0 |
| 本地存储 | DataStore Preferences + Room Database |
| Markdown渲染 | com.halilibo:rich-text 0.16.0 |
| 异步处理 | Kotlin Coroutines 1.8.0 + Flow |

### 项目结构
```
ai-text-selection-assistant/
├── app/src/main/java/top/brzjomo/aitextselectionassistant/
│   ├── data/              # 数据层
│   │   ├── local/         # 本地数据实体
│   │   ├── remote/        # 远程数据接口
│   │   └── repository/    # 数据仓库
│   ├── ui/                # UI层
│   │   ├── components/    # 可复用组件
│   │   ├── main/          # 主界面
│   │   ├── process/       # 文本处理界面
│   │   └── theme/         # 主题配置
│   ├── AppContainer.kt    # 依赖注入容器
│   ├── MainActivity.kt    # 应用入口
│   └── ViewModelFactory.kt# ViewModel工厂
├── app/build.gradle.kts   # 应用构建配置
└── gradle/                # Gradle配置
```