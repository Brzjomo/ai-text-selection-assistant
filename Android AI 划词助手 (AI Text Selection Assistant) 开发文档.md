# Android AI 划词助手 (AI Text Selection Assistant) 开发文档

*本文档版本：2.0（优化版）*
*最后更新日期：2026年1月7日*
*基于原始文档 v1.0 优化和完善*

## 目录

1. [项目概述](#1-项目概述)
2. [开发环境搭建](#2-开发环境搭建)
3. [项目架构设计](#3-项目架构设计)
4. [核心技术实现](#4-核心技术实现)
5. [详细开发指南](#5-详细开发指南)
6. [测试策略](#6-测试策略)
7. [部署和发布](#7-部署和发布)
8. [维护和扩展](#8-维护和扩展)
9. [附录](#9-附录)

---

## 1. 项目概述

### 1.1 项目背景和目标

本项目旨在开发一款 Android 辅助工具应用，通过系统级文本选择操作（`ACTION_PROCESS_TEXT`），实现跨应用（如浏览器、社交软件）获取文本，利用用户自定义的 LLM API（如 OpenAI, DeepSeek, Ollama）和自定义 Prompt 模版进行翻译、总结、润色等处理，并以流式（Streaming）方式在悬浮窗口展示结果。

#### 项目核心价值
1. **跨应用文本处理**：突破应用边界，在任何应用中选中文本即可调用 AI 处理
2. **高度自定义**：用户可配置自己的 LLM API 和 Prompt 模板
3. **流式实时响应**：提供类似 ChatGPT 的打字机式流式输出体验
4. **悬浮窗交互**：非侵入式 Dialog 界面，不影响原应用使用

#### 目标用户群体
- **效率工作者**：需要快速翻译、总结文档内容
- **开发者**：需要解释代码、技术文档
- **学生/研究人员**：需要文献翻译、内容摘要
- **内容创作者**：需要文本润色、改写辅助

### 1.2 核心功能特性

#### 核心功能清单

1. **跨应用文本选择**
   - 通过 `ACTION_PROCESS_TEXT` 系统意图获取任意应用中的选中文本
   - 自动唤起 AI 处理悬浮窗
   - 支持浏览器、社交软件、文档应用等绝大多数文本应用

2. **多 LLM 提供商支持**
   - OpenAI GPT 系列 (GPT-4o, GPT-4o-mini)
   - DeepSeek 系列 (DeepSeek-Chat, DeepSeek-V3)
   - 本地部署模型 (Ollama, LocalAI)
   - 任何兼容 OpenAI API 格式的服务

3. **智能 Prompt 模板系统**
   - 预置常用模板：翻译、总结、润色、解释代码等
   - 自定义模板创建和管理
   - 模板变量系统（支持 `{{text}}` 等占位符）
   - 模板分类和快速切换

4. **流式响应输出**
   - 支持 Server-Sent Events (SSE) 流式传输
   - 实时打字机效果显示
   - 响应过程中可中断
   - 网络异常自动重试

5. **现代化用户界面**
   - Jetpack Compose 构建的 Material Design 3 界面
   - 悬浮窗 Dialog 设计，不影响原应用
   - Markdown 格式渲染（支持代码高亮、表格、列表等）
   - 深色/浅色主题适配

6. **本地数据管理**
   - API 配置安全存储（DataStore）
   - Prompt 模板数据库（Room）
   - 用户偏好设置持久化

7. **实用辅助功能**
   - 一键复制处理结果
   - 重新生成响应
   - 历史记录查看（可选扩展）
   - 多语言界面支持（可选扩展）

### 1.3 技术选型说明

#### 技术栈决策矩阵

| **模块**     | **技术方案**                  | **选择理由**                                 |
| ------------ | ----------------------------- | ---------------------------------------- |
| **开发语言** | Kotlin                        | Android 官方首选，空安全，协程支持好      |
| **UI 框架**  | Jetpack Compose               | 声明式UI，代码简洁，现代化，性能优秀     |
| **架构模式** | MVVM                          | 数据与UI解耦，便于测试和维护             |
| **网络请求** | Retrofit + OkHttp             | RESTful API 支持好，**必须支持 SSE 流式响应** |
| **本地存储** | DataStore (Proto/Preferences) | 类型安全，协程支持，替代 SharedPreferences |
| **数据库**   | Room                          | SQLite 抽象层，编译时检查，协程支持      |
| **Markdown** | com.halilibo:rich-text        | Compose 兼容，支持代码高亮，维护活跃     |
| **异步处理** | Coroutines + Flow             | 官方推荐，流式数据处理自然               |

#### 技术选型详细说明

**1. Kotlin**
- **版本要求**：Kotlin 1.9+，兼容 Android API 级别 24+
- **关键特性**：空安全、扩展函数、协程、数据类
- **替代方案考虑**：Java（开发效率低，缺乏现代语言特性）

**2. Jetpack Compose**
- **版本要求**：Compose BOM 2024.08.00+
- **关键特性**：声明式 UI、状态驱动、实时预览
- **替代方案考虑**：传统 XML + View 系统（代码冗余，维护困难）

**3. MVVM 架构**
- **实现方式**：ViewModel + LiveData/StateFlow
- **数据流**：View → ViewModel → Repository → Data Source
- **优势**：关注点分离，UI 状态可测试，生命周期感知

**4. Retrofit + OkHttp**

- **SSE 支持**：需要自定义 OkHttp Interceptor 处理流式响应
- **版本要求**：Retrofit 2.10+，OkHttp 4.12+
- **序列化**：使用 Kotlinx.serialization 或 Gson

**5. DataStore**
- **类型**：Proto DataStore（类型安全）或 Preferences DataStore
- **适用场景**：API Key、Base URL、用户偏好设置
- **迁移**：从 SharedPreferences 迁移的指导

**6. Room**
- **版本要求**：Room 2.6+
- **关键配置**：数据库版本管理、迁移策略
- **性能优化**：索引、DAO 设计、事务处理

**7. Markdown 渲染**
- **库选择**：com.halilibo:rich-text 或 com.github.jeziellago:compose-markdown
- **特性要求**：Compose 兼容、代码高亮、自定义样式
- **备选方案**：WebView 渲染（性能差，兼容性问题）

**8. 协程和 Flow**
- **并发模式**：结构化并发，避免内存泄漏
- **Flow 使用**：StateFlow 用于 UI 状态，SharedFlow 用于事件
- **异常处理**：协程异常处理器，SupervisorJob

#### 兼容性考虑
- **最低 API 级别**：24（Android 7.0 Nougat），覆盖 95%+ 设备
- **权限要求**：无需特殊权限，依赖系统 `ACTION_PROCESS_TEXT`
- **存储权限**：仅应用内部存储，无需外部存储权限
- **网络权限**：需要 INTERNET 权限访问 LLM API

---

## 2. 开发环境搭建

### 2.1 Android Studio 安装和配置

#### 安装要求

1. **操作系统**：
   - Windows 10/11 (64-bit)
   - macOS 10.14 (Mojave) 或更高版本
   - Linux (Ubuntu 18.04+ 或类似发行版)

2. **硬件要求**：
   - 最低 8GB RAM，推荐 16GB RAM
   - 至少 8GB 可用磁盘空间（建议 16GB）
   - 1280x800 最小屏幕分辨率

3. **Java 要求**：
   - JDK 17 或更高版本（Android Studio 自带 JDK）

#### 下载和安装

1. **下载 Android Studio**
   - 访问 [developer.android.com/studio](https://developer.android.com/studio)
   - 下载适合你操作系统的版本
   - 验证文件完整性（可选）

2. **安装步骤**
   - **Windows**：运行 `.exe` 安装程序，按向导完成
   - **macOS**：拖拽到 Applications 文件夹
   - **Linux**：解压并运行 `./studio.sh`

3. **首次运行配置**
   - 选择安装类型：Standard（标准）或 Custom（自定义）
   - 选择主题：Light（浅色）或 Darcula（深色）
   - 下载必要的 SDK 组件
   - 配置模拟器（可选）

#### 必要插件安装

1. **Kotlin 插件**（通常已内置）
2. **Compose 插件**：
   - 打开 Settings → Plugins
   - 搜索 "Android Compose"
   - 安装并重启

3. **推荐插件**：
   - **JSON to Kotlin Class**：JSON 转 Kotlin 数据类
   - **Material Theme UI**：美化 IDE
   - **GitToolBox**：Git 增强
   - **Rainbow Brackets**：彩色括号

#### SDK 配置

1. **SDK Manager**：
   - 打开 Tools → SDK Manager
   - 安装以下组件：
     - Android SDK Platform 34（或最新）
     - Android SDK Build-Tools 34+
     - Android Emulator
     - Android SDK Platform-Tools
     - NDK（可选）

2. **环境变量配置**（可选）：
   - `ANDROID_HOME`：指向 SDK 目录
   - 将 `platform-tools` 和 `tools` 添加到 PATH

#### 验证安装

1. **创建测试项目**：
   - File → New → New Project
   - 选择 "Empty Activity"
   - 点击 Finish

2. **构建和运行**：
   - 点击 Run 按钮 (▶)
   - 选择模拟器或连接真机
   - 验证应用能否正常运行

3. **常见问题排查**：
   - **Gradle 构建失败**：检查网络，使用国内镜像
   - **模拟器无法启动**：启用虚拟化技术（BIOS/UEFI）
   - **设备无法识别**：安装 USB 驱动（Windows）

### 2.2 项目创建步骤

#### 步骤1：新建项目

1. **打开 Android Studio**
2. **选择 "New Project"**
3. **选择模板**：
   - `Empty Activity`（手动添加 Compose）
4. **配置项目**：
   - **Name**：`AI Text Selection Assistant`
   - **Package name**：`com.yourname.aitext`（替换 yourname）
   - **Save location**：选择项目保存路径
   - **Language**：`Kotlin`
   - **Minimum SDK**：`API 24: Android 7.0 (Nougat)`
   - **Build configuration language**：`Kotlin DSL`（推荐）
5. **点击 "Finish"**

#### 步骤2：项目结构验证

1. **等待 Gradle 同步完成**
2. **检查关键文件**：
   - `app/build.gradle.kts`：项目配置
   - `app/src/main/AndroidManifest.xml`：清单文件
   - `app/src/main/java/com/yourname/aitext/MainActivity.kt`：主Activity
   - `app/src/main/res/values/themes.xml`：主题配置

3. **运行测试**：
   - 连接设备或启动模拟器
   - 点击 Run 按钮 (▶)
   - 验证空白应用能否正常运行

#### 步骤3：基础配置修改

1. **修改 AndroidManifest.xml**：
   - 添加 `ACTION_PROCESS_TEXT` intent-filter（后续）
   - 配置权限和主题

2. **更新 build.gradle.kts**：
   - 添加必要依赖（下一节详述）
   - 配置编译选项

3. **创建目录结构**：
   - 按照 [3.2 目录结构详解](#32-目录结构详解) 创建包和目录

#### 步骤4：版本控制初始化

1. **初始化 Git**：
```bash
git init
git add .
git commit -m "Initial commit: Android AI 划词助手项目"
```

2. **创建 .gitignore**（Android Studio 通常已生成）：
```
*.iml
.gradle
.local
build/
captures/
*.apk
*.ap_
*.jar
*.class
```

### 2.3 依赖库配置

#### build.gradle.kts 完整配置

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.yourname.aitext"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourname.aitext"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // 或使用 Proto DataStore
    // implementation("androidx.datastore:datastore:1.1.1")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Kotlin Serialization（可选，用于 JSON 序列化）
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    // Markdown Rendering
    implementation("com.halilibo:rich-text:0.20.2")
    // 或使用：implementation("com.github.jeziellago:compose-markdown:0.6.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// proguard-rules.pro 配置（可选）
/*
-keep class com.yourname.aitext.** { *; }
-keep class * implements com.yourname.aitext.data.remote.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
*/
```

#### 依赖版本说明

1. **Compose BOM**：使用 BOM 管理 Compose 依赖版本，确保兼容性
2. **Kotlin 版本**：与 Android Studio 内置版本保持一致
3. **Room 版本**：2.6.1 支持 Kotlin 协程和 Flow
4. **Retrofit 版本**：2.11.0 支持 Kotlin 协程和流式响应

#### 国内镜像配置（可选）

在 `settings.gradle.kts` 中添加国内镜像加速下载：

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
    }
}
```

### 2.4 环境验证

#### 验证步骤

1. **Gradle 同步**
   - 打开项目后等待 Gradle 同步完成
   - 确认无错误提示

2. **构建项目**
   - 执行 `Build → Make Project`
   - 确认构建成功

3. **运行测试应用**
   - 连接设备或启动模拟器
   - 点击 Run 按钮
   - 验证空白应用正常运行

4. **依赖检查**
   - 打开 `Project` 视图中的 `External Libraries`
   - 确认所有依赖已正确下载

#### 常见问题解决

1. **Gradle 同步失败**
   - 检查网络连接
   - 尝试使用国内镜像
   - 清除缓存：`File → Invalidate Caches and Restart`

2. **依赖下载失败**
   - 检查代理设置
   - 尝试离线模式
   - 手动下载依赖

3. **构建错误**
   - 检查 JDK 版本
   - 更新 Gradle 版本
   - 清理项目：`./gradlew clean`

#### 下一步
环境搭建完成后，可以开始按照 [5. 详细开发指南](#5-详细开发指南) 进行开发。

---

## 3. 项目架构设计

### 3.1 MVVM 架构说明

#### MVVM 架构概述

Model-View-ViewModel (MVVM) 是一种软件架构模式，用于将用户界面逻辑与业务逻辑分离。在本项目中，MVVM 架构提供以下优势：

1. **关注点分离**：UI 代码、业务逻辑和数据访问各司其职
2. **可测试性**：ViewModel 可以独立于 UI 进行测试
3. **数据绑定**：通过 StateFlow/LiveData 实现自动 UI 更新
4. **生命周期感知**：避免内存泄漏和资源浪费

#### 架构组件

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│      View       │    │   ViewModel      │    │      Model      │
│  (Compose UI)   │◄──►│ (UI Logic/State) │◄──►│ (Data/Logic)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   UI Events     │    │   StateFlow      │    │  Repository     │
│  (用户交互)     │    │  (UI 状态)       │    │  (数据仓库)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                   │           │
                                                   ▼           ▼
                                          ┌─────────────┐ ┌─────────────┐
                                          │  Local      │ │  Remote     │
                                          │  DataSource │ │  DataSource │
                                          │  (Room/     │ │  (API)      │
                                          │  DataStore) │ │             │
                                          └─────────────┘ └─────────────┘
```

#### 各层职责

**1. View 层 (UI)**
- **组成**：Compose 函数、Activity、Fragment
- **职责**：
  - 显示 UI 元素
  - 收集用户输入事件
  - 观察 ViewModel 的状态变化
  - 根据状态更新 UI
- **原则**：
  - 不包含业务逻辑
  - 尽可能无状态
  - 通过 ViewModel 访问数据

**2. ViewModel 层**
- **组成**：继承 `androidx.lifecycle.ViewModel`
- **职责**：
  - 管理 UI 状态（通过 StateFlow/LiveData）
  - 处理用户交互事件
  - 调用 Repository 获取数据
  - 执行业务逻辑
- **特性**：
  - 生命周期感知（ survives configuration changes）
  - 协程作用域管理
  - 错误处理和状态管理

**3. Model 层**
- **组成**：Repository、UseCase、数据模型
- **职责**：
  - 封装业务逻辑和数据操作
  - 协调多个数据源（本地、远程）
  - 提供干净的数据接口
- **Repository 模式**：
  - 单一数据源抽象
  - 数据转换和缓存
  - 网络错误处理和重试

#### 数据流示例

```kotlin
// View (Compose)
@Composable
fun ProcessTextScreen(viewModel: ProcessTextViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is ProcessTextUiState.Loading -> LoadingScreen()
        is ProcessTextUiState.Success -> SuccessScreen(state.text)
        is ProcessTextUiState.Error -> ErrorScreen(state.message)
    }

    // 发送事件
    Button(onClick = { viewModel.onEvent(ProcessTextEvent.Retry) }) {
        Text("重试")
    }
}

// ViewModel
class ProcessTextViewModel(private val repository: TextRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ProcessTextUiState>(ProcessTextUiState.Loading)
    val uiState: StateFlow<ProcessTextUiState> = _uiState.asStateFlow()

    fun onEvent(event: ProcessTextEvent) {
        when (event) {
            is ProcessTextEvent.ProcessText -> processText(event.text)
            ProcessTextEvent.Retry -> retry()
        }
    }

    private fun processText(text: String) {
        viewModelScope.launch {
            _uiState.value = ProcessTextUiState.Loading
            try {
                val result = repository.processText(text)
                _uiState.value = ProcessTextUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = ProcessTextUiState.Error(e.message ?: "未知错误")
            }
        }
    }
}

// Repository
class TextRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) {
    suspend fun processText(text: String): String {
        // 业务逻辑：先检查缓存，再调用 API
        return remoteDataSource.processText(text)
    }
}
```

#### 状态管理

1. **UI 状态密封类**
```kotlin
sealed interface ProcessTextUiState {
    object Loading : ProcessTextUiState
    data class Success(val text: String) : ProcessTextUiState
    data class Error(val message: String) : ProcessTextUiState
}
```

2. **事件密封类**
```kotlin
sealed class ProcessTextEvent {
    data class ProcessText(val text: String) : ProcessTextEvent()
    object Retry : ProcessTextEvent()
}
```

#### 最佳实践

1. **ViewModel 拆分**：按功能模块拆分 ViewModel，避免臃肿
2. **状态不可变**：使用 `data class` 和 `copy()` 更新状态
3. **单向数据流**：事件 → ViewModel → 状态 → UI
4. **错误处理**：统一的错误状态和恢复机制
5. **测试策略**：ViewModel 单元测试，UI 集成测试

### 3.2 目录结构详解

建议采用 Feature-based 或 Layer-based 结构。

#### 推荐目录结构

```
com.yourname.aitext
├── app
│   ├── ui
│   │   ├── theme               // Compose 主题配置
│   │   ├── components          // 通用组件 (如 Loading, MarkdownText)
│   │   ├── main                // 主界面 (设置 API，管理 Prompt)
│   │   └── process             // 划词处理界面 (ProcessTextActivity + Dialog UI)
│   ├── data
│   │   ├── local               // Room Database & DataStore
│   │   │   ├── AppDatabase.kt
│   │   │   ├── PromptDao.kt
│   │   │   └── UserPreferences.kt
│   │   ├── remote              // Retrofit API
│   │   │   ├── LlmService.kt
│   │   │   ├── OpenAiRequest.kt
│   │   │   └── SseHandler.kt   // 流式响应解析器
│   │   └── repository          // 数据仓库 (Repository Pattern)
│   ├── domain
│   │   ├── model               // 数据模型 (PromptTemplate, ChatMessage)
│   │   └── usecase             // 业务逻辑 (TemplateEngineUseCase)
│   └── utils                   // 工具类 (ClipboardUtils, JsonUtils)
```

#### 结构说明

**1. UI 层 (`ui/`)**
- `theme/`：Compose 主题定义，颜色、字体、形状配置
- `components/`：可复用组件，如 LoadingIndicator、MarkdownText、ErrorView
- `main/`：主设置界面，API 配置、Prompt 管理
- `process/`：划词处理核心界面，ProcessTextActivity 及相关 Composable

**2. 数据层 (`data/`)**
- `local/`：本地数据存储
  - `AppDatabase.kt`：Room 数据库定义
  - `PromptDao.kt`：Prompt 模板数据访问对象
  - `UserPreferences.kt`：DataStore 偏好设置管理
- `remote/`：网络数据源
  - `LlmService.kt`：Retrofit API 接口定义
  - `OpenAiRequest.kt`：API 请求数据模型
  - `SseHandler.kt`：SSE 流式响应解析器
- `repository/`：数据仓库，协调本地和远程数据源

**3. 领域层 (`domain/`)**
- `model/`：核心业务模型
  - `PromptTemplate.kt`：Prompt 模板数据类
  - `ChatMessage.kt`：聊天消息数据类
  - `AppConfig.kt`：应用配置数据类
- `usecase/`：业务用例
  - `TemplateEngineUseCase.kt`：Prompt 模板引擎
  - `ChatUseCase.kt`：聊天对话业务逻辑

**4. 工具层 (`utils/`)**
- `ClipboardUtils.kt`：剪贴板操作工具
- `JsonUtils.kt`：JSON 序列化/反序列化工具
- `NetworkUtils.kt`：网络状态检测工具
- `WindowUtils.kt`：窗口布局和位置工具

#### 包命名规范
- 使用反向域名：`com.yourname.aitext`
- 模块名使用小写字母和下划线：`ui.process`
- 类名使用大驼峰：`ProcessTextActivity`
- 资源文件使用小写字母和下划线：`ic_launcher.xml`

#### Gradle 模块划分建议
对于大型项目，可考虑拆分为以下模块：
1. `:app`：主应用模块
2. `:core`：核心业务逻辑
3. `:data`：数据层实现
4. `:ui`：UI 组件库

### 3.3 模块职责划分

#### 核心模块职责矩阵

| **模块** | **主要职责** | **关键类** | **依赖关系** |
|----------|-------------|------------|-------------|
| **UI 层** | 用户界面展示和交互 | `ProcessTextActivity`, `MainScreen`, `PromptDialog` | 依赖 ViewModel |
| **ViewModel** | UI 状态管理和业务逻辑协调 | `ProcessTextViewModel`, `MainViewModel` | 依赖 Repository |
| **Repository** | 数据聚合和业务逻辑封装 | `TextRepository`, `PromptRepository` | 依赖 DataSource |
| **DataSource** | 具体数据源操作 | `LocalDataSource`, `RemoteDataSource` | 独立模块 |
| **Domain** | 核心业务模型和用例 | `PromptTemplate`, `ChatUseCase` | 独立模块 |
| **Utils** | 通用工具函数 | `ClipboardUtils`, `NetworkUtils` | 独立模块 |

#### 详细职责说明

**1. UI 模块 (`ui/`)**
- **`ui.process`**：划词处理相关界面
  - `ProcessTextActivity`：跨应用文本处理入口 Activity
  - `ProcessTextScreen`：Compose 主界面
  - `PromptSelectionDialog`：Prompt 选择对话框
- **`ui.main`**：主设置界面
  - `MainScreen`：API 配置和 Prompt 管理主界面
  - `ApiConfigScreen`：API 配置界面
  - `PromptListScreen`：Prompt 模板列表界面
- **`ui.components`**：通用 UI 组件
  - `MarkdownText`：Markdown 渲染组件
  - `LoadingIndicator`：加载指示器
  - `ErrorView`：错误显示组件
- **`ui.theme`**：主题和样式
  - `Theme.kt`：应用主题定义
  - `Colors.kt`：颜色定义
  - `Typography.kt`：字体定义

**2. ViewModel 模块**
- **`ProcessTextViewModel`**：划词处理业务逻辑
  - 管理文本处理状态
  - 处理用户 Prompt 选择
  - 协调文本处理和流式输出
- **`MainViewModel`**：主界面业务逻辑
  - 管理 API 配置状态
  - 处理 Prompt 模板 CRUD 操作
  - 管理应用设置

**3. Repository 模块 (`data/repository/`)**
- **`TextRepository`**：文本处理数据仓库
  - 协调本地缓存和远程 API 调用
  - 实现重试和错误处理逻辑
  - 提供干净的文本处理接口
- **`PromptRepository`**：Prompt 模板数据仓库
  - 管理 Prompt 模板的增删改查
  - 处理模板导入导出
  - 提供预设模板管理

**4. DataSource 模块**
- **`data/local/`**：本地数据源
  - `AppDatabase`：Room 数据库定义
  - `PromptDao`：Prompt 模板数据访问
  - `UserPreferences`：DataStore 偏好设置
- **`data/remote/`**：远程数据源
  - `LlmService`：Retrofit API 接口
  - `SseHandler`：SSE 流式响应处理器
  - `ApiClient`：API 客户端封装

**5. Domain 模块**
- **`domain/model/`**：数据模型
  - `PromptTemplate`：Prompt 模板实体
  - `ChatMessage`：聊天消息模型
  - `AppConfig`：应用配置模型
- **`domain/usecase/`**：业务用例
  - `TemplateEngineUseCase`：模板引擎业务逻辑
  - `ChatUseCase`：聊天对话业务逻辑

**6. Utils 模块**
- **`ClipboardUtils`**：剪贴板操作
- **`NetworkUtils`**：网络状态检测
- **`JsonUtils`**：JSON 处理工具
- **`WindowUtils`**：窗口布局工具

#### 模块间通信

1. **UI → ViewModel**：通过事件（Event）发送用户交互
2. **ViewModel → Repository**：直接调用 Repository 方法
3. **Repository → DataSource**：协调多个数据源
4. **ViewModel → UI**：通过 StateFlow 更新 UI 状态
5. **跨模块通信**：通过依赖注入传递实例

#### 依赖注入建议

使用 Hilt 或 Koin 实现依赖注入：

```kotlin
// Hilt 示例
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideLlmService(): LlmService {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LlmService::class.java)
    }

    @Provides
    @Singleton
    fun provideTextRepository(
        llmService: LlmService,
        promptDao: PromptDao
    ): TextRepository {
        return TextRepository(
            remoteDataSource = RemoteDataSource(llmService),
            localDataSource = LocalDataSource(promptDao)
        )
    }
}
```

#### 测试策略

1. **ViewModel 测试**：模拟 Repository，验证状态变化
2. **Repository 测试**：模拟 DataSource，验证业务逻辑
3. **DataSource 测试**：真实数据库和 Mock 网络
4. **UI 测试**：Compose 测试框架，模拟用户交互

---

## 4. 核心技术实现

### 4.1 ACTION_PROCESS_TEXT 完整实现

这是应用的核心入口。不使用无障碍服务，而是注册为系统的文本处理器。

#### Manifest 配置

需要定义一个 Activity，并设置 theme 为 Dialog 模式，使其看起来像悬浮窗。

```xml
<activity
    android:name=".ui.process.ProcessTextActivity"
    android:label="AI 助手"
    android:theme="@style/Theme.AppCompat.Dialog"
    android:excludeFromRecents="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
</activity>
```

#### ProcessTextActivity 实现要点

1. **获取选中文本**
```kotlin
val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
```

2. **Dialog 窗口配置**
```kotlin
window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
window.setGravity(Gravity.BOTTOM)
window.setBackgroundDrawableResource(android.R.color.transparent)
```

3. **主题样式要求**
- 使用 `Theme.AppCompat.Dialog` 或 `Theme.MaterialComponents.Dialog`
- 设置 `android:windowIsFloating="true"`
- 调整 `android:windowBackground` 为透明或半透明

#### 兼容性注意事项

1. **Android 版本支持**
- `ACTION_PROCESS_TEXT` 从 Android 6.0 (API 23) 开始提供
- 但部分定制 ROM 可能有限制
- 建议最低支持 Android 7.0 (API 24)

2. **应用兼容性**
- 支持大多数浏览器 (Chrome, Firefox, Edge)
- 支持社交应用 (微信, Telegram, Twitter)
- 支持文档应用 (WPS, Office, PDF Reader)
- 不支持某些游戏或定制应用

3. **权限要求**
- 无需 `SYSTEM_ALERT_WINDOW` 权限
- 无需无障碍服务权限
- 只需要 `INTERNET` 权限访问 API

#### 测试方法
1. 在 Chrome 中选中文本，点击"分享"或"更多"选项
2. 在应用列表中找到"AI 助手"
3. 验证是否能正确打开并显示选中文本
4. 测试不同应用中的兼容性

### 4.2 Prompt 模板引擎详细设计

简单的字符串替换逻辑，允许用户定义变量。

#### 核心功能

- **变量替换**：支持 `{{text}}` 占位符，自动替换为用户选中的文本
- **多变量支持**：可扩展支持 `{{language}}`、`{{style}}` 等自定义变量
- **条件逻辑**：可选支持简单的条件判断和循环
- **模板管理**：CRUD 操作，分类，导入导出

#### 基础实现

```kotlin
fun buildPrompt(template: String, rawText: String): String {
    // 支持 {{text}} 占位符
    return template.replace("{{text}}", rawText)
}
```

#### 高级模板引擎设计

```kotlin
class TemplateEngine {
    private val variablePattern = Regex("\\{\\{([^{}]+)\\}\\}")

    fun render(template: String, variables: Map<String, String>): String {
        return variablePattern.replace(template) { matchResult ->
            val variableName = matchResult.groupValues[1].trim()
            variables[variableName] ?: matchResult.value
        }
    }

    fun extractVariables(template: String): List<String> {
        return variablePattern.findAll(template)
            .map { it.groupValues[1].trim() }
            .toList()
    }
}
```

#### 预设模板示例

1. **翻译模板**
```
将以下文本翻译成中文：{{text}}
```

2. **总结模板**
```
用简洁的语言总结以下内容：{{text}}
```

3. **代码解释**
```
解释以下代码的功能和逻辑：{{text}}
```

4. **邮件润色**
```
将以下文本润色为专业的商务邮件：{{text}}
```

5. **语法检查**
```
检查以下英文文本的语法错误并修正：{{text}}
```

#### 模板数据结构

```kotlin
@Entity(tableName = "prompts")
data class PromptTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,         // 显示在按钮上的文字，如 "翻译"
    val template: String,      // 模版内容，如 "Translate this: {{text}}"
    val systemPrompt: String = "You are a helpful assistant.", // 可选的 system role
    val category: String = "general", // 分类：translation, code, writing, etc.
    val isPreset: Boolean = false, // 是否为预设模板
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 使用示例

```kotlin
val template = "将以下文本翻译成英文：{{text}}"
val engine = TemplateEngine()
val result = engine.render(template, mapOf("text" to "你好，世界"))
// 结果: "将以下文本翻译成英文：你好，世界"
```

### 4.3 LLM API 集成和 SSE 流式处理

为了用户体验，必须支持 `stream: true`。Retrofit 本身处理流式不够直观，通常结合 `OkHttp` 的 `ResponseBody` 进行按行读取。

#### API 兼容性

核心适配 OpenAI 接口格式 (`v1/chat/completions`)，因为 DeepSeek、Moonshot、LocalAI (Ollama) 均兼容此格式。

**支持的服务商**：
1. **OpenAI**：`https://api.openai.com/v1/chat/completions`
2. **DeepSeek**：`https://api.deepseek.com/v1/chat/completions`
3. **Ollama**：`http://localhost:11434/v1/chat/completions`
4. **Azure OpenAI**：`https://{resource}.openai.azure.com/openai/deployments/{deployment}/chat/completions`
5. **其他兼容服务**：任何实现 OpenAI 兼容 API 的服务

#### 请求数据结构

```kotlin
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val max_tokens: Int? = null,
    val temperature: Double? = null,
    val top_p: Double? = null
)

@Serializable
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)
```

#### SSE 流式处理逻辑

1. **建立连接**：使用 OkHttp 发起请求，设置 `stream: true`
2. **读取 InputStream**：按行读取响应流
3. **检测 `data: ` 前缀**：每行以 `data: ` 开头
4. **解析 JSON**：提取 `choices[0].delta.content`
5. **通过 Kotlin Flow `emit` 数据块更新 UI**

#### 流式解析器实现

```kotlin
class SseParser {
    private val dataPrefix = "data: "
    private val doneSignal = "[DONE]"
    private val gson = Gson()

    fun parseStream(inputStream: InputStream): Flow<String> = flow {
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.startsWith(dataPrefix)) {
                    val data = line.substring(dataPrefix.length)
                    if (data == doneSignal) {
                        return@flow
                    }
                    try {
                        val jsonObject = gson.fromJson(data, JsonObject::class.java)
                        val content = jsonObject
                            .getAsJsonArray("choices")
                            ?.get(0)
                            ?.asJsonObject
                            ?.getAsJsonObject("delta")
                            ?.get("content")
                            ?.asString
                        if (!content.isNullOrEmpty()) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        // 处理解析错误
                    }
                }
            }
        }
    }
}
```

#### Retrofit 服务定义

```kotlin
interface LlmService {
    @POST("chat/completions")
    @Streaming
    suspend fun chatCompletion(
        @Body request: ChatRequest
    ): Response<ResponseBody>
}
```

#### OkHttp 配置

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()
        chain.proceed(request)
    }
    .build()
```

#### 错误处理策略

1. **网络超时**：自动重试最多 3 次
2. **API 密钥错误**：提示用户检查配置
3. **流式中断**：保存已接收内容，提供继续按钮
4. **服务不可用**：降级到非流式模式或提示稍后重试

### 4.4 Markdown 渲染实现

#### 库选择

推荐使用 `com.halilibo:rich-text` 或 `com.github.jeziellago:compose-markdown`，两者都提供 Compose 兼容的 Markdown 渲染。

**依赖配置**：
```kotlin
dependencies {
    implementation("com.halilibo:rich-text:0.20.2")
    // 或
    implementation("com.github.jeziellago:compose-markdown:0.6.0")
}
```

#### 基础使用

```kotlin
@Composable
fun MarkdownText(content: String) {
    RichText(text = content)
}

// 或使用 compose-markdown
@Composable
fun MarkdownText(content: String) {
    MarkdownText(
        markdown = content,
        modifier = Modifier.fillMaxWidth()
    )
}
```

#### 代码高亮配置

```kotlin
// 使用 rich-text 的代码高亮
RichText(
    text = markdownContent,
    codeBlockTheme = CodeBlockTheme.DRACULA,
    textStyle = { defaultStyle ->
        defaultStyle.copy(
            color = MaterialTheme.colorScheme.onBackground
        )
    }
)
```

#### 自定义样式

```kotlin
@Composable
fun CustomMarkdownText(content: String) {
    RichText(
        text = content,
        onLinkClicked = { url -> /* 处理链接点击 */ },
        style = RichTextStyle(
            codeBlockStyle = CodeBlockStyle(
                backgroundColor = Color(0xFF282C34),
                textColor = Color(0xFFABB2BF),
                borderRadius = 8.dp,
                border = BorderStroke(1.dp, Color(0xFF3E4451))
            ),
            linkStyle = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        )
    )
}
```

#### 性能优化

1. **分块渲染**：长内容分块加载渲染
2. **图片懒加载**：延迟加载 Markdown 中的图片
3. **缓存机制**：缓存解析后的 AST
4. **避免重组**：使用 `remember` 缓存 Markdown 解析结果

---

## 5. 详细开发指南

### 5.1 第一阶段：原型验证

**目标**：在浏览器选中文字，弹出你的 App，并显示选中的文字。

#### 开发步骤

1. **创建项目**：Empty Compose Activity
   - 按照 [2.2 项目创建步骤](#22-项目创建步骤) 创建新项目
   - 配置最小 SDK 为 24，语言为 Kotlin

2. **Manifest 配置**：添加 `ACTION_PROCESS_TEXT` intent-filter
   - 参考 [4.1 ACTION_PROCESS_TEXT 完整实现](#41-action_process_text-完整实现)
   - 添加必要的权限和主题配置

3. **ProcessTextActivity 实现**：
   - 创建 `ui.process.ProcessTextActivity` 类
   - 在 `onCreate` 中获取 `intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)`
   - 使用 Compose 显示简单的 `Text(text = selectedText)`

4. **Dialog 窗口配置**：
   - 设置 Activity 的 Window 布局属性（宽高、位置）
   - 确保使用 Dialog 样式
   - 配置透明背景和圆角

5. **测试验证**：
   - 在 Chrome 中选中文本，点击"分享"或"更多"选项
   - 选择"AI 助手"应用
   - 验证是否能正确打开并显示选中文本

#### 关键代码

```kotlin
// ProcessTextActivity.kt
class ProcessTextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""

        // Dialog 窗口配置
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.BOTTOM)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            AITextSelectionAssistantTheme {
                Surface {
                    Text(text = "选中的文本：$selectedText")
                }
            }
        }
    }
}
```

#### 验收标准

1. ✅ 项目创建成功，Gradle 同步通过
2. ✅ Manifest 正确配置 `ACTION_PROCESS_TEXT`
3. ✅ ProcessTextActivity 能正确获取选中文本
4. ✅ 应用能在文本选择菜单中显示
5. ✅ 点击后能打开 Dialog 并显示文本

#### 常见问题

1. **应用不在选择菜单中**：检查 intent-filter 配置和 exported 属性
2. **无法获取文本**：检查 `EXTRA_PROCESS_TEXT` 的获取方式
3. **Dialog 样式不正确**：检查主题配置和窗口属性
4. **应用崩溃**：检查最小 SDK 和依赖兼容性

### 5.2 第二阶段：配置管理与 Prompt 系统

### 5.3 第三阶段：网络层与流式输出

### 5.4 第四阶段：UI 优化与 Markdown 渲染

### 5.5 第五阶段：测试与发布

---

## 6. 测试策略

### 6.1 单元测试

#### 测试目标
验证 ViewModel、Repository、UseCase 等业务逻辑单元的正确性。

#### 测试框架
- **JUnit 4/5**：基础测试框架
- **MockK**：Kotlin Mocking 框架
- **Kotlin Coroutines Test**：协程测试支持
- **Turbine**：Flow 测试库

#### ViewModel 测试示例
```kotlin
class ProcessTextViewModelTest {
    private lateinit var viewModel: ProcessTextViewModel
    private val mockRepository = mockk<TextRepository>()

    @Before
    fun setup() {
        viewModel = ProcessTextViewModel(mockRepository)
    }

    @Test
    fun `processText should emit success state`() = runTest {
        // Given
        val testText = "Hello World"
        coEvery { mockRepository.processText(testText) } returns "Processed: Hello World"

        // When
        viewModel.onEvent(ProcessTextEvent.ProcessText(testText))

        // Then
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState is ProcessTextUiState.Loading)

            val successState = awaitItem()
            assertTrue(successState is ProcessTextUiState.Success)
            assertEquals("Processed: Hello World", (successState as ProcessTextUiState.Success).text)
        }
    }
}
```

#### 测试覆盖率目标
- ViewModel：90%+
- Repository：85%+
- UseCase：90%+
- 工具类：80%+

#### 最佳实践
1. 使用 `runTest` 代替 `runBlockingTest`
2. 模拟所有外部依赖
3. 测试正常流程和异常流程
4. 使用 descriptive test names

### 6.2 集成测试

### 6.3 UI 测试

### 6.4 兼容性测试指南

---

## 7. 部署和发布

### 7.1 代码签名配置

### 7.2 Google Play 发布指南

### 7.3 版本管理策略

---

## 8. 维护和扩展

### 8.1 代码规范

### 8.2 错误处理最佳实践

### 8.3 性能优化建议

### 8.4 功能扩展指南

---

## 9. 附录

### 9.1 完整代码示例

### 9.2 常见问题解答

### 9.3 参考资料

---

*文档优化计划执行中...*