# DemoWork Project

这是一个基于 Android 的模块化项目示例。

## 项目结构

项目采用模块化架构，主要包含以下模块：

- `app`: 主应用程序模块，负责应用的入口和整体协调。
- `base`: 基础模块，包含通用的 Activity、Fragment、工具类等。
- `feature_home`: 首页功能模块。
- `feature_login`: 登录功能模块。
- `feature_user`: 用户中心功能模块。
- `lib_network`: 网络库模块，封装了网络请求相关功能。

## 使用的技术栈

- **Kotlin**: 主要开发语言。
- **Gradle**: 项目构建工具。
- **AndroidX**: Android 官方推荐的库集合。
- **ARouter**: 阿里巴巴开源的 Android 路由框架，用于模块间的导航。
- **Retrofit & OkHttp**: 用于网络请求。
- **XLog**: 强大的 Android 日志库。
- **MMKV**: 腾讯开源的高性能键值存储库。
- **ViewBinding**: 简化布局文件交互。
- **Lifecycle Components**: Android 生命周期管理组件。

## 模块说明

- **base**: 提供了 `BaseActivity` 等基础组件，集成了日志、状态栏设置、ViewBinding、加载框、权限请求等通用功能。
- **lib_network**: 封装了基于 Retrofit 和 OkHttp 的网络请求逻辑，集成了日志拦截器。
- **feature_login**: 包含登录相关的页面和逻辑。
- **feature_home**: 包含首页相关的页面和逻辑。
- **feature_user**: 包含用户中心相关的页面和逻辑。

## 构建与运行

1. 克隆项目到本地。
2. 在 Android Studio 中打开项目。
3. 同步 Gradle。
4. 运行 `app` 模块到设备或模拟器。

## 贡献

欢迎提交 Pull Request。

## 许可证

[待定]